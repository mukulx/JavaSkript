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
  private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public\\s+class\\s+(\\w+)");
  private static final Pattern CLASS_PATTERN =
      Pattern.compile("(?:public\\s+)?class\\s+(\\w+)(?:\\s+extends|\\s+implements|\\s*\\{)");

  public ScriptCompiler(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.tempDir = new File(plugin.getDataFolder(), "temp");

    if (!tempDir.exists()) {
      tempDir.mkdirs();
    }
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
      plugin.getLogger().severe("Could not find public class in script: " + scriptName);
      return null;
    }

    // Extract all class names (public and package-private)
    List<String> allClassNames = extractAllClassNames(sourceCode);
    if (allClassNames.isEmpty()) {
      plugin.getLogger().severe("Could not find any classes in script: " + scriptName);
      return null;
    }

    plugin.debug(
        "Found "
            + allClassNames.size()
            + " class(es) in "
            + scriptName
            + ": "
            + String.join(", ", allClassNames));

    File sourceFile = null;
    File outputDir = null;

    try {
      // Create temporary directories
      File scriptTempDir = new File(tempDir, "compile_" + System.currentTimeMillis());
      scriptTempDir.mkdirs();

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

      // Read all compiled classes
      Map<String, byte[]> compiledClasses = new HashMap<>();

      for (String className : allClassNames) {
        File classFile = new File(outputDir, className + ".class");
        if (classFile.exists()) {
          byte[] bytecode = Files.readAllBytes(classFile.toPath());
          compiledClasses.put(className, bytecode);
          plugin
              .getLogger()
              .fine("Loaded class: " + className + " (" + bytecode.length + " bytes)");
        } else {
          plugin.getLogger().warning("Compiled class file not found: " + className);
        }
      }

      // Also check for inner classes (ClassName$InnerClass.class)
      File[] classFiles = outputDir.listFiles((dir, name) -> name.endsWith(".class"));
      if (classFiles != null) {
        for (File classFile : classFiles) {
          String fileName = classFile.getName();
          String className = fileName.substring(0, fileName.length() - 6); // Remove .class

          if (!compiledClasses.containsKey(className)) {
            byte[] bytecode = Files.readAllBytes(classFile.toPath());
            compiledClasses.put(className, bytecode);
            plugin
                .getLogger()
                .fine(
                    "Loaded inner/nested class: " + className + " (" + bytecode.length + " bytes)");
          }
        }
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
    // Remove .java extension
    String name = scriptName;
    if (name.endsWith(".java")) {
      name = name.substring(0, name.length() - 5);
    }
    return name;
  }

  private String buildClasspath() {
    return buildClasspath(Collections.emptyList());
  }

  private String buildClasspath(List<File> dependencyFiles) {
    StringBuilder classpath = new StringBuilder();

    try {
      // Add Bukkit/Paper API
      addToClasspath(classpath, org.bukkit.Bukkit.class);

      // Add Adventure API (Component, etc.)
      addToClasspath(classpath, net.kyori.adventure.text.Component.class);

      // Add Adventure Examination API (required by Component)
      try {
        Class<?> examinableClass = Class.forName("net.kyori.examination.Examinable");
        addToClasspath(classpath, examinableClass);
      } catch (ClassNotFoundException e) {
        plugin.getLogger().warning("Adventure Examination API not found in classpath");
      }

      // Add Adventure MiniMessage
      try {
        Class<?> miniMessageClass =
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
        addToClasspath(classpath, miniMessageClass);
      } catch (ClassNotFoundException e) {
        plugin.getLogger().warning("MiniMessage not found in classpath");
      }

      // Add BungeeCord Chat API (required by Paper)
      try {
        Class<?> bungeeChatClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
        addToClasspath(classpath, bungeeChatClass);
      } catch (ClassNotFoundException e) {
        plugin.getLogger().warning("BungeeCord Chat API not found in classpath");
      }

      // Add plugin jar itself (contains bundled dependencies)
      String pluginJar =
          plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
      if (pluginJar != null && !pluginJar.isEmpty()) {
        if (classpath.length() > 0) {
          classpath.append(File.pathSeparator);
        }
        classpath.append(pluginJar);
      }

      // Add all loaded plugin jars (for cross-plugin compatibility)
      for (var loadedPlugin : plugin.getServer().getPluginManager().getPlugins()) {
        try {
          String path =
              loadedPlugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
          if (path != null && !path.isEmpty() && !classpath.toString().contains(path)) {
            if (classpath.length() > 0) {
              classpath.append(File.pathSeparator);
            }
            classpath.append(path);
          }
        } catch (Exception e) {
          // Skip plugins that can't provide their path
        }
      }

      // Add script dependencies
      for (File depFile : dependencyFiles) {
        if (depFile.exists()) {
          if (classpath.length() > 0) {
            classpath.append(File.pathSeparator);
          }
          classpath.append(depFile.getAbsolutePath());
        }
      }

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to build classpath", e);
    }

    String result = classpath.toString();
    if (result.isEmpty()) {
      plugin.getLogger().severe("Classpath is empty! Scripts will fail to compile.");
    }

    return result;
  }

  private void addToClasspath(StringBuilder classpath, Class<?> clazz) {
    try {
      String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
      if (path != null && !path.isEmpty() && !classpath.toString().contains(path)) {
        if (classpath.length() > 0) {
          classpath.append(File.pathSeparator);
        }
        classpath.append(path);
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
