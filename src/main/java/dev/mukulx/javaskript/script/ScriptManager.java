package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.compiler.ScriptCompiler;
import dev.mukulx.javaskript.script.loader.ScriptClassLoader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ScriptManager {

  private static final Pattern DEPENDENCY_ANNOTATION_PATTERN =
      Pattern.compile("@ScriptDependenc(?:y|ies)\\s*\\(([^)]+)\\)");
  private static final Pattern DEPENDENCY_QUOTE_PATTERN = Pattern.compile("\"([^\"]+)\"");
  private static final Pattern DEPENDENCY_COMMENT_PATTERN =
      Pattern.compile("//\\s*@dependency\\s+([^\\s]+)");
  private static final Pattern BAD_FOLDER_PATTERN =
      Pattern.compile(
          "new\\s+File\\s*\\(\\s*(?:[^,]+\\.)?getDataFolder\\(\\)\\s*,\\s*\"([^\"]+)\"\\s*\\)");

  private final JavaSkriptPlugin plugin;
  private final File scriptsFolder;
  private final Map<String, ScriptInstance> loadedScripts;
  private final Set<String> disabledScripts;
  private final ScriptCompiler compiler;
  private final File disabledFile;
  private final Map<String, List<File>> scriptDependencies;
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

    loadDisabledScripts();
  }

  // ==========================================
  // Path Utilities
  // ==========================================

  /**
   * Get the relative path of a script file from the scripts folder. Uses forward slashes for
   * consistency across platforms.
   */
  public String getRelativePath(File scriptFile) {
    Path base = scriptsFolder.toPath().normalize();
    Path target = scriptFile.toPath().normalize();
    String relative = base.relativize(target).toString();
    return relative.replace('\\', '/');
  }

  /**
   * Get the script key used for tracking. For dash-disabled files or folders, the key is the path
   * without leading dashes on folder/file names (e.g., "-examples/-Example.java" ->
   * "examples/Example.java").
   */
  public String getScriptKey(File scriptFile) {
    String relative = getRelativePath(scriptFile);
    String[] parts = relative.split("/");
    for (int i = 0; i < parts.length; i++) {
      while (parts[i].startsWith("-")) {
        parts[i] = parts[i].substring(1);
      }
    }
    return String.join("/", parts);
  }

  /**
   * Check if a script file is dash-disabled. Returns true if the file name starts with '-', or if
   * any parent directory up to the scripts folder starts with '-', or if it ends with '.disabled'.
   */
  public boolean isDashDisabled(File scriptFile) {
    if (scriptFile == null) return false;
    Path base = scriptsFolder.toPath().normalize();
    Path current = scriptFile.toPath().normalize();
    while (current != null && current.startsWith(base) && !current.equals(base)) {
      String name = current.getFileName().toString();
      if (name.startsWith("-") || name.endsWith(".disabled")) {
        return true;
      }
      current = current.getParent();
    }
    return false;
  }

  /**
   * Resolve a script key (relative path or simple name) to a File. Checks exact path, dash-prefixed
   * variants, and recursively searches subfolders.
   */
  public File resolveScriptFile(String scriptKey) {
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }
    scriptKey = scriptKey.replace('\\', '/');

    // 1. Direct match
    File normal = new File(scriptsFolder, scriptKey);
    if (normal.exists()) {
      return normal;
    }

    // 2. Direct dash-prefixed filename match
    int lastSlash = scriptKey.lastIndexOf('/');
    String dashName =
        (lastSlash >= 0)
            ? scriptKey.substring(0, lastSlash + 1) + "-" + scriptKey.substring(lastSlash + 1)
            : "-" + scriptKey;
    File dashed = new File(scriptsFolder, dashName);
    if (dashed.exists()) {
      return dashed;
    }

    // 3. Dash-prefixed directory match (e.g. -examples/Script.java)
    String dashedDir = "-" + scriptKey;
    File dashedDirFile = new File(scriptsFolder, dashedDir);
    if (dashedDirFile.exists()) {
      return dashedDirFile;
    }

    // 4. Recursive search: match by filename or script key suffix
    String simpleName = (lastSlash >= 0) ? scriptKey.substring(lastSlash + 1) : scriptKey;
    final String targetKey = scriptKey;
    final String targetName = simpleName;
    try (Stream<Path> paths = Files.walk(scriptsFolder.toPath())) {
      Optional<Path> found =
          paths
              .filter(Files::isRegularFile)
              .filter(
                  path -> {
                    String fn = path.getFileName().toString();
                    String rel = getRelativePath(path.toFile());
                    String key = getScriptKey(path.toFile());
                    return fn.equalsIgnoreCase(targetName)
                        || fn.equalsIgnoreCase("-" + targetName)
                        || rel.equalsIgnoreCase(targetKey)
                        || key.equalsIgnoreCase(targetKey);
                  })
              .findFirst();

      if (found.isPresent()) {
        return found.get().toFile();
      }
    } catch (IOException ignored) {
    }

    return normal;
  }

  /** Check if a path refers to a directory inside the scripts folder. */
  public boolean isDirectory(String path) {
    File dir = resolveDirectory(path);
    return dir != null && dir.isDirectory();
  }

  /** Resolve a directory name or path (supporting dash prefixes). */
  public File resolveDirectory(String path) {
    if (path == null || path.isEmpty()) return null;
    path = path.replace('\\', '/');
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    File normal = new File(scriptsFolder, path);
    if (normal.exists() && normal.isDirectory()) return normal;

    // Check with leading dash on the folder (e.g. -examples)
    int lastSlash = path.lastIndexOf('/');
    String dashPath =
        (lastSlash >= 0)
            ? path.substring(0, lastSlash + 1) + "-" + path.substring(lastSlash + 1)
            : "-" + path;
    File dashed = new File(scriptsFolder, dashPath);
    if (dashed.exists() && dashed.isDirectory()) return dashed;

    return null;
  }

  /** Load all scripts in a directory recursively. */
  public int loadDirectory(File dir) {
    if (dir == null || !dir.isDirectory()) return 0;
    int count = 0;
    try (Stream<Path> paths = Files.walk(dir.toPath())) {
      List<File> files =
          paths
              .filter(Files::isRegularFile)
              .filter(p -> p.toString().endsWith(".java"))
              .map(Path::toFile)
              .collect(java.util.stream.Collectors.toList());

      for (File file : files) {
        if (!isDashDisabled(file) && !disabledScripts.contains(getScriptKey(file))) {
          if (loadScript(file)) {
            count++;
          }
        }
      }
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Error loading directory: " + dir.getName(), e);
    }
    return count;
  }

  /** Unload all loaded scripts in a directory. */
  public int unloadDirectory(String dirKey) {
    dirKey = dirKey.replace('\\', '/');
    if (dirKey.endsWith("/")) dirKey = dirKey.substring(0, dirKey.length() - 1);
    if (dirKey.startsWith("-")) dirKey = dirKey.substring(1);

    String prefix = dirKey.isEmpty() ? "" : dirKey + "/";
    List<String> toUnload = new ArrayList<>();
    for (String key : loadedScripts.keySet()) {
      if (key.startsWith(prefix) || key.equals(dirKey)) {
        toUnload.add(key);
      }
    }

    for (String key : toUnload) {
      unloadScript(key);
    }
    return toUnload.size();
  }

  /** Reload all scripts in a directory. */
  public int reloadDirectory(File dir) {
    String dirKey = getRelativePath(dir);
    unloadDirectory(dirKey);
    return loadDirectory(dir);
  }

  /** Disable an entire directory by renaming it with a leading dash. */
  public boolean disableDirectory(File dir) {
    if (dir == null || !dir.isDirectory()) return false;
    String name = dir.getName();
    if (name.startsWith("-")) return false; // Already disabled

    unloadDirectory(getRelativePath(dir));
    File newDir = new File(dir.getParentFile(), "-" + name);
    return dir.renameTo(newDir);
  }

  /** Enable an entire directory by renaming it to remove the leading dash. */
  public boolean enableDirectory(File dir) {
    if (dir == null || !dir.isDirectory()) return false;
    String name = dir.getName();
    if (!name.startsWith("-")) return false; // Not disabled

    File newDir = new File(dir.getParentFile(), name.substring(1));
    if (dir.renameTo(newDir)) {
      loadDirectory(newDir);
      return true;
    }
    return false;
  }

  // ==========================================
  // Loading
  // ==========================================

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

    String scriptKey = getScriptKey(scriptFile);

    // Check if dash-disabled (filename starts with -)
    if (isDashDisabled(scriptFile)) {
      plugin.debug("Script is dash-disabled, skipping: " + scriptKey);
      return false;
    }

    // Check if disabled via disabled-scripts.json
    if (disabledScripts.contains(scriptKey)) {
      plugin.debug("Script is disabled, skipping: " + scriptKey);
      return false;
    }

    try {
      // Hot-replace logic: unload active instances prior to compilation tasks
      if (loadedScripts.containsKey(scriptKey)) {
        unloadScript(scriptKey);
      }

      plugin.debug("Loading script: " + scriptKey);

      String scriptContent = Files.readString(scriptFile.toPath());

      // Early check for @Disabled annotation or // @disabled comment before compiling/resolving
      if (scriptContent.contains("@Disabled")
          || scriptContent.contains("// @disabled")
          || scriptContent.contains("/* @disabled */")) {
        plugin.debug("Script marked as disabled in file, skipping: " + scriptKey);
        return false;
      }

      // Regex validation scanner to prevent target scripts from breaking directory layouts
      if (!checkScriptFolderUsage(scriptKey, scriptContent)) {
        plugin.getLogger().severe("Script rejected due to improper folder usage: " + scriptKey);
        return false;
      }

      // Read metadata annotations and spawn async threads to pull external Maven dependencies
      List<String> dependencies = extractDependencies(scriptContent);
      List<File> dependencyFiles = new ArrayList<>();

      if (!dependencies.isEmpty()) {
        plugin
            .getLogger()
            .info("Resolving " + dependencies.size() + " dependencies for " + scriptKey);

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

        scriptDependencies.put(scriptKey, dependencyFiles);
      }

      long lastModified = scriptFile.lastModified();
      String contentHash = computeHash(scriptContent);
      CachedScript cached = compilationCache.get(scriptKey);
      Map<String, byte[]> compiledClasses;

      if (cached != null
          && cached.lastModified == lastModified
          && cached.contentHash.equals(contentHash)) {
        plugin.debug("Using cached compilation for: " + scriptKey);
        compiledClasses = cached.compiledClasses;
      } else {
        compiledClasses = compiler.compileAll(scriptKey, scriptContent, dependencyFiles);

        if (compiledClasses == null || compiledClasses.isEmpty()) {
          plugin.getLogger().severe("Failed to compile script: " + scriptKey);
          return false;
        }

        compilationCache.put(
            scriptKey, new CachedScript(lastModified, contentHash, compiledClasses));
        plugin.debug("Compiled and cached: " + scriptKey);
      }

      // Instantiate isolated classloader mapping to assign the raw byte array data into real
      // classes
      ScriptClassLoader classLoader = new ScriptClassLoader(plugin, scriptKey, dependencyFiles);
      try {
        Map<String, Class<?>> loadedClasses = classLoader.defineClasses(compiledClasses);

        if (loadedClasses.isEmpty()) {
          plugin.getLogger().severe("Failed to load any classes for script: " + scriptKey);
          classLoader.unloadAll();
          return false;
        }

        // Reflective search setup to locate valid public runtime entry points
        String expectedClassName = compiler.getClassName(scriptKey);
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
                          + scriptKey
                          + ")");
              break;
            }
          }
        }

        if (scriptClass == null) {
          plugin.getLogger().severe("No suitable main class found for: " + scriptKey);
          plugin.getLogger().severe("Available classes: " + loadedClasses.keySet());
          classLoader.unloadAll();
          return false;
        }

        // Check for @Disabled annotation on the compiled class
        if (scriptClass.isAnnotationPresent(Disabled.class)) {
          Disabled disabled = scriptClass.getAnnotation(Disabled.class);
          String reason = disabled.value();
          if (reason != null && !reason.isEmpty()) {
            plugin.debug(
                "Script has @Disabled annotation (" + reason + "), skipping: " + scriptKey);
          } else {
            plugin.debug("Script has @Disabled annotation, skipping: " + scriptKey);
          }
          classLoader.unloadAll();
          return false;
        }

        ScriptInstance instance = new ScriptInstance(plugin, scriptFile, scriptClass, classLoader);

        if (!instance.initialize()) {
          plugin.getLogger().severe("Failed to initialize script: " + scriptKey);
          classLoader.unloadAll();
          return false;
        }

        loadedScripts.put(scriptKey, instance);
        plugin.getLogger().info("Loaded: " + scriptKey);
        return true;
      } catch (Exception e) {
        classLoader.unloadAll();
        throw e;
      }

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error loading script: " + scriptKey, e);
      return false;
    }
  }

  // ==========================================
  // Unloading & Reloading
  // ==========================================

  public boolean unloadScript(String scriptKey) {
    // Normalize: strip .java if missing, normalize slashes
    scriptKey = normalizeKey(scriptKey);

    ScriptInstance instance = loadedScripts.remove(scriptKey);

    if (instance == null) {
      return false;
    }

    try {
      instance.unload();
      scriptDependencies.remove(scriptKey);
      compilationCache.remove(scriptKey);
      plugin.debug("Unloaded script: " + scriptKey);
      return true;
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error unloading script: " + scriptKey, e);
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
    loadAllScripts();
  }

  // ==========================================
  // Enable / Disable
  // ==========================================

  /**
   * Disable a script. Tries dash-prefix renaming first (rename file to -Name.java). Falls back to
   * disabled-scripts.json if rename fails.
   */
  public boolean disableScript(String scriptKey) {
    scriptKey = normalizeKey(scriptKey);

    File scriptFile = resolveScriptFile(scriptKey);
    boolean renamed = false;

    // Try dash-prefix rename
    if (scriptFile.exists() && !isDashDisabled(scriptFile)) {
      File parent = scriptFile.getParentFile();
      File dashedFile = new File(parent, "-" + scriptFile.getName());
      if (scriptFile.renameTo(dashedFile)) {
        renamed = true;
        plugin.debug("Dash-disabled script: " + scriptKey);
      }
    }

    // Fallback: add to disabled-scripts.json
    if (!renamed) {
      disabledScripts.add(scriptKey);
      saveDisabledScripts();
    }

    // Unload if currently loaded
    if (loadedScripts.containsKey(scriptKey)) {
      unloadScript(scriptKey);
    }

    plugin.debug("Disabled script: " + scriptKey);
    return true;
  }

  /**
   * Enable a script. Checks for dash-prefix file first (rename -Name.java back to Name.java). Also
   * removes from disabled-scripts.json.
   */
  public boolean enableScript(String scriptKey) {
    scriptKey = normalizeKey(scriptKey);

    boolean wasDisabled = false;

    // Check for dash-prefixed file and rename it back
    File dashFile = resolveScriptFile(scriptKey);
    if (dashFile.exists() && isDashDisabled(dashFile)) {
      File parent = dashFile.getParentFile();
      String normalName = dashFile.getName().substring(1); // Remove leading '-'
      File normalFile = new File(parent, normalName);
      if (dashFile.renameTo(normalFile)) {
        wasDisabled = true;
        plugin.debug("Un-dashed script: " + scriptKey);
        // Load the now-enabled script
        loadScript(normalFile);
      }
    }

    // Also remove from disabled-scripts.json
    if (disabledScripts.remove(scriptKey)) {
      wasDisabled = true;
      saveDisabledScripts();

      // Load the script if not already loaded
      if (!loadedScripts.containsKey(scriptKey)) {
        File scriptFile = resolveScriptFile(scriptKey);
        if (scriptFile.exists()) {
          loadScript(scriptFile);
        }
      }
    }

    if (!wasDisabled) {
      return false;
    }

    plugin.debug("Enabled script: " + scriptKey);
    return true;
  }

  public boolean isScriptDisabled(String scriptKey) {
    scriptKey = normalizeKey(scriptKey);

    // Check disabled-scripts.json
    if (disabledScripts.contains(scriptKey)) {
      return true;
    }

    // Check dash-prefix on disk
    File resolved = resolveScriptFile(scriptKey);
    return resolved.exists() && isDashDisabled(resolved);
  }

  public Set<String> getDisabledScripts() {
    // Collect all disabled scripts: from json + dash-prefixed files on disk
    Set<String> all = new LinkedHashSet<>(disabledScripts);

    // Walk disk for dash-prefixed files and folders
    try (Stream<Path> paths = Files.walk(scriptsFolder.toPath())) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .filter(path -> isDashDisabled(path.toFile()))
          .forEach(
              path -> {
                String key = getScriptKey(path.toFile());
                all.add(key);
              });
    } catch (IOException ignored) {
    }

    return Collections.unmodifiableSet(all);
  }

  /**
   * Get all script files on disk (both enabled and disabled), returning their script keys. This is
   * useful for tab completion.
   */
  public List<String> getAllScriptKeys() {
    List<String> keys = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(scriptsFolder.toPath())) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                String key = getScriptKey(path.toFile());
                if (!keys.contains(key)) {
                  keys.add(key);
                }
              });
    } catch (IOException ignored) {
    }
    return keys;
  }

  /** Get all subdirectory names under the scripts folder. */
  public List<String> getSubdirectories() {
    List<String> dirs = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(scriptsFolder.toPath())) {
      paths
          .filter(Files::isDirectory)
          .filter(path -> !path.equals(scriptsFolder.toPath()))
          .forEach(
              path -> {
                String relative =
                    scriptsFolder.toPath().relativize(path).toString().replace('\\', '/');
                dirs.add(relative);
              });
    } catch (IOException ignored) {
    }
    return dirs;
  }

  // ==========================================
  // Getters
  // ==========================================

  public ScriptInstance getScript(String scriptKey) {
    scriptKey = normalizeKey(scriptKey);
    return loadedScripts.get(scriptKey);
  }

  public Map<String, ScriptInstance> getLoadedScripts() {
    return Collections.unmodifiableMap(loadedScripts);
  }

  public File getScriptsFolder() {
    return scriptsFolder;
  }

  public JavaSkriptPlugin getPlugin() {
    return plugin;
  }

  // ==========================================
  // Example Scripts (copied to scripts/examples/ with - prefix)
  // ==========================================

  private void createExampleScripts() {
    String[] examples = {
      "Example.java",
      "ActionBarExample.java",
      "AnnounceCommand.java",
      "BossBarExample.java",
      "ConfigExample.java",
      "CustomRecipes.java",
      "DatabaseExample.java",
      "DialogExample.java",
      "ExtraInventory.java",
      "FlyCommand.java",
      "FoliaCompatibleExample.java",
      "GUIExample.java",
      "HealCommand.java",
      "HologramExample.java",
      "LifecycleExample.java",
      "MultiClassExample.java",
      "NBSPlayer.java",
      "PDCExample.java",
      "PaperOnlyExample.java",
      "PermissionExample.java",
      "PlaceholderExample.java",
      "RestartTimerCommand.java",
      "SchedulerExample.java",
      "SoundExample.java",
      "TitleExample.java",
      "WelcomeScript.java",
      "CommandAPIExample.java",
      "ItemBuilderExample.java",
      "CooldownExample.java",
      "EventAndPlayerExample.java",
      "ChatAPIExample.java",
    };

    File examplesDir = new File(scriptsFolder, "examples");
    examplesDir.mkdirs();

    for (String example : examples) {
      copyExampleFromResources("examples/" + example, "examples/-" + example);
    }

    // Copy README too (no dash prefix for non-java files)
    copyExampleFromResources("examples/README.md", "examples/README.md");
  }

  private void copyExampleFromResources(String resourcePath, String targetRelativePath) {
    try {
      InputStream resource = plugin.getResource(resourcePath);
      if (resource == null) {
        plugin.debug("Example script not found in resources: " + resourcePath);
        return;
      }

      File targetFile = new File(scriptsFolder, targetRelativePath);
      if (targetFile.exists()) {
        resource.close();
        return;
      }

      // Ensure parent dirs exist
      targetFile.getParentFile().mkdirs();

      Files.copy(resource, targetFile.toPath());
      resource.close();
      plugin.debug("Created example script: " + targetRelativePath);
    } catch (IOException e) {
      plugin.getLogger().log(Level.WARNING, "Failed to copy example script: " + resourcePath, e);
    }
  }

  // ==========================================
  // Disabled Scripts Persistence (JSON)
  // ==========================================

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

  // ==========================================
  // Utilities
  // ==========================================

  /** Normalize a script key: ensure .java suffix, normalize slashes */
  private String normalizeKey(String key) {
    if (key == null) return "";
    key = key.replace('\\', '/');
    if (!key.endsWith(".java")) {
      key += ".java";
    }
    // Strip leading dash from filename portion
    int lastSlash = key.lastIndexOf('/');
    if (lastSlash >= 0) {
      String dir = key.substring(0, lastSlash + 1);
      String name = key.substring(lastSlash + 1);
      if (name.startsWith("-")) {
        name = name.substring(1);
      }
      key = dir + name;
    } else {
      if (key.startsWith("-")) {
        key = key.substring(1);
      }
    }
    return key;
  }

  /**
   * Extract Maven dependencies from script source code Supports both @ScriptDependency annotation
   * and // @dependency comments
   */
  private List<String> extractDependencies(String sourceCode) {
    List<String> dependencies = new ArrayList<>();

    Matcher annotationMatcher = DEPENDENCY_ANNOTATION_PATTERN.matcher(sourceCode);
    while (annotationMatcher.find()) {
      String dependenciesStr = annotationMatcher.group(1);
      Matcher quoteMatcher = DEPENDENCY_QUOTE_PATTERN.matcher(dependenciesStr);

      while (quoteMatcher.find()) {
        String dep = quoteMatcher.group(1).trim();
        if (!dep.isEmpty() && !dependencies.contains(dep)) {
          dependencies.add(dep);
        }
      }
    }

    Matcher commentMatcher = DEPENDENCY_COMMENT_PATTERN.matcher(sourceCode);
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
   * @param scriptKey The script key
   * @param sourceCode The script source code
   * @return true if script is safe to load, false if it violates folder rules
   */
  private boolean checkScriptFolderUsage(String scriptKey, String sourceCode) {
    if (plugin.getConfig().getBoolean("scripts.allow-unrestricted-folders", false)) {
      return true;
    }

    Matcher matcher = BAD_FOLDER_PATTERN.matcher(sourceCode);
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
      plugin.getLogger().severe("Script: " + scriptKey);
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

  private String computeHash(String content) {
    try {
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      var hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      var hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      return Integer.toHexString(content.hashCode());
    }
  }
}
