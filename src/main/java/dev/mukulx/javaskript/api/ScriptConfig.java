package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Easy configuration management for scripts Each script can have its own folder with multiple YAML
 * files
 */
public class ScriptConfig {

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private final File scriptFolder;

  public ScriptConfig(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName = scriptName.replace(".java", "");

    if (this.scriptName.isEmpty()
        || this.scriptName.contains("..")
        || this.scriptName.contains("/")
        || this.scriptName.contains("\\")) {
      throw new IllegalArgumentException("Invalid script name: " + scriptName);
    }

    this.scriptFolder = new File(plugin.getDataFolder(), "script-data/" + this.scriptName);

    plugin.debug("ScriptConfig created for: " + this.scriptName + " -> " + scriptFolder.getAbsolutePath());
  }

  /**
   * Get or create a config file
   *
   * @param fileName Name of the YAML file (e.g., "config.yml", "messages.yml")
   * @return FileConfiguration
   */
  public FileConfiguration getConfig(String fileName) {
    if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
      fileName += ".yml";
    }

    if (!scriptFolder.exists()) {
      plugin.debug("Creating folder for " + scriptName + ": " + scriptFolder.getAbsolutePath());
      scriptFolder.mkdirs();
    }

    File configFile = new File(scriptFolder, fileName);

    if (!configFile.exists()) {
      try {
        plugin.debug("Creating config file: " + configFile.getAbsolutePath() + " for script: " + scriptName);
        configFile.createNewFile();
      } catch (IOException e) {
        plugin.getLogger().log(Level.SEVERE, "Failed to create config file: " + fileName, e);
      }
    }

    return YamlConfiguration.loadConfiguration(configFile);
  }

  /**
   * Save a config file
   *
   * @param fileName Name of the YAML file
   * @param config The configuration to save
   */
  public void saveConfig(String fileName, FileConfiguration config) {
    if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
      fileName += ".yml";
    }

    File configFile = new File(scriptFolder, fileName);

    try {
      config.save(configFile);
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to save config file: " + fileName, e);
    }
  }

  /**
   * Reload a config file from disk
   *
   * @param fileName Name of the YAML file
   * @return Reloaded FileConfiguration
   */
  public FileConfiguration reloadConfig(String fileName) {
    return getConfig(fileName);
  }

  /**
   * Get the script's data folder
   *
   * @return The folder where script data is stored
   */
  public File getDataFolder() {
    return scriptFolder;
  }

  /**
   * Check if a config file exists
   *
   * @param fileName Name of the YAML file
   * @return true if exists
   */
  public boolean configExists(String fileName) {
    if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
      fileName += ".yml";
    }
    return new File(scriptFolder, fileName).exists();
  }

  /**
   * Delete a config file
   *
   * @param fileName Name of the YAML file
   * @return true if deleted successfully
   */
  public boolean deleteConfig(String fileName) {
    if (!fileName.endsWith(".yml") && !fileName.endsWith(".yaml")) {
      fileName += ".yml";
    }
    File configFile = new File(scriptFolder, fileName);
    return configFile.exists() && configFile.delete();
  }

  /**
   * Get all config files in the script folder
   *
   * @return List of config file names
   */
  public List<String> listConfigs() {
    File[] files =
        scriptFolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));

    if (files == null) {
      return List.of();
    }

    return List.of(files).stream().map(File::getName).toList();
  }

  /** Helper method to get a string with default value */
  public String getString(String fileName, String path, String defaultValue) {
    return getConfig(fileName).getString(path, defaultValue);
  }

  /** Helper method to get an int with default value */
  public int getInt(String fileName, String path, int defaultValue) {
    return getConfig(fileName).getInt(path, defaultValue);
  }

  /** Helper method to get a boolean with default value */
  public boolean getBoolean(String fileName, String path, boolean defaultValue) {
    return getConfig(fileName).getBoolean(path, defaultValue);
  }

  /** Helper method to set a value and save */
  public void set(String fileName, String path, Object value) {
    FileConfiguration config = getConfig(fileName);
    config.set(path, value);
    saveConfig(fileName, config);
  }
}
