package dev.mukulx.javaskript.script.loader;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptClassLoader extends URLClassLoader {

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private final Map<String, Class<?>> loadedClasses;
  // Bytecode not yet defined. Superclasses and interfaces are defined on demand from here, so
  // definition order within a script no longer matters.
  private final Map<String, byte[]> pendingBytecode = new HashMap<>();
  private long totalBytecodeBytes = 0;

  public ScriptClassLoader(JavaSkriptPlugin plugin, String scriptName) {
    this(plugin, scriptName, new ArrayList<>());
  }

  public ScriptClassLoader(JavaSkriptPlugin plugin, String scriptName, List<File> dependencies) {
    super(buildURLs(dependencies), plugin.getClass().getClassLoader());
    this.plugin = plugin;
    this.scriptName = scriptName;
    this.loadedClasses = new HashMap<>();
  }

  private static URL[] buildURLs(List<File> dependencies) {
    try {
      URL[] urls = new URL[dependencies.size()];
      for (int i = 0; i < dependencies.size(); i++) {
        urls[i] = dependencies.get(i).toURI().toURL();
      }
      return urls;
    } catch (Exception e) {
      throw new RuntimeException("Failed to build URLs for dependencies", e);
    }
  }

  /**
   * Define a single class
   *
   * @param name Class name
   * @param classData Bytecode
   * @return The defined class
   */
  public synchronized Class<?> defineClass(String name, byte[] classData) {
    if (name == null || classData == null) {
      throw new IllegalArgumentException("Class name and data cannot be null");
    }

    // Check if already loaded in this script's class loader
    if (loadedClasses.containsKey(name)) {
      return loadedClasses.get(name);
    }

    pendingBytecode.remove(name);
    try {
      Class<?> clazz = defineClass(name, classData, 0, classData.length);
      loadedClasses.put(name, clazz);
      totalBytecodeBytes += classData.length;
      plugin.getLogger().fine("[" + scriptName + "] Defined class: " + name);
      return clazz;
    } catch (Exception e) {
      plugin.getLogger().severe("[" + scriptName + "] Failed to define class: " + name);
      throw new RuntimeException("Failed to define class: " + name, e);
    }
  }

  /**
   * Define multiple classes at once Each script gets its own isolated ClassLoader, so classes with
   * the same name in different scripts won't conflict.
   *
   * @param classes Map of class names to bytecode
   * @return Map of class names to loaded classes
   */
  public synchronized Map<String, Class<?>> defineClasses(Map<String, byte[]> classes) {
    if (classes == null || classes.isEmpty()) {
      throw new IllegalArgumentException("Classes map cannot be null or empty");
    }

    Map<String, Class<?>> definedClasses = new HashMap<>();
    pendingBytecode.putAll(classes);

    for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
      String className = entry.getKey();
      byte[] bytecode = entry.getValue();

      try {
        Class<?> clazz = defineClass(className, bytecode);
        definedClasses.put(className, clazz);
      } catch (Exception e) {
        plugin.getLogger().severe("[" + scriptName + "] Failed to define class: " + className);
        plugin.getLogger().severe("Error: " + e.getMessage());
        // Continue loading other classes
      }
    }

    if (definedClasses.isEmpty()) {
      throw new RuntimeException("Failed to define any classes for script: " + scriptName);
    }

    plugin.debug(
        "["
            + scriptName
            + "] Loaded "
            + definedClasses.size()
            + " class(es) in isolated ClassLoader");
    return definedClasses;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    Class<?> clazz = loadedClasses.get(name);
    if (clazz != null) {
      return clazz;
    }
    byte[] pending;
    synchronized (this) {
      pending = pendingBytecode.get(name);
    }
    if (pending != null) {
      return defineClass(name, pending);
    }
    try {
      return super.findClass(name);
    } catch (ClassNotFoundException notInDependencies) {
      // JavaSkript is a Paper plugin, so its own loader only sees the plugins it declares. Scripts
      // compile against every loaded plugin jar, so look those plugins up here as well.
      for (var other : plugin.getServer().getPluginManager().getPlugins()) {
        ClassLoader otherLoader = other.getClass().getClassLoader();
        if (other == plugin || otherLoader == getParent()) {
          continue;
        }
        try {
          return otherLoader.loadClass(name);
        } catch (ClassNotFoundException | LinkageError ignored) {
        }
      }
      throw notInDependencies;
    }
  }

  /**
   * Get all loaded classes
   *
   * @return Map of class names to classes
   */
  public Map<String, Class<?>> getLoadedClasses() {
    return new HashMap<>(loadedClasses);
  }

  /**
   * Check if a class is loaded
   *
   * @param name Class name
   * @return true if loaded
   */
  public boolean isClassLoaded(String name) {
    return loadedClasses.containsKey(name);
  }

  public long getTotalBytecodeBytes() {
    return totalBytecodeBytes;
  }

  public synchronized void unloadAll() {
    loadedClasses.clear();
    pendingBytecode.clear();
    try {
      close();
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(
              java.util.logging.Level.WARNING, "[" + scriptName + "] Error closing ClassLoader", e);
    }
  }
}
