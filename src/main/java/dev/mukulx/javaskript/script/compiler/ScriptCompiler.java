package dev.mukulx.javaskript.script.compiler;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

public class ScriptCompiler {

  private final JavaSkriptPlugin plugin;
  private final File tempDir;
  private static final Pattern PUBLIC_CLASS_PATTERN =
      Pattern.compile(
          "public\\s+(?:(?:final|abstract|sealed|non-sealed|static)\\s+)*(?:class|record|enum|interface)\\s+(\\w+)");
  private static final Pattern CLASS_PATTERN =
      Pattern.compile(
          "(?:(?:public|protected|private|static|final|abstract|sealed|non-sealed)\\s+)*(?:class|record|enum|interface)\\s+(\\w+)");

  public ScriptCompiler(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.tempDir = new File(plugin.getDataFolder(), "temp");

    if (tempDir.exists()) {
      deleteRecursively(tempDir);
    }
    tempDir.mkdirs();
  }

  /**
   * Compile a script and return all compiled classes
   *
   * @param scriptName The script file name
   * @param sourceCode The source code
   * @return Map of class names to bytecode, or null if compilation failed
   */
  public Map<String, byte[]> compileAll(String scriptName, String sourceCode) {
    return compileAll(scriptName, sourceCode, Collections.emptyList());
  }

  /**
   * Compile a script with dependencies and return all compiled classes
   *
   * @param scriptName The script file name
   * @param sourceCode The source code
   * @param dependencyFiles List of dependency JAR files to include in classpath
   * @return Map of class names to bytecode, or null if compilation failed
   */
  public Map<String, byte[]> compileAll(
      String scriptName, String sourceCode, List<File> dependencyFiles) {
    if (sourceCode == null || sourceCode.trim().isEmpty()) {
      plugin.getLogger().warning("Empty source code for script: " + scriptName);
      return null;
    }

    String mainClassName = extractPublicClassName(sourceCode);
    if (mainClassName == null) {
      mainClassName = getClassName(scriptName);
    }

    File sourceFile = null;
    File outputDir = null;

    try {
      // Create temporary directories
      File scriptTempDir = Files.createTempDirectory(tempDir.toPath(), "compile-").toFile();

      outputDir = new File(scriptTempDir, "output");
      outputDir.mkdirs();

      // Write source file
      sourceFile = new File(scriptTempDir, mainClassName + ".java");
      Files.writeString(sourceFile.toPath(), sourceCode, StandardCharsets.UTF_8);

      // Prepare compiler arguments
      StringWriter errorWriter = new StringWriter();
      PrintWriter errorPrintWriter = new PrintWriter(errorWriter);

      // Get classpath with dependencies
      String classpath = buildClasspath(dependencyFiles);

      String[] args = {
        "-21",
        "-encoding",
        "UTF-8",
        "-d",
        outputDir.getAbsolutePath(),
        "-classpath",
        classpath,
        "-nowarn",
        sourceFile.getAbsolutePath()
      };

      // Compile
      boolean success =
          BatchCompiler.compile(args, new PrintWriter(System.out), errorPrintWriter, null);

      if (!success) {
        plugin.getLogger().severe("Compilation failed for script: " + scriptName);
        plugin.getLogger().severe("Errors:\n" + errorWriter.toString());
        return null;
      }

      // Read all compiled classes from compiler output directory
      Map<String, byte[]> compiledClasses = new HashMap<>();

      final java.nio.file.Path outputPath = outputDir.toPath();
      try (java.util.stream.Stream<java.nio.file.Path> stream = Files.walk(outputPath)) {
        stream
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".class"))
            .forEach(
                p -> {
                  String relPath = outputPath.relativize(p).toString();
                  String className =
                      relPath.replace(File.separatorChar, '.').substring(0, relPath.length() - 6);
                  try {
                    byte[] bytecode = Files.readAllBytes(p);
                    compiledClasses.put(className, bytecode);
                    plugin
                        .getLogger()
                        .fine("Loaded class: " + className + " (" + bytecode.length + " bytes)");
                  } catch (IOException e) {
                    plugin.getLogger().warning("Failed to read compiled class file: " + className);
                  }
                });
      }

      if (compiledClasses.isEmpty()) {
        plugin.getLogger().severe("No compiled classes found for script: " + scriptName);
        return null;
      }

      plugin.debug(
          "Successfully compiled " + compiledClasses.size() + " class(es) for " + scriptName);

