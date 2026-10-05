package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ScriptStorage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Resilient, crash-safe, comment-preserving configuration management for JavaSkript.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>O(1) in-memory cached reads with zero tick-lag
 *   <li>Native comment preservation for headers, keys, and inline comments
 *   <li>Crash-safe atomic saves (writes to temporary file before atomic rename, preventing 0-byte
 *       corrupt files)
 *   <li>Non-destructive defaults: adds missing options while strictly preserving player/admin
 *       modifications
 *   <li>Built-in schema migration engine for versioned updates without data loss
 *   <li>Batch update transactions for multi-key writes with a single disk flush
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
    this.scriptName = ScriptStorage.id(scriptName);
    this.configCache = new ConcurrentHashMap<>();

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

    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    config.options().parseComments(true);
    config.options().copyDefaults(true);
    return config;
  }

  /**
   * Save a configuration to disk atomically with zero risk of corruption.
   *
   * @param fileName The YAML file name
   * @param config The configuration instance to persist
   */
  public synchronized void saveConfig(String fileName, FileConfiguration config) {
    String normalized = normalizeFileName(fileName);
    File configFile = new File(scriptFolder, normalized);
    File tempFile = new File(scriptFolder, normalized + ".tmp");

    try {
      if (!scriptFolder.exists()) {
        scriptFolder.mkdirs();
      }

      // Write to temp file first to prevent partial 0-byte writes on server crashes
      config.save(tempFile);

      // Atomically replace target file
      try {
        Files.move(
            tempFile.toPath(),
            configFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        Files.move(tempFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }

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

  /** Save a specific named configuration. */
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

  public FileConfiguration reloadConfig() {
    return reloadConfig(DEFAULT_CONFIG);
  }

  public void reload() {
    reloadConfig();
  }

  // ==========================================
  // Header & Comment Preservation
  // ==========================================

  /** Set file header comment lines at the top of default config. */
  public void setHeader(String... headerLines) {
    setFileHeader(DEFAULT_CONFIG, headerLines);
  }

  /** Set file header comment lines at the top of a named config. */
  public void setFileHeader(String fileName, String... headerLines) {
    FileConfiguration config = getConfig(fileName);
    config.options().setHeader(List.of(headerLines));
    saveConfig(fileName, config);
  }

  /** Set file header comment lines for a named config using a list. */
  public void setHeader(String fileName, List<String> headerLines) {
    FileConfiguration config = getConfig(fileName);
    config.options().setHeader(headerLines);
    saveConfig(fileName, config);
  }

  /** Get top-of-file header comments. */
  public List<String> getHeader() {
    return getHeader(DEFAULT_CONFIG);
  }

  public List<String> getHeader(String fileName) {
    return getConfig(fileName).options().getHeader();
  }

  /** Set comments above a specific key in default config. */
  public void setComments(String path, String... comments) {
    setComments(DEFAULT_CONFIG, path, List.of(comments));
  }

  public void setComments(String path, List<String> comments) {
    setComments(DEFAULT_CONFIG, path, comments);
  }

  public void setComments(String fileName, String path, List<String> comments) {
    FileConfiguration config = getConfig(fileName);
    config.setComments(path, comments);
    saveConfig(fileName, config);
  }

  /** Get comments above a specific key. */
  public List<String> getComments(String path) {
    return getComments(DEFAULT_CONFIG, path);
  }

  public List<String> getComments(String fileName, String path) {
    return getConfig(fileName).getComments(path);
  }

  /** Set inline comment for a specific key (e.g. {@code key: value # inline comment}). */
  public void setInlineComments(String path, String... comments) {
    setInlineComments(DEFAULT_CONFIG, path, List.of(comments));
  }

  public void setInlineComments(String fileName, String path, List<String> comments) {
    FileConfiguration config = getConfig(fileName);
    config.setInlineComments(path, comments);
    saveConfig(fileName, config);
  }

  public List<String> getInlineComments(String path) {
    return getInlineComments(DEFAULT_CONFIG, path);
  }

  public List<String> getInlineComments(String fileName, String path) {
    return getConfig(fileName).getInlineComments(path);
  }

  // ==========================================
  // Non-Destructive Defaults & Migration
  // ==========================================

  /**
   * Set a default value if not already present, preserving comments and user edits. If the key
   * already exists, its existing user value is strictly preserved!
   *
   * @param path Key path
   * @param value Default value
   * @param comments Optional comment lines to place above the key
   */
  public void addDefault(String path, Object value, String... comments) {
    addFileDefault(DEFAULT_CONFIG, path, value, comments);
  }

  /**
   * Add a default key-value pair to a specific configuration file with optional comments.
   *
   * @param fileName The YAML configuration file name
   * @param path The YAML configuration path
   * @param value Default value
   * @param comments Optional comment lines to place above the key
   */
  public void addFileDefault(String fileName, String path, Object value, String... comments) {
    FileConfiguration config = getConfig(fileName);
    boolean modified = false;

    if (!config.contains(path)) {
      config.set(path, value);
      modified = true;
    }

    if (comments != null && comments.length > 0) {
      List<String> existing = config.getComments(path);
      if (existing == null || existing.isEmpty()) {
        config.setComments(path, List.of(comments));
        modified = true;
      }
    }

    if (modified) {
      saveConfig(fileName, config);
    }
  }

  /** Add default value for a named config file with comments list. */
  public void addDefault(String fileName, String path, Object value, List<String> comments) {
    addFileDefault(
        fileName, path, value, comments != null ? comments.toArray(new String[0]) : new String[0]);
  }

  /** Get current configuration schema version (stored in 'config-version', default 1). */
  public int getVersion() {
    return getVersion(DEFAULT_CONFIG);
  }

  public int getVersion(String fileName) {
    return getConfig(fileName).getInt("config-version", 1);
  }

  /**
   * Automatically migrate configuration to a newer target version with zero data loss.
   *
   * <p>If the user's config is below targetVersion, migrationAction is executed and
   * 'config-version' is automatically updated with comments and saved cleanly.
   *
   * @param targetVersion The desired version number
   * @param migrationAction Migration logic to transform old keys or inject new defaults
   */
  public void migrate(int targetVersion, Consumer<ScriptConfig> migrationAction) {
    migrate(DEFAULT_CONFIG, targetVersion, migrationAction);
  }

  public synchronized void migrate(
      String fileName, int targetVersion, Consumer<ScriptConfig> migrationAction) {
    int currentVersion = getVersion(fileName);
    if (currentVersion < targetVersion) {
      plugin
          .getLogger()
          .info(
              "["
                  + scriptName
                  + "] Migrating "
                  + fileName
                  + " from v"
                  + currentVersion
                  + " to v"
                  + targetVersion);
      try {
        if (migrationAction != null) {
          migrationAction.accept(this);
        }
        FileConfiguration config = getConfig(fileName);
        config.set("config-version", targetVersion);
        config.setComments(
            "config-version",
            List.of(
                "Configuration version - automatically managed by script migrations.",
                "Do not modify manually."));
        saveConfig(fileName, config);
        plugin
            .getLogger()
            .info(
                "[" + scriptName + "] Successfully migrated " + fileName + " to v" + targetVersion);
      } catch (Throwable t) {
        plugin
            .getLogger()
            .log(
                Level.SEVERE,
                "[" + scriptName + "] Failed to migrate " + fileName + ": " + t.getMessage(),
                t);
      }
    }
  }

  /**
   * Execute multiple configuration edits in memory and flush to disk in a single atomic save.
   *
   * @param batchAction The consumer performing set / addDefault actions
   */
  public synchronized void batch(Consumer<FileConfiguration> batchAction) {
    batch(DEFAULT_CONFIG, batchAction);
  }

  public synchronized void batch(String fileName, Consumer<FileConfiguration> batchAction) {
    FileConfiguration config = getConfig(fileName);
    if (batchAction != null) {
      batchAction.accept(config);
      saveConfig(fileName, config);
    }
  }

  // ==========================================
  // Typed Getters (O(1) in-memory cached)
  // ==========================================

  public Object get(String path) {
    return get(DEFAULT_CONFIG, path);
  }

  public Object get(String path, Object defaultValue) {
    return get(DEFAULT_CONFIG, path, defaultValue);
  }

  public Object get(String fileName, String path) {
    return getConfig(fileName).get(path);
  }

  public Object get(String fileName, String path, Object defaultValue) {
    return getConfig(fileName).get(path, defaultValue);
  }

  public String getString(String path, String defaultValue) {
    return getString(DEFAULT_CONFIG, path, defaultValue);
  }

  public String getString(String path) {
    return getString(DEFAULT_CONFIG, path, "");
  }

  public String getString(String fileName, String path, String defaultValue) {
    return getConfig(fileName).getString(path, defaultValue);
  }

  public String getFileString(String fileName, String path) {
    return getString(fileName, path, "");
  }

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
  // Sections & Keys
  // ==========================================

  public boolean contains(String path) {
    return contains(DEFAULT_CONFIG, path);
  }

  public boolean contains(String fileName, String path) {
    return getConfig(fileName).contains(path);
  }

  public ConfigurationSection getSection(String path) {
    return getSection(DEFAULT_CONFIG, path);
  }

  public ConfigurationSection getSection(String fileName, String path) {
    return getConfig(fileName).getConfigurationSection(path);
  }

  public Set<String> getKeys(boolean deep) {
    return getKeys(DEFAULT_CONFIG, deep);
  }

  public Set<String> getFileKeys(String fileName, boolean deep) {
    return getConfig(fileName).getKeys(deep);
  }

  public Set<String> getKeys(String path, boolean deep) {
    ConfigurationSection section = getConfig(DEFAULT_CONFIG).getConfigurationSection(path);
    return section != null ? section.getKeys(deep) : Collections.emptySet();
  }

  public Set<String> getKeys(String fileName, String path, boolean deep) {
    ConfigurationSection section = getConfig(fileName).getConfigurationSection(path);
    return section != null ? section.getKeys(deep) : Collections.emptySet();
  }

  public Map<String, Object> getValues(boolean deep) {
    return getValues(DEFAULT_CONFIG, deep);
  }

  public Map<String, Object> getFileValues(String fileName, boolean deep) {
    return getConfig(fileName).getValues(deep);
  }

  public Map<String, Object> getValues(String path, boolean deep) {
    ConfigurationSection section = getConfig(DEFAULT_CONFIG).getConfigurationSection(path);
    return section != null ? section.getValues(deep) : Collections.emptyMap();
  }

  public Map<String, Object> getValues(String fileName, String path, boolean deep) {
    ConfigurationSection section = getConfig(fileName).getConfigurationSection(path);
    return section != null ? section.getValues(deep) : Collections.emptyMap();
  }

  // ==========================================
  // Setters & Deletion
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

  public void setMemory(String fileName, String path, Object value) {
    getConfig(fileName).set(path, value);
  }

  /** Remove a key and save immediately. */
  public void remove(String path) {
    remove(DEFAULT_CONFIG, path);
  }

  public void remove(String fileName, String path) {
    FileConfiguration config = getConfig(fileName);
    if (config.contains(path)) {
      config.set(path, null);
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
