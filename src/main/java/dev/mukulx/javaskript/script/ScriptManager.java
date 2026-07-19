package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.compiler.ScriptCompiler;
import dev.mukulx.javaskript.script.loader.ScriptClassLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public class ScriptManager {

  private final JavaSkriptPlugin plugin;
  private final File scriptsFolder;
  private final Map<String, ScriptInstance> loadedScripts;
  private final Set<String> disabledScripts;
  private final ScriptCompiler compiler;
  private final File disabledFile;
  private final Map<String, List<File>> scriptDependencies;

  // In-memory compilation cache to bypass repetitive disk/javac tasks on reload
  private final Map<String, CachedScript> compilationCache;

  // Cached bytecode and hash state to verify source modifications
  private static class CachedScript {
    final long lastModified;
    final String contentHash;
    final Map<String, byte[]> compiledClasses;

    CachedScript(long lastModified, String contentHash, Map<String, byte[]> compiledClasses) {
      this.lastModified = lastModified;
      this.contentHash = contentHash;
      this.compiledClasses = compiledClasses;
    }
  }

  public ScriptManager(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.scriptsFolder = new File(plugin.getDataFolder(), "scripts");
    this.loadedScripts = new ConcurrentHashMap<>();
    this.disabledScripts = ConcurrentHashMap.newKeySet();
    this.compiler = new ScriptCompiler(plugin);
    this.disabledFile = new File(plugin.getDataFolder(), "disabled-scripts.json");
    this.scriptDependencies = new ConcurrentHashMap<>();
    this.compilationCache = new ConcurrentHashMap<>();

    if (!scriptsFolder.exists()) {
      scriptsFolder.mkdirs();
      createExampleScripts();
    }

    // Load blacklist array from the local JSON storage file
    loadDisabledScripts();
  }

  public void loadAllScripts() {
    if (!scriptsFolder.exists() || !scriptsFolder.isDirectory()) {
      plugin.getLogger().warning("Scripts folder does not exist!");
      return;
    }

    // Walk the file system and synchronously pull paths matching the extension criteria
    try (Stream<Path> paths = Files.walk(scriptsFolder.toPath())) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  loadScript(path.toFile());
                } catch (Exception e) {
                  plugin
                      .getLogger()
                      .log(Level.SEVERE, "Failed to load script: " + path.getFileName(), e);
                }
              });
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to scan scripts folder!", e);
    }
  }

  public boolean loadScript(File scriptFile) {
    if (scriptFile == null || !scriptFile.exists()) {
      plugin.getLogger().warning("Script file does not exist: " + scriptFile);
      return false;
    }

    String scriptName = scriptFile.getName();

    if (disabledScripts.contains(scriptName)) {
      plugin.debug("Script is disabled, skipping: " + scriptName);
      return false;
    }

    try {
      // Hot-replace logic: unload active instances prior to compilation tasks
      if (loadedScripts.containsKey(scriptName)) {
        unloadScript(scriptName);
      }

      plugin.debug("Loading script: " + scriptName);

      String scriptContent = Files.readString(scriptFile.toPath());

      // Regex validation scanner to prevent target scripts from breaking directory layouts
      if (!checkScriptFolderUsage(scriptName, scriptContent)) {
        plugin.getLogger().severe("Script rejected due to improper folder usage: " + scriptName);
        return false;
      }

      // Read metadata annotations and spawn async threads to pull external Maven dependencies
      List<String> dependencies = extractDependencies(scriptContent);
      List<File> dependencyFiles = new ArrayList<>();

      if (!dependencies.isEmpty()) {
        plugin
            .getLogger()
            .info("Resolving " + dependencies.size() + " dependencies for " + scriptName);

        List<java.util.concurrent.CompletableFuture<List<File>>> futures =
            dependencies.stream()
                .map(
                    dep ->
                        java.util.concurrent.CompletableFuture.supplyAsync(
                            () -> plugin.getDependencyManager().resolveDependency(dep)))
                .collect(java.util.stream.Collectors.toList());

        java.util.concurrent.CompletableFuture.allOf(
                futures.toArray(new java.util.concurrent.CompletableFuture[0]))
            .join();

        for (java.util.concurrent.CompletableFuture<List<File>> future : futures) {
          try {
            List<File> resolved = future.get();
            if (resolved.isEmpty()) {
              plugin.getLogger().warning("Failed to resolve a dependency");
            } else {
              dependencyFiles.addAll(resolved);
            }
          } catch (Exception e) {
            plugin.getLogger().warning("Error resolving dependency: " + e.getMessage());
          }
        }

        scriptDependencies.put(scriptName, dependencyFiles);
      }

      // Check timestamps and structural hashes before spinning up the Java compiler engine
      long lastModified = scriptFile.lastModified();
      String contentHash = Integer.toHexString(scriptContent.hashCode());
      CachedScript cached = compilationCache.get(scriptName);
      Map<String, byte[]> compiledClasses;

      if (cached != null
          && cached.lastModified == lastModified
          && cached.contentHash.equals(contentHash)) {
        plugin.debug("Using cached compilation for: " + scriptName);
        compiledClasses = cached.compiledClasses;
      } else {
        // Fallback to active runtime compilation using dependencies as custom classpath attachments
        compiledClasses = compiler.compileAll(scriptName, scriptContent, dependencyFiles);

        if (compiledClasses == null || compiledClasses.isEmpty()) {
          plugin.getLogger().severe("Failed to compile script: " + scriptName);
          return false;
        }

        compilationCache.put(
            scriptName, new CachedScript(lastModified, contentHash, compiledClasses));
        plugin.debug("Compiled and cached: " + scriptName);
      }

      // Instantiate isolated classloader mapping to assign the raw byte array data into real
      // classes
      ScriptClassLoader classLoader = new ScriptClassLoader(plugin, scriptName, dependencyFiles);
      Map<String, Class<?>> loadedClasses = classLoader.defineClasses(compiledClasses);

      if (loadedClasses.isEmpty()) {
        plugin.getLogger().severe("Failed to load any classes for script: " + scriptName);
        return false;
      }

      // Reflective search setup to locate valid public runtime entry points
      String expectedClassName = compiler.getClassName(scriptName);
      Class<?> scriptClass = loadedClasses.get(expectedClassName);

      if (scriptClass == null) {
        for (Map.Entry<String, Class<?>> entry : loadedClasses.entrySet()) {
          if (entry.getKey().equalsIgnoreCase(expectedClassName)) {
            scriptClass = entry.getValue();
            plugin.getLogger().info("Found main class with different case: " + entry.getKey());
            break;
          }
        }
      }

      if (scriptClass == null) {
        for (Map.Entry<String, Class<?>> entry : loadedClasses.entrySet()) {
          Class<?> clazz = entry.getValue();
          if (java.lang.reflect.Modifier.isPublic(clazz.getModifiers())) {
            scriptClass = clazz;
            plugin
                .getLogger()
                .info(
                    "Using public class '"
                        + entry.getKey()
                        + "' as main class (filename was: "
                        + scriptName
                        + ")");
            break;
          }
        }
      }

      if (scriptClass == null) {
        plugin.getLogger().severe("No suitable main class found for: " + scriptName);
        plugin.getLogger().severe("Available classes: " + loadedClasses.keySet());
        return false;
      }

      ScriptInstance instance = new ScriptInstance(plugin, scriptFile, scriptClass, classLoader);

      if (!instance.initialize()) {
        plugin.getLogger().severe("Failed to initialize script: " + scriptName);
        return false;
      }

      loadedScripts.put(scriptName, instance);
      plugin.getLogger().info("Loaded: " + scriptName);
      return true;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error loading script: " + scriptName, e);
      return false;
    }
  }

  public boolean unloadScript(String scriptName) {
    ScriptInstance instance = loadedScripts.remove(scriptName);

    if (instance == null) {
      return false;
    }

    try {
      // Execute unregister loops for listeners and command maps inside the target container
      instance.unload();

      scriptDependencies.remove(scriptName);
      compilationCache.remove(scriptName);

      // Force-null local tracking hooks to help out old class loaders clear from memory
      instance = null;

      plugin.debug("Unloaded script: " + scriptName);
      return true;
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error unloading script: " + scriptName, e);
      return false;
    }
  }

  public void unloadAllScripts() {
    List<String> scriptNames = new ArrayList<>(loadedScripts.keySet());
    for (String scriptName : scriptNames) {
      unloadScript(scriptName);
    }
  }

  public void reloadAllScripts() {
    plugin.debug("Reloading all scripts...");

    List<String> scriptNames = new ArrayList<>(loadedScripts.keySet());
    int unloadedCount = 0;

    for (String scriptName : scriptNames) {
      ScriptInstance instance = loadedScripts.remove(scriptName);
      if (instance != null) {
        try {
          instance.unload();
          unloadedCount++;
        } catch (Exception e) {
          plugin.getLogger().log(Level.SEVERE, "Error unloading script: " + scriptName, e);
        }
      }
    }

    plugin.debug("Unloaded " + unloadedCount + " scripts");

    scriptDependencies.clear();

    // Sleep interval blocks giving regional Folia ticks space to safely cycle unregistrations
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      // Ignore
    }

    // Suggest execution cleanups between transitions to keep native memory usage in check
    System.gc();

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Ignore
    }

    loadAllScripts();
  }

  public ScriptInstance getScript(String scriptName) {
    return loadedScripts.get(scriptName);
  }

  public Map<String, ScriptInstance> getLoadedScripts() {
    return Collections.unmodifiableMap(loadedScripts);
  }

  public File getScriptsFolder() {
    return scriptsFolder;
  }

  private void createExampleScripts() {
    copyExampleFromResources("examples/Example.java", "Example.java");
  }

  private void copyExampleFromResources(String resourcePath, String targetFileName) {
    try {
      var resource = plugin.getResource(resourcePath);
      if (resource == null) {
        plugin.getLogger().warning("example script not found in resources: " + resourcePath);
        return;
      }

      File targetFile = new File(scriptsFolder, targetFileName);
      if (targetFile.exists()) {
        return;
      }

      Files.copy(resource, targetFile.toPath());
      plugin.debug("Created example script: " + targetFileName);
    } catch (IOException e) {
      plugin.getLogger().log(Level.WARNING, "Failed to copy example script: " + resourcePath, e);
    }
  }

  public JavaSkriptPlugin getPlugin() {
    return plugin;
  }

  public boolean disableScript(String scriptName) {
    if (!scriptName.endsWith(".java")) {
      scriptName += ".java";
    }

    disabledScripts.add(scriptName);
    saveDisabledScripts();

    if (loadedScripts.containsKey(scriptName)) {
      unloadScript(scriptName);
    }

    plugin.debug("Disabled script: " + scriptName);
    return true;
  }

  public boolean enableScript(String scriptName) {
    if (!scriptName.endsWith(".java")) {
      scriptName += ".java";
    }

    boolean wasDisabled = disabledScripts.remove(scriptName);

    if (!wasDisabled) {
      return false;
    }

    saveDisabledScripts();

    File scriptFile = new File(scriptsFolder, scriptName);
    if (scriptFile.exists()) {
      loadScript(scriptFile);
    }

    plugin.debug("Enabled script: " + scriptName);
    return true;
  }

  public boolean isScriptDisabled(String scriptName) {
    if (!scriptName.endsWith(".java")) {
      scriptName += ".java";
    }
    return disabledScripts.contains(scriptName);
  }

  public Set<String> getDisabledScripts() {
    return Collections.unmodifiableSet(disabledScripts);
  }

  private void loadDisabledScripts() {
    if (!disabledFile.exists()) {
      return;
    }

    // Manual string parsing loops to extract items out of basic JSON array lines
    try {
      String content = Files.readString(disabledFile.toPath());

      content = content.trim();
      if (content.startsWith("[") && content.endsWith("]")) {
        content = content.substring(1, content.length() - 1);

        if (!content.trim().isEmpty()) {
          String[] scripts = content.split(",");
          for (String script : scripts) {
            script = script.trim();
            if (script.startsWith("\"") && script.endsWith("\"")) {
              script = script.substring(1, script.length() - 1);
            }
            if (!script.isEmpty()) {
              disabledScripts.add(script);
            }
          }
        }
      }

      plugin.debug("Loaded " + disabledScripts.size() + " disabled script(s)");
    } catch (IOException e) {
      plugin.getLogger().log(Level.WARNING, "Failed to load disabled scripts list", e);
    }
  }

  private void saveDisabledScripts() {
    try {
      if (!plugin.getDataFolder().exists()) {
        plugin.getDataFolder().mkdirs();
      }

      StringBuilder json = new StringBuilder("[\n");

      int i = 0;
      for (String script : disabledScripts) {
        if (i > 0) {
          json.append(",\n");
        }
        json.append("  \"").append(script).append("\"");
        i++;
      }

      json.append("\n]");

      Files.writeString(disabledFile.toPath(), json.toString());
    } catch (IOException e) {
      plugin.getLogger().log(Level.WARNING, "Failed to save disabled scripts list", e);
    }
  }

  /**
   * Extract Maven dependencies from script source code Supports both @ScriptDependency annotation
   * and // @dependency comments
   */
  private List<String> extractDependencies(String sourceCode) {
    List<String> dependencies = new ArrayList<>();

    // Parser block 1: Scrapes @ScriptDependency blocks via structured RegEx matchers
    java.util.regex.Pattern annotationPattern =
        java.util.regex.Pattern.compile("@ScriptDependenc(?:y|ies)\\s*\\(\\s*\\{([^}]+)\\}\\s*\\)");
    java.util.regex.Matcher annotationMatcher = annotationPattern.matcher(sourceCode);

    if (annotationMatcher.find()) {
      String dependenciesStr = annotationMatcher.group(1);
      java.util.regex.Pattern quotePattern = java.util.regex.Pattern.compile("\"([^\"]+)\"");
      java.util.regex.Matcher quoteMatcher = quotePattern.matcher(dependenciesStr);

      while (quoteMatcher.find()) {
        String dep = quoteMatcher.group(1).trim();
        if (!dep.isEmpty()) {
          dependencies.add(dep);
        }
      }
    }

    // Parser block 2: Inline line comments parsing strategy (// @dependency coordinates)
    java.util.regex.Pattern commentPattern =
        java.util.regex.Pattern.compile("//\\s*@dependency\\s+([^\\s]+)");
    java.util.regex.Matcher commentMatcher = commentPattern.matcher(sourceCode);

    while (commentMatcher.find()) {
      String dep = commentMatcher.group(1).trim();
      if (!dep.isEmpty() && !dependencies.contains(dep)) {
        dependencies.add(dep);
      }
    }

    return dependencies;
  }

  /**
   * Check if script uses folders properly (script-data folder only)
   *
   * @param scriptName The script name
   * @param sourceCode The script source code
   * @return true if script is safe to load, false if it violates folder rules
   */
  private boolean checkScriptFolderUsage(String scriptName, String sourceCode) {
    if (plugin.getConfig().getBoolean("scripts.allow-unrestricted-folders", false)) {
      return true;
    }

    // RegEx block tracking File instances targeted out-of-bounds from the layout standard
    java.util.regex.Pattern badFolderPattern =
        java.util.regex.Pattern.compile(
            "new\\s+File\\s*\\(\\s*(?:[^,]+\\.)?getDataFolder\\(\\)\\s*,\\s*\"([^\"]+)\"\\s*\\)");

    java.util.regex.Matcher matcher = badFolderPattern.matcher(sourceCode);

    List<String> violations = new ArrayList<>();

    while (matcher.find()) {
      String folderName = matcher.group(1);

      if (folderName.equals("script-data")) {
        continue;
      }

      if (folderName.equals("scripts") || folderName.equals("libs") || folderName.equals("temp")) {
        continue;
      }

      violations.add(folderName);
    }

    if (!violations.isEmpty()) {
      plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      plugin.getLogger().severe("SCRIPT REJECTED: Improper folder usage detected!");
      plugin.getLogger().severe("");
      plugin.getLogger().severe("Script: " + scriptName);
      plugin.getLogger().severe("Attempted to create folder(s): " + String.join(", ", violations));
      plugin.getLogger().severe("");
      plugin.getLogger().severe("Scripts MUST use the script-data folder for all data storage!");
      plugin.getLogger().severe("");
      plugin.getLogger().severe("WRONG:");
      plugin
          .getLogger()
          .severe(
              "  new File(JavaSkriptPlugin.getInstance().getDataFolder(), \""
                  + violations.get(0)
                  + "\")");
      plugin.getLogger().severe("");
      plugin.getLogger().severe("CORRECT:");
      plugin
          .getLogger()
          .severe(
              "  File scriptData = new File(JavaSkriptPlugin.getInstance().getDataFolder(),"
                  + " \"script-data\");");
      plugin.getLogger().severe("  File myFolder = new File(scriptData, \"MyScriptName\");");
      plugin.getLogger().severe("  File subFolder = new File(myFolder, \"subfolder\"); // OK!");
      plugin.getLogger().severe("");
      plugin
          .getLogger()
          .severe(
              "To disable this check, set 'scripts.allow-unrestricted-folders: true' in"
                  + " config.yml");
      plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      return false;
    }

    return true;
  }
}