      return compiledClasses;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error compiling script: " + scriptName, e);
      return null;
    } finally {
      // Cleanup
      cleanup(sourceFile, outputDir);
    }
  }

  /**
   * Compile a script and return only the main public class bytecode (legacy method)
   *
   * @param scriptName The script file name
   * @param sourceCode The source code
   * @return Bytecode of the main public class, or null if compilation failed
   */
  public byte[] compile(String scriptName, String sourceCode) {
    Map<String, byte[]> allClasses = compileAll(scriptName, sourceCode);
    if (allClasses == null || allClasses.isEmpty()) {
      return null;
    }

    String mainClassName = extractPublicClassName(sourceCode);
    return allClasses.get(mainClassName);
  }

  private String extractPublicClassName(String sourceCode) {
    Matcher matcher = PUBLIC_CLASS_PATTERN.matcher(sourceCode);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private List<String> extractAllClassNames(String sourceCode) {
    List<String> classNames = new ArrayList<>();
    Matcher matcher = CLASS_PATTERN.matcher(sourceCode);

    while (matcher.find()) {
      String className = matcher.group(1);
      if (!classNames.contains(className)) {
        classNames.add(className);
      }
    }

    return classNames;
  }

  public String getClassName(String scriptName) {
    String name = scriptName;
    if (name.contains("/")) {
      name = name.substring(name.lastIndexOf('/') + 1);
    }
    if (name.contains("\\")) {
      name = name.substring(name.lastIndexOf('\\') + 1);
    }
    while (name.startsWith("-")) {
      name = name.substring(1);
    }
    if (name.endsWith(".java")) {
      name = name.substring(0, name.length() - 5);
    }
    return name;
  }

  // Rebuilt whenever the set of loaded plugins changes, so plugins that load after the first
  // compile are visible to scripts as well
  private volatile String cachedBaseClasspath = null;
  private volatile Set<String> cachedPluginNames = Collections.emptySet();

  private String getBaseClasspath() {
    Set<String> pluginNames = new HashSet<>();
    for (var loadedPlugin : plugin.getServer().getPluginManager().getPlugins()) {
      pluginNames.add(loadedPlugin.getName());
    }

    String cached = cachedBaseClasspath;
    if (cached != null && pluginNames.equals(cachedPluginNames)) {
      return cached;
    }

    // A set of exact paths, so one path never hides another that merely contains it as text
    Set<String> entries = new LinkedHashSet<>();

    try {
      // Add Bukkit/Paper API
      addToClasspath(entries, org.bukkit.Bukkit.class);

      // Add Adventure API (Component, etc.)
      addToClasspath(entries, net.kyori.adventure.text.Component.class);

      // Add Adventure Examination API (required by Component)
      try {
        Class<?> examinableClass = Class.forName("net.kyori.examination.Examinable");
        addToClasspath(entries, examinableClass);
      } catch (ClassNotFoundException e) {
        plugin.getLogger().warning("Adventure Examination API not found in classpath");
      }

      // Add Adventure MiniMessage
      try {
        Class<?> miniMessageClass =
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
        addToClasspath(entries, miniMessageClass);
      } catch (ClassNotFoundException e) {
        plugin.getLogger().warning("MiniMessage not found in classpath");
      }

      // Add BungeeCord Chat API (required by Paper)
      try {
        Class<?> bungeeChatClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
        addToClasspath(entries, bungeeChatClass);
      } catch (ClassNotFoundException e) {
        plugin.getLogger().warning("BungeeCord Chat API not found in classpath");
      }

      // Add Google Gson API
      try {
        Class<?> gsonClass = Class.forName("com.google.gson.Gson");
        addToClasspath(entries, gsonClass);
      } catch (ClassNotFoundException e) {
        plugin.getLogger().warning("Gson not found in classpath");
      }

      // Add Google Guava API
      try {
        Class<?> guavaClass = Class.forName("com.google.common.collect.ImmutableList");
        addToClasspath(entries, guavaClass);
      } catch (ClassNotFoundException e) {
        plugin.getLogger().warning("Guava not found in classpath");
      }

      // Add plugin jar itself (contains bundled dependencies)
      addToClasspath(entries, plugin.getClass());

      // Add all loaded plugin jars (for cross-plugin compatibility)
      for (var loadedPlugin : plugin.getServer().getPluginManager().getPlugins()) {
        addToClasspath(entries, loadedPlugin.getClass());
      }

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to build base classpath", e);
    }

    String classpath = String.join(File.pathSeparator, entries);
    cachedPluginNames = pluginNames;
    cachedBaseClasspath = classpath;
    return classpath;
  }

  private String buildClasspath(List<File> dependencyFiles) {
    String base = getBaseClasspath();
    if (dependencyFiles == null || dependencyFiles.isEmpty()) {
      return base;
    }

    StringBuilder classpath = new StringBuilder(base);
    for (File depFile : dependencyFiles) {
      if (depFile != null && depFile.exists()) {
        if (classpath.length() > 0) {
          classpath.append(File.pathSeparator);
        }
        classpath.append(depFile.getAbsolutePath());
      }
    }

    return classpath.toString();
  }

  private void addToClasspath(Set<String> entries, Class<?> clazz) {
    try {
      var loc = clazz.getProtectionDomain().getCodeSource().getLocation();
      if (loc != null) {
        entries.add(new File(loc.toURI()).getAbsolutePath());
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.WARNING, "Could not add " + clazz.getName() + " to classpath", e);
    }
  }

  private void cleanup(File sourceFile, File outputDir) {
    try {
      if (sourceFile != null && sourceFile.exists()) {
        deleteRecursively(sourceFile.getParentFile());
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "Failed to cleanup temp files", e);
    }
  }

  private void deleteRecursively(File file) {
    if (file == null || !file.exists()) {
      return;
    }

    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File child : files) {
          deleteRecursively(child);
        }
      }
    }

    file.delete();
  }
}
