package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * High-performance, thread-safe configuration management for scripts.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>O(1) in-memory cached reads (no disk I/O on every tick or event)
 *   <li>Default zero-boilerplate overloads assuming "config.yml"
 *   <li>Multi-file YAML management per script folder
 *   <li>Support for Strings, Integers, Longs, Doubles, Booleans, and String Lists
 *   <li>Thread-safe saves, reloads, and modifications
 * </ul>
 */
public class ScriptConfig {

  public static final String DEFAULT_CONFIG = "config.yml";

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private final File scriptFolder;
  private final Map<String, FileConfiguration> configCache;

  public ScriptConfig(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName = scriptName.replace(".java", "");
    this.configCache = new ConcurrentHashMap<>();

    if (this.scriptName.isEmpty()
        || this.scriptName.contains("..")
        || this.scriptName.contains("/")
        || this.scriptName.contains("\\")) {
      throw new IllegalArgumentException("Invalid script name: " + scriptName);
    }

    this.scriptFolder = new File(plugin.getDataFolder(), "script-data/" + this.scriptName);

    plugin.debug(
        "ScriptConfig initialized for: "
            + this.scriptName
            + " -> "
            + scriptFolder.getAbsolutePath());
  }

  // ==========================================
  // Core Configuration Management
  // ==========================================

  /**
   * Get or load a cached configuration file.
   *
   * @param fileName The YAML file name (e.g. "config.yml", "messages.yml")
   * @return The in-memory FileConfiguration
   */
  public FileConfiguration getConfig(String fileName) {
    String normalized = normalizeFileName(fileName);
    return configCache.computeIfAbsent(normalized, this::loadFromDisk);
  }

  /**
   * Get the default "config.yml" configuration.
   *
   * @return The default FileConfiguration
   */
  public FileConfiguration getConfig() {
    return getConfig(DEFAULT_CONFIG);
  }

  private synchronized FileConfiguration loadFromDisk(String normalizedFileName) {
    if (!scriptFolder.exists()) {
      plugin.debug("Creating folder for " + scriptName + ": " + scriptFolder.getAbsolutePath());
      scriptFolder.mkdirs();
    }

    File configFile = new File(scriptFolder, normalizedFileName);

    if (!configFile.exists()) {
      try {
        plugin.debug(
            "Creating config file: " + configFile.getAbsolutePath() + " for script: " + scriptName);
        configFile.createNewFile();
      } catch (IOException e) {
        plugin
            .getLogger()
            .log(Level.SEVERE, "Failed to create config file: " + normalizedFileName, e);
      }
    }

    return YamlConfiguration.loadConfiguration(configFile);
  }

  /**
   * Save a configuration to disk.
   *
   * @param fileName The YAML file name
   * @param config The configuration instance to persist
   */
  public synchronized void saveConfig(String fileName, FileConfiguration config) {
    String normalized = normalizeFileName(fileName);
    File configFile = new File(scriptFolder, normalized);

    try {
      if (!scriptFolder.exists()) {
        scriptFolder.mkdirs();
      }
      config.save(configFile);
      configCache.put(normalized, config);
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to save config file: " + normalized, e);
    }
  }

  /** Save the default "config.yml" configuration to disk. */
  public void saveConfig() {
    saveConfig(DEFAULT_CONFIG, getConfig(DEFAULT_CONFIG));
  }

  /** Shortcut to save the default configuration. */
  public void save() {
    saveConfig();
  }

  /**
   * Save a specific named configuration.
   *
   * @param fileName The YAML file name
   */
  public void saveConfig(String fileName) {
    saveConfig(fileName, getConfig(fileName));
  }

