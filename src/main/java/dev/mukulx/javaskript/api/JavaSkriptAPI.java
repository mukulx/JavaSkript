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
  private final DialogHelper dialogHelper;
  private final PDCHelper pdcHelper;
  private final HologramHelper hologramHelper;

  public JavaSkriptAPI(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.actionBarHelper = new ActionBarHelper(plugin);
    this.titleHelper = new TitleHelper();
    this.bossBarHelper = new BossBarHelper(plugin);
    this.soundHelper = new SoundHelper();
    this.dialogHelper = new DialogHelper(plugin);
    this.pdcHelper = new PDCHelper(plugin);
    this.hologramHelper = new HologramHelper(plugin);
  }

  /**
   * Get a CommandHelper for creating and registering fluent commands.
   *
   * @return A CommandHelper instance
   */
  public dev.mukulx.javaskript.api.command.CommandHelper getCommandHelper() {
    return new dev.mukulx.javaskript.api.command.CommandHelper(plugin, "api");
  }

  /**
   * Get a CommandHelper for creating and registering fluent commands (alias).
   *
   * @return A CommandHelper instance
   */
  public dev.mukulx.javaskript.api.command.CommandHelper commands() {
    return getCommandHelper();
  }

  /**
   * Get an ItemHelper for creating and manipulating custom items.
   *
   * @return An ItemHelper instance
   */
  public dev.mukulx.javaskript.api.item.ItemHelper getItemHelper() {
    return new dev.mukulx.javaskript.api.item.ItemHelper(plugin);
  }

  /**
   * Get an ItemHelper for creating and manipulating custom items (alias).
   *
   * @return An ItemHelper instance
   */
  public dev.mukulx.javaskript.api.item.ItemHelper items() {
    return getItemHelper();
  }

  /**
   * Get a CooldownHelper for managing player and global cooldowns.
   *
   * @return A CooldownHelper instance
   */
  public CooldownHelper getCooldownHelper() {
    return new CooldownHelper(plugin, "api");
  }

  /**
   * Get a CooldownHelper for managing player and global cooldowns (alias).
   *
   * @return A CooldownHelper instance
   */
  public CooldownHelper cooldowns() {
    return getCooldownHelper();
  }

  /**
   * Get an EventHelper for registering functional lambda event listeners.
   *
   * @return An EventHelper instance
   */
  public dev.mukulx.javaskript.api.event.EventHelper getEventHelper() {
    return new dev.mukulx.javaskript.api.event.EventHelper(plugin, "api");
  }

  /**
   * Get an EventHelper for registering functional lambda event listeners (alias).
   *
   * @return An EventHelper instance
   */
  public dev.mukulx.javaskript.api.event.EventHelper events() {
    return getEventHelper();
  }

  /**
   * Get the universal PlayerHelper for messaging, titles, sounds, and stats.
   *
   * @return PlayerHelper instance
   */
  public dev.mukulx.javaskript.api.player.PlayerHelper getPlayerHelper() {
    return new dev.mukulx.javaskript.api.player.PlayerHelper(plugin);
  }

  /**
   * Get the universal PlayerHelper for messaging, titles, sounds, and stats (alias).
   *
   * @return PlayerHelper instance
   */
  public dev.mukulx.javaskript.api.player.PlayerHelper players() {
    return getPlayerHelper();
  }

  /**
   * Get the Hologram helper for creating modern Display Entity holograms
   *
   * @return HologramHelper instance
   */
  public HologramHelper getHologramHelper() {
    return hologramHelper;
  }

  /**
   * Get the PersistentData (PDC) helper for items, entities, and blocks
   *
   * @return PDCHelper instance
   */
  public PDCHelper getPDCHelper() {
    return pdcHelper;
  }

  /**
   * Get the PersistentData (PDC) helper alias
   *
   * @return PDCHelper instance
   */
  public PDCHelper getPdcHelper() {
    return pdcHelper;
  }

  /**
   * Get the Dialog helper for creating Paper dialogs
   *
   * @return DialogHelper instance
   */
  public DialogHelper getDialogHelper() {
    return dialogHelper;
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
