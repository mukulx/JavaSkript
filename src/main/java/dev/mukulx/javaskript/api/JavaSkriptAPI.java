package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptInstance;
import java.io.File;
import java.util.Map;

/** Public API for JavaSkript plugin Other plugins can use this to interact with JavaSkript */
public class JavaSkriptAPI {

  private final JavaSkriptPlugin plugin;
  private final ActionBarHelper actionBarHelper;
  private final TitleHelper titleHelper;
  private final BossBarHelper bossBarHelper;
  private final SoundHelper soundHelper;

  public JavaSkriptAPI(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.actionBarHelper = new ActionBarHelper(plugin);
    this.titleHelper = new TitleHelper();
    this.bossBarHelper = new BossBarHelper(plugin);
    this.soundHelper = new SoundHelper();
  }

  /**
   * Get the ActionBar helper for creating action bars
   *
   * @return ActionBarHelper instance
   */
  public ActionBarHelper getActionBarHelper() {
    return actionBarHelper;
  }

  /**
   * Get the Title helper for creating titles and subtitles
   *
   * @return TitleHelper instance
   */
  public TitleHelper getTitleHelper() {
    return titleHelper;
  }

  /**
   * Get the BossBar helper for creating boss bars
   *
   * @return BossBarHelper instance
   */
  public BossBarHelper getBossBarHelper() {
    return bossBarHelper;
  }

  /**
   * Get the Sound helper for playing sounds
   *
   * @return SoundHelper instance
   */
  public SoundHelper getSoundHelper() {
    return soundHelper;
  }

  /**
   * Load a script from file
   *
   * @param scriptFile The script file to load
   * @return true if successful, false otherwise
   */
  public boolean loadScript(File scriptFile) {
    if (scriptFile == null || !scriptFile.exists()) {
      return false;
    }
    return plugin.getScriptManager().loadScript(scriptFile);
  }

  /**
   * Unload a script by name
   *
   * @param scriptName The name of the script to unload
   * @return true if successful, false otherwise
   */
  public boolean unloadScript(String scriptName) {
    if (scriptName == null || scriptName.isEmpty()) {
      return false;
    }
    return plugin.getScriptManager().unloadScript(scriptName);
  }

  /**
   * Get a loaded script instance
   *
   * @param scriptName The name of the script
   * @return The script instance or null if not found
   */
  public ScriptInstance getScript(String scriptName) {
    if (scriptName == null || scriptName.isEmpty()) {
      return null;
    }
    return plugin.getScriptManager().getScript(scriptName);
  }

  /**
   * Get all loaded scripts
   *
   * @return Map of script names to instances
   */
  public Map<String, ScriptInstance> getAllScripts() {
    return plugin.getScriptManager().getLoadedScripts();
  }

  /** Reload all scripts */
  public void reloadAllScripts() {
    plugin.getScriptManager().reloadAllScripts();
  }

  /**
   * Get the scripts folder
   *
   * @return The scripts folder
   */
  public File getScriptsFolder() {
    return plugin.getScriptManager().getScriptsFolder();
  }

  /**
   * Check if a script is loaded
   *
   * @param scriptName The name of the script
   * @return true if loaded, false otherwise
   */
  public boolean isScriptLoaded(String scriptName) {
    if (scriptName == null || scriptName.isEmpty()) {
      return false;
    }
    return plugin.getScriptManager().getScript(scriptName) != null;
  }
}