  /** Save all currently cached configurations to disk. */
  public synchronized void saveAll() {
    for (Map.Entry<String, FileConfiguration> entry : configCache.entrySet()) {
      saveConfig(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Reload a configuration file from disk into cache.
   *
   * @param fileName The YAML file name
   * @return The freshly reloaded FileConfiguration
   */
  public synchronized FileConfiguration reloadConfig(String fileName) {
    String normalized = normalizeFileName(fileName);
    configCache.remove(normalized);
    return getConfig(normalized);
  }

  /**
   * Reload the default "config.yml" configuration from disk.
   *
   * @return The freshly reloaded default FileConfiguration
   */
  public FileConfiguration reloadConfig() {
    return reloadConfig(DEFAULT_CONFIG);
  }

  /**
   * Shortcut to reload the default configuration.
   *
   * @return The freshly reloaded default FileConfiguration
   */
  public FileConfiguration reload() {
    return reloadConfig();
  }

  /** Reload all cached configuration files from disk. */
  public synchronized void reloadAll() {
    configCache.clear();
    for (String file : listConfigs()) {
      getConfig(file);
    }
  }

  /** Clear the in-memory cache without modifying disk files. */
  public void clearCache() {
    configCache.clear();
  }

  // ==========================================
  // String Getters
  // ==========================================

  public String getString(String path, String defaultValue) {
    return getString(DEFAULT_CONFIG, path, defaultValue);
  }

  public String getString(String path) {
    return getString(DEFAULT_CONFIG, path, null);
  }

  public String getString(String fileName, String path, String defaultValue) {
    return getConfig(fileName).getString(path, defaultValue);
  }

  // ==========================================
  // Integer Getters
  // ==========================================

  public int getInt(String path, int defaultValue) {
    return getInt(DEFAULT_CONFIG, path, defaultValue);
  }

  public int getInt(String path) {
    return getInt(DEFAULT_CONFIG, path, 0);
  }

  public int getInt(String fileName, String path, int defaultValue) {
    return getConfig(fileName).getInt(path, defaultValue);
  }

  public int getInt(String fileName, String path) {
    return getInt(fileName, path, 0);
  }

  // ==========================================
  // Long Getters
  // ==========================================

  public long getLong(String path, long defaultValue) {
    return getLong(DEFAULT_CONFIG, path, defaultValue);
  }

  public long getLong(String path) {
    return getLong(DEFAULT_CONFIG, path, 0L);
  }

  public long getLong(String fileName, String path, long defaultValue) {
    return getConfig(fileName).getLong(path, defaultValue);
  }

  public long getLong(String fileName, String path) {
    return getLong(fileName, path, 0L);
  }

  // ==========================================
  // Double Getters
  // ==========================================

  public double getDouble(String path, double defaultValue) {
    return getDouble(DEFAULT_CONFIG, path, defaultValue);
  }

  public double getDouble(String path) {
    return getDouble(DEFAULT_CONFIG, path, 0.0);
  }

  public double getDouble(String fileName, String path, double defaultValue) {
    return getConfig(fileName).getDouble(path, defaultValue);
  }

  public double getDouble(String fileName, String path) {
    return getDouble(fileName, path, 0.0);
  }

  // ==========================================
  // Boolean Getters
  // ==========================================

  public boolean getBoolean(String path, boolean defaultValue) {
    return getBoolean(DEFAULT_CONFIG, path, defaultValue);
  }

  public boolean getBoolean(String path) {
    return getBoolean(DEFAULT_CONFIG, path, false);
  }

  public boolean getBoolean(String fileName, String path, boolean defaultValue) {
    return getConfig(fileName).getBoolean(path, defaultValue);
  }

  public boolean getBoolean(String fileName, String path) {
    return getBoolean(fileName, path, false);
  }

  // ==========================================
  // List Getters
  // ==========================================

  public List<String> getStringList(String path) {
    return getStringList(DEFAULT_CONFIG, path);
  }

  public List<String> getStringList(String fileName, String path) {
    return getConfig(fileName).getStringList(path);
  }

  public List<?> getList(String path) {
    return getList(DEFAULT_CONFIG, path);
  }

  public List<?> getList(String fileName, String path) {
    return getConfig(fileName).getList(path);
  }

  // ==========================================
  // Path Existence Check
  // ==========================================

  public boolean contains(String path) {
    return contains(DEFAULT_CONFIG, path);
  }

  public boolean contains(String fileName, String path) {
    return getConfig(fileName).contains(path);
  }

  // ==========================================
  // Setters & Modifiers
  // ==========================================

  /** Set a value in default config.yml and save immediately. */
  public void set(String path, Object value) {
    set(DEFAULT_CONFIG, path, value);
  }

  /** Set a value in a named configuration and save immediately. */
  public void set(String fileName, String path, Object value) {
    FileConfiguration config = getConfig(fileName);
    config.set(path, value);
    saveConfig(fileName, config);
  }

  /** Set a value in in-memory cache without saving to disk immediately. */
  public void setMemory(String path, Object value) {
    setMemory(DEFAULT_CONFIG, path, value);
  }

  /** Set a value in in-memory cache for a named config without saving immediately. */
  public void setMemory(String fileName, String path, Object value) {
    getConfig(fileName).set(path, value);
  }

  /** Set a default value if not already present, copying defaults on save. */
  public void addDefault(String path, Object value) {
    addDefault(DEFAULT_CONFIG, path, value);
  }

  /** Set a default value in a named config if not present. */
  public void addDefault(String fileName, String path, Object value) {
    FileConfiguration config = getConfig(fileName);
    if (!config.contains(path)) {
      config.set(path, value);
      saveConfig(fileName, config);
    }
  }

  // ==========================================
  // File Utilities
  // ==========================================

  public File getDataFolder() {
    return scriptFolder;
  }

  public boolean configExists() {
    return configExists(DEFAULT_CONFIG);
  }

  public boolean configExists(String fileName) {
    String normalized = normalizeFileName(fileName);
    return new File(scriptFolder, normalized).exists();
  }

  public synchronized boolean deleteConfig(String fileName) {
    String normalized = normalizeFileName(fileName);
    configCache.remove(normalized);
    File configFile = new File(scriptFolder, normalized);
    return configFile.exists() && configFile.delete();
  }

  public List<String> listConfigs() {
    if (!scriptFolder.exists()) {
      return List.of();
    }
    File[] files =
        scriptFolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));

    if (files == null) {
      return List.of();
    }

    return List.of(files).stream().map(File::getName).toList();
  }

  private String normalizeFileName(String fileName) {
    if (fileName == null || fileName.trim().isEmpty()) {
      return DEFAULT_CONFIG;
    }
    String cleaned = fileName.trim().replace("\\", "/");
    if (cleaned.contains("/")) {
      cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1);
    }
    if (!cleaned.endsWith(".yml") && !cleaned.endsWith(".yaml")) {
      cleaned += ".yml";
    }
    return cleaned;
  }
}
