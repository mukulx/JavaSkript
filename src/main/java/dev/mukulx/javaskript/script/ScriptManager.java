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
  // Only a real marker counts: an annotation (optionally after other annotations) or a comment
  // that starts its line. A mention of "@Disabled" inside prose or a string must not skip a script.
  private static final Pattern DISABLED_MARKER_PATTERN =
      Pattern.compile(
          "(?m)^\\s*(?:(?:@\\w+(?:\\([^)]*\\))?\\s+)*@Disabled\\b"
              + "|(?://\\s*|/\\*\\s*)@disabled\\b)");
  private static final Pattern DEPENDENCY_QUOTE_PATTERN = Pattern.compile("\"([^\"]+)\"");
  private static final Pattern DEPENDENCY_COMMENT_PATTERN =
      Pattern.compile("//\\s*@dependency\\s+([^\\s]+)");

  private final JavaSkriptPlugin plugin;
  private final File scriptsFolder;
  private final Map<String, ScriptInstance> loadedScripts;
  private final Set<String> disabledScripts;
  private final ScriptCompiler compiler;
  private final File disabledFile;
  // Dependencies resolved off-thread, handed to the follow-up load of the same script
  private final Map<String, ResolvedDependencies> pendingDependencies;
  private final Map<String, CachedScript> compilationCache;

  private record ResolvedDependencies(List<String> coordinates, List<File> files) {}

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
    this.pendingDependencies = new ConcurrentHashMap<>();
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

  /** Reject keys that could point outside the scripts folder. */
  private boolean isSafeKey(String key) {
    if (key == null || key.isEmpty() || key.indexOf('\0') >= 0) {
      return false;
    }
    String normalized = key.replace('\\', '/');
    if (normalized.startsWith("/")
        || normalized.matches("^[A-Za-z]:.*")
        || new File(normalized).isAbsolute()) {
      return false;
    }
    for (String part : normalized.split("/")) {
      if (part.equals("..")) {
        return false;
      }
    }
    return true;
  }

  /**
   * Resolve a script key (relative path or simple name) to a File. Checks exact path, dash-prefixed
   * variants, and recursively searches subfolders.
   */
  public File resolveScriptFile(String scriptKey) {
    if (!isSafeKey(scriptKey)) {
      // Nothing outside scripts/ may be resolved; callers treat a missing file as "not found"
      return new File(scriptsFolder, "invalid-script-key");
    }
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
    if (path == null || path.isEmpty() || !isSafeKey(path)) return null;
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
    while (dirKey.startsWith("/")) dirKey = dirKey.substring(1);
    while (dirKey.endsWith("/")) dirKey = dirKey.substring(0, dirKey.length() - 1);
    String[] parts = dirKey.split("/");
    for (int i = 0; i < parts.length; i++) {
      while (parts[i].startsWith("-")) {
        parts[i] = parts[i].substring(1);
      }
    }
    dirKey = String.join("/", parts);

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

  /** Enable an entire directory by un-dashing it (if dashed) and enabling all scripts inside. */
  public int enableDirectory(File dir) {
    if (dir == null || !dir.isDirectory()) return -1;
    File targetDir = dir;
    String name = dir.getName();
    if (name.startsWith("-")) {
      File newDir = new File(dir.getParentFile(), name.substring(1));
      if (dir.renameTo(newDir)) {
        targetDir = newDir;
      }
    }

    final File finalTargetDir = targetDir;
    final Path targetPath = targetDir.toPath();

    // Un-dash all dashed files and subdirectories inside targetDir depth-first
    try (Stream<Path> paths = Files.walk(targetPath)) {
      List<Path> allPaths =
          paths
              .filter(p -> !p.equals(targetPath))
              .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
              .collect(java.util.stream.Collectors.toList());

      for (Path p : allPaths) {
        String baseName = p.getFileName().toString();
        if (baseName.startsWith("-")) {
          File f = p.toFile();
          File parent = f.getParentFile();
          if (parent != null) {
            File unDashed = new File(parent, baseName.substring(1));
            f.renameTo(unDashed);
          }
        }
      }
    } catch (IOException e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "Error un-dashing directory contents: " + finalTargetDir.getName(), e);
    }

    // Remove any script keys in targetDir from disabledScripts
    String dirKey = getScriptKey(finalTargetDir);
    String prefix = dirKey.isEmpty() ? "" : dirKey + "/";
    boolean changed = disabledScripts.removeIf(key -> key.startsWith(prefix) || key.equals(dirKey));
    if (changed) {
      saveDisabledScripts();
    }

    return loadDirectory(finalTargetDir);
  }

  /**
   * Enable all scripts across all directories: un-dashes all files and folders, clears
   * disabled-scripts.json, and loads all scripts.
   */
  public int enableAllScripts() {
    if (!scriptsFolder.exists() || !scriptsFolder.isDirectory()) {
      return 0;
    }

    try (Stream<Path> paths = Files.walk(scriptsFolder.toPath())) {
      List<Path> allPaths =
          paths
              .filter(p -> !p.equals(scriptsFolder.toPath()))
              .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
              .collect(java.util.stream.Collectors.toList());

      for (Path p : allPaths) {
        String baseName = p.getFileName().toString();
        if (baseName.startsWith("-")) {
          File f = p.toFile();
          File parent = f.getParentFile();
          if (parent != null) {
            File unDashed = new File(parent, baseName.substring(1));
            f.renameTo(unDashed);
          }
        }
      }
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Error enabling all scripts on disk", e);
    }

    disabledScripts.clear();
    saveDisabledScripts();

    loadAllScripts();
    return loadedScripts.size();
  }

  /**
   * Disable all scripts: renames all non-dashed .java files to have a '-' prefix, and unloads all
   * scripts.
   */
  public int disableAllScripts() {
    if (!scriptsFolder.exists() || !scriptsFolder.isDirectory()) {
      return 0;
    }

    int disabledCount = 0;
    try (Stream<Path> paths = Files.walk(scriptsFolder.toPath())) {
      List<File> files =
          paths
              .filter(Files::isRegularFile)
              .filter(p -> p.toString().endsWith(".java"))
              .map(Path::toFile)
              .filter(f -> !f.getName().startsWith("-"))
              .collect(java.util.stream.Collectors.toList());

      for (File file : files) {
        File parent = file.getParentFile();
        File dashed = new File(parent, "-" + file.getName());
        if (file.renameTo(dashed)) {
          disabledCount++;
        } else {
          disabledScripts.add(getScriptKey(file));
          disabledCount++;
        }
      }
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Error disabling all scripts on disk", e);
    }

    saveDisabledScripts();
    int unloaded = loadedScripts.size();
    unloadAllScripts();
    return Math.max(disabledCount, unloaded);
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

  public synchronized boolean loadScript(File scriptFile) {
    if (scriptFile == null || !scriptFile.exists()) {
      plugin.getLogger().warning("Script file does not exist: " + scriptFile);
      return false;
    }

    String scriptKey = getScriptKey(scriptFile);

    // Check if dash-disabled (filename starts with -)
    if (isDashDisabled(scriptFile)) {
      plugin.debug("Script is dash-disabled, skipping: " + scriptKey);
      if (loadedScripts.containsKey(scriptKey)) {
        unloadScript(scriptKey);
      }
      return false;
    }

    // Check if disabled via disabled-scripts.json
    if (disabledScripts.contains(scriptKey)) {
      plugin.debug("Script is disabled, skipping: " + scriptKey);
      if (loadedScripts.containsKey(scriptKey)) {
        unloadScript(scriptKey);
      }
      return false;
    }

    try {
      boolean isReload = loadedScripts.containsKey(scriptKey);

      plugin.debug("Loading script: " + scriptKey);

      String scriptContent = Files.readString(scriptFile.toPath());

      // Fire pre-compile event allowing external plugins/addons to preprocess or cancel
      dev.mukulx.javaskript.event.ScriptPreCompileEvent preCompileEvent =
          new dev.mukulx.javaskript.event.ScriptPreCompileEvent(scriptKey, scriptContent);
      try {
        org.bukkit.Bukkit.getPluginManager().callEvent(preCompileEvent);
      } catch (Throwable t) {
        plugin.getLogger().warning("Error in ScriptPreCompileEvent handler: " + t.getMessage());
      }
      if (preCompileEvent.isCancelled()) {
        plugin.debug("Script compilation cancelled by external plugin event: " + scriptKey);
        return false;
      }
      scriptContent = preCompileEvent.getSourceCode();

      // Early check for @Disabled annotation or // @disabled comment before compiling/resolving
      if (DISABLED_MARKER_PATTERN.matcher(scriptContent).find()) {
        plugin.debug("Script marked as disabled in file, skipping: " + scriptKey);
        if (isReload) {
          unloadScript(scriptKey);
        }
        return false;
      }

      // Read metadata annotations and pull external Maven dependencies.
      // Downloads run off the game thread so the server never freezes while Maven responds.
      List<String> dependencies = extractDependencies(scriptContent);
      List<File> dependencyFiles = new ArrayList<>();

      if (!dependencies.isEmpty()) {
        // A background resolve for this exact dependency set already finished; use its result
        ResolvedDependencies handoff = pendingDependencies.remove(scriptKey);
        if (handoff != null && handoff.coordinates().equals(dependencies)) {
          dependencyFiles = handoff.files();
        } else {
          boolean onGameThread = false;
          try {
            onGameThread = org.bukkit.Bukkit.isPrimaryThread();
          } catch (Throwable ignored) {
            onGameThread = false;
          }
          if (onGameThread) {
            final String queuedKey = scriptKey;
            final File queuedFile = scriptFile;
            plugin
                .getLogger()
                .info(
                    "Resolving "
                        + dependencies.size()
                        + " dependencies for "
                        + scriptKey
                        + " in the background");
            java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> resolveDependenciesBlocking(dependencies, queuedKey))
                .thenAccept(
                    resolved -> {
                      pendingDependencies.put(
                          queuedKey, new ResolvedDependencies(dependencies, resolved));
                      dev.mukulx.javaskript.util.ServerUtil.runSync(
                          plugin,
                          () -> {
                            try {
                              loadScript(queuedFile);
                            } catch (Throwable t) {
                              plugin
                                  .getLogger()
                                  .warning(
                                      "Background dependency load failed for "
                                          + queuedKey
                                          + ": "
                                          + t.getMessage());
                            }
                          });
                    });
            // Queued in the background; the second pass finishes the load without blocking.
            return false;
          }

          plugin
              .getLogger()
              .info("Resolving " + dependencies.size() + " dependencies for " + scriptKey);
          dependencyFiles = resolveDependenciesBlocking(dependencies, scriptKey);
        }
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
          if (isReload) {
            unloadScript(scriptKey);
          }
          classLoader.unloadAll();
          return false;
        }

        // Do not take a running script down for a bad edit. Compilation and class validation have
        // completed successfully; only now is it safe to replace the active instance.
        if (isReload) {
          unloadScript(scriptKey, true);
        }

        ScriptInstance instance = new ScriptInstance(plugin, scriptFile, scriptClass, classLoader);

        if (!instance.initialize()) {
          plugin.getLogger().severe("Failed to initialize script: " + scriptKey);
          classLoader.unloadAll();
          return false;
        }

        loadedScripts.put(scriptKey, instance);
        plugin.getLogger().info("Loaded: " + scriptKey);

        // Fire lifecycle events for external plugins and addons
        try {
          if (isReload) {
            org.bukkit.Bukkit.getPluginManager()
                .callEvent(new dev.mukulx.javaskript.event.ScriptReloadEvent(scriptKey, instance));
          }
          org.bukkit.Bukkit.getPluginManager()
              .callEvent(
                  new dev.mukulx.javaskript.event.ScriptLoadEvent(
                      scriptKey, instance, scriptClass));
        } catch (Throwable t) {
          plugin.getLogger().warning("Error in ScriptLoadEvent handler: " + t.getMessage());
        }

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
    return unloadScript(scriptKey, false);
  }

  /**
   * @param keepCompilationCache true when the script is being replaced by a fresh load, so the
   *     bytecode just compiled for it must survive the unload of the old instance
   */
  private boolean unloadScript(String scriptKey, boolean keepCompilationCache) {
    // Normalize: strip .java if missing, normalize slashes
    scriptKey = normalizeKey(scriptKey);

    ScriptInstance instance = loadedScripts.remove(scriptKey);

    if (instance == null) {
      return false;
    }

    try {
      // Fire unload lifecycle event for external plugins and addons
      try {
        org.bukkit.Bukkit.getPluginManager()
            .callEvent(new dev.mukulx.javaskript.event.ScriptUnloadEvent(scriptKey, instance));
      } catch (Throwable t) {
        plugin.getLogger().warning("Error in ScriptUnloadEvent handler: " + t.getMessage());
      }

      instance.unload();
      pendingDependencies.remove(scriptKey);
      if (!keepCompilationCache) {
        compilationCache.remove(scriptKey);
      }
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
      try {
        unloadScript(scriptName);
      } catch (Throwable t) {
        plugin
            .getLogger()
            .log(
                Level.SEVERE,
                "Error unloading script " + scriptName + " (continuing): " + t.getMessage(),
                t);
      }
    }
  }

  public void reloadAllScripts() {
    plugin.debug("Reloading all scripts...");

    List<String> scriptNames = new ArrayList<>(loadedScripts.keySet());
    int unloadedCount = 0;

    for (String scriptName : scriptNames) {
      try {
        if (unloadScript(scriptName)) {
          unloadedCount++;
        }
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Error unloading script: " + scriptName, e);
      }
    }

    plugin.debug("Unloaded " + unloadedCount + " scripts");
    pendingDependencies.clear();
    compilationCache.clear();
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
    if (!isSafeKey(scriptKey)) {
      plugin.getLogger().warning("Refusing to disable script with unsafe key: " + scriptKey);
      return false;
    }

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
    if (!isSafeKey(scriptKey)) {
      plugin.getLogger().warning("Refusing to enable script with unsafe key: " + scriptKey);
      return false;
    }

    boolean wasDisabled = false;

    // Check for dash-prefixed file and rename it back
    File dashFile = resolveScriptFile(scriptKey);
    if (dashFile.exists() && isDashDisabled(dashFile)) {
      String fileName = dashFile.getName();
      if (fileName.startsWith("-")) {
        File parent = dashFile.getParentFile();
        File normalFile = new File(parent, fileName.replaceFirst("^-+", ""));
        if (dashFile.renameTo(normalFile)) {
          wasDisabled = true;
          plugin.debug("Un-dashed script: " + scriptKey);
          // Load the now-enabled script
          loadScript(normalFile);
        }
      } else {
        // Disabled by a dashed parent folder. Renaming the file would corrupt its name, and
        // un-dashing the folder would enable every sibling script.
        plugin
            .getLogger()
            .warning(
                "Cannot enable "
                    + scriptKey
                    + " on its own: it sits inside a disabled (dashed) folder. Enable the folder.");
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
                if (!dirs.contains(relative)) {
                  dirs.add(relative);
                }
                String[] parts = relative.split("/");
                for (int i = 0; i < parts.length; i++) {
                  while (parts[i].startsWith("-")) {
                    parts[i] = parts[i].substring(1);
                  }
                }
                String clean = String.join("/", parts);
                if (!dirs.contains(clean)) {
                  dirs.add(clean);
                }
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

  /**
   * True when this exact file is already loaded and has not been modified since it was compiled.
   * Renames and folder moves keep the modification time, so the watcher uses this to avoid
   * reloading (and re-running onEnable for) scripts that did not change.
   */
  public boolean isLoadedAndUnchanged(File scriptFile) {
    String scriptKey = getScriptKey(scriptFile);
    ScriptInstance instance = loadedScripts.get(scriptKey);
    CachedScript cached = compilationCache.get(scriptKey);
    return instance != null
        && cached != null
        && instance.getScriptFile().toPath().normalize().equals(scriptFile.toPath().normalize())
        && cached.lastModified == scriptFile.lastModified();
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
      "TeamExample.java",
      "EconomyExample.java",
      "SharedVariablesAndEventsExample.java",
      "HttpAndDiscordExample.java",
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

  /** Resolve Maven coordinates off the game thread. Never call this on the main thread. */
  private List<File> resolveDependenciesBlocking(List<String> dependencies, String scriptKey) {
    List<File> dependencyFiles = new ArrayList<>();
    List<java.util.concurrent.CompletableFuture<List<File>>> futures =
        dependencies.stream()
            .map(
                dep ->
                    java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> plugin.getDependencyManager().resolveDependency(dep)))
            .collect(java.util.stream.Collectors.toList());

    // One shared deadline, so a hung download can't block the load forever
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(5);
    for (java.util.concurrent.CompletableFuture<List<File>> future : futures) {
      try {
        long remaining = Math.max(1L, deadline - System.nanoTime());
        List<File> resolved = future.get(remaining, java.util.concurrent.TimeUnit.NANOSECONDS);
        if (resolved == null || resolved.isEmpty()) {
          plugin.getLogger().warning("Failed to resolve a dependency for " + scriptKey);
        } else {
          dependencyFiles.addAll(resolved);
        }
      } catch (java.util.concurrent.TimeoutException e) {
        future.cancel(true);
        plugin.getLogger().warning("Timed out resolving a dependency for " + scriptKey);
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning("Error resolving dependency for " + scriptKey + ": " + e.getMessage());
      }
    }
    return dependencyFiles;
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
