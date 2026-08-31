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
  private final dev.mukulx.javaskript.api.addon.AddonRegistry addonRegistry;

  public JavaSkriptAPI(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.actionBarHelper = new ActionBarHelper(plugin);
    this.titleHelper = new TitleHelper();
    this.bossBarHelper = new BossBarHelper(plugin);
    this.soundHelper = new SoundHelper();
    this.dialogHelper = new DialogHelper(plugin);
    this.pdcHelper = new PDCHelper(plugin);
    this.hologramHelper = new HologramHelper(plugin);
    this.addonRegistry = new dev.mukulx.javaskript.api.addon.AddonRegistry(plugin);
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
   * Get the ChatHelper for interactive clickable chat, confirmations, prompts, and pagers.
   *
   * @return ChatHelper instance
   */
  public dev.mukulx.javaskript.api.chat.ChatHelper getChatHelper() {
    return new dev.mukulx.javaskript.api.chat.ChatHelper(plugin, "api");
  }

  /**
   * Get the ChatHelper for interactive clickable chat, confirmations, prompts, and pagers (alias).
   *
   * @return ChatHelper instance
   */
  public dev.mukulx.javaskript.api.chat.ChatHelper chat() {
    return getChatHelper();
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

  /**
   * Get the EconomyHelper for managing balances, Vault, built-in economy, and custom currencies.
   *
   * @return The EconomyHelper instance
   */
  public dev.mukulx.javaskript.api.economy.EconomyHelper getEconomyHelper() {
    return plugin.getEconomyHelper();
  }

  /**
   * Get the EconomyHelper (alias).
   *
   * @return The EconomyHelper instance
   */
  public dev.mukulx.javaskript.api.economy.EconomyHelper economy() {
    return getEconomyHelper();
  }

  /**
   * Get the VariableHelper for managing shared in-memory and persistent script state.
   *
   * @return The VariableHelper instance
   */
  public dev.mukulx.javaskript.api.variable.VariableHelper getVariableHelper() {
    return plugin.getVariableHelper();
  }

  /**
   * Get the VariableHelper (alias).
   *
   * @return The VariableHelper instance
   */
  public dev.mukulx.javaskript.api.variable.VariableHelper variables() {
    return getVariableHelper();
  }

  /**
   * Get the inter-script Pub/Sub EventBus.
   *
   * @return The ScriptEventBus instance
   */
  public dev.mukulx.javaskript.api.event.ScriptEventBus getEventBus() {
    return plugin.getEventBus();
  }

  // ==========================================
  // Addon & Custom Injector Management
  // ==========================================

  /**
   * Get the AddonRegistry managing external addons and custom script field injectors.
   *
   * @return The AddonRegistry
   */
  public dev.mukulx.javaskript.api.addon.AddonRegistry getAddonRegistry() {
    return addonRegistry;
  }

  /**
   * Register an addon extending JavaSkript.
   *
   * @param addon The addon instance
   */
  public void registerAddon(dev.mukulx.javaskript.api.addon.JavaSkriptAddon addon) {
    addonRegistry.registerAddon(addon);
  }

  /**
   * Unregister an addon.
   *
   * @param addon The addon instance
   */
  public void unregisterAddon(dev.mukulx.javaskript.api.addon.JavaSkriptAddon addon) {
    addonRegistry.unregisterAddon(addon);
  }

  /**
   * Get all registered addons.
   *
   * @return Collection of registered addons
   */
  public java.util.Collection<dev.mukulx.javaskript.api.addon.JavaSkriptAddon> getAddons() {
    return addonRegistry.getAddons();
  }

  /**
   * Register a custom field injector matching a specific class type. When any script declares a
   * field of this type, the injector provides the instance automatically.
   *
   * @param type The class type to match
   * @param injector The provider lambda
   */
  public <T> void registerInjector(
      Class<T> type, dev.mukulx.javaskript.api.addon.FieldInjector<T> injector) {
    addonRegistry.registerInjector(type, injector);
  }

  /**
   * Register a custom field injector matching a field name (case-insensitive).
   *
   * @param fieldName The name of the field to match
   * @param injector The provider lambda
   */
  public void registerInjector(
      String fieldName, dev.mukulx.javaskript.api.addon.FieldInjector<?> injector) {
    addonRegistry.registerInjector(fieldName, injector);
  }

  /**
   * Register a universal fallback injector that inspects every field.
   *
   * @param injector The provider lambda
   */
  public void registerUniversalInjector(dev.mukulx.javaskript.api.addon.FieldInjector<?> injector) {
    addonRegistry.registerUniversalInjector(injector);
  }

  /**
   * Resolve a custom injection for a script field.
   *
   * @param instance The script instance
   * @param fieldType The declared field type
   * @param fieldName The declared field name
   * @return Injected object, or null if no injector matches
   */
  public Object resolveCustomInjection(
      ScriptInstance instance, Class<?> fieldType, String fieldName) {
    return addonRegistry.resolveInjection(instance, fieldType, fieldName);
  }

  // ==========================================
  // Inter-Plugin Script Invocation
  // ==========================================

  /**
   * Invoke a public method on a loaded script from another plugin.
   *
   * @param scriptName Name or path of the script (e.g. "pvp/CombatLog" or "Welcome")
   * @param methodName Name of the public method
   * @param args Arguments to pass
   * @return Return value of the method, or null
   * @throws NoSuchMethodException if no matching method is found
   * @throws Exception if invocation fails
   */
  public Object call(String scriptName, String methodName, Object... args) throws Exception {
    ScriptInstance instance = getScript(scriptName);
    if (instance == null || instance.getInstance() == null) {
      throw new IllegalArgumentException("Script not found or not loaded: " + scriptName);
    }
    Object target = instance.getInstance();
    Class<?> clazz = target.getClass();

    int expectedArgCount = (args != null ? args.length : 0);
    for (java.lang.reflect.Method m : clazz.getMethods()) {
      if (m.getName().equals(methodName) && m.getParameterCount() == expectedArgCount) {
        m.setAccessible(true);
        return m.invoke(target, args);
      }
    }
    throw new NoSuchMethodException(
        "Method '"
            + methodName
            + "' with "
            + expectedArgCount
            + " parameters not found in "
            + scriptName);
  }
}
