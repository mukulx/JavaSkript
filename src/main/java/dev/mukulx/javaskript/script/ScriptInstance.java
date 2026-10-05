package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.*;
import dev.mukulx.javaskript.api.chat.ChatHelper;
import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.api.event.EventHelper;
import dev.mukulx.javaskript.api.item.ItemHelper;
import dev.mukulx.javaskript.api.player.PlayerHelper;
import dev.mukulx.javaskript.script.loader.ScriptClassLoader;
import dev.mukulx.javaskript.util.ServerUtil;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;

public class ScriptInstance {

  private final JavaSkriptPlugin plugin;
  private final File scriptFile;
  private final Class<?> scriptClass;
  private final ScriptClassLoader classLoader;
  private Object instance;
  private final ScriptListeners listeners;
  private final ScriptContext context;

  // API instances for this script
  private ScriptScheduler scheduler;
  private ScriptConfig config;
  private DatabaseHelper database;
  private PlaceholderHelper placeholders;
  private RecipeHelper recipes;
  private ActionBarHelper actionBars;
  private BossBarHelper bossBars;
  private DialogHelper dialog;
  private PDCHelper pdc;
  private HologramHelper holograms;
  private dev.mukulx.javaskript.api.MannequinHelper mannequins;
  private dev.mukulx.javaskript.api.AdvancementHelper advancements;
  private CommandHelper commands;
  private ItemHelper items;
  private CooldownHelper cooldowns;
  private EventHelper events;
  private PlayerHelper players;
  private ChatHelper chat;
  private dev.mukulx.javaskript.api.economy.EconomyHelper economy;
  private dev.mukulx.javaskript.api.variable.VariableHelper variables;
  private dev.mukulx.javaskript.api.variable.ScriptVariables scriptVariables;
  private dev.mukulx.javaskript.api.http.HttpHelper http;
  private dev.mukulx.javaskript.api.team.TeamHelper teams;

  public ScriptInstance(
      JavaSkriptPlugin plugin,
      File scriptFile,
      Class<?> scriptClass,
      ScriptClassLoader classLoader) {
    this.plugin = plugin;
    this.scriptFile = scriptFile;
    this.scriptClass = scriptClass;
    this.classLoader = classLoader;
    this.listeners = new ScriptListeners(plugin, scriptFile, scriptClass, classLoader);
    this.context = new ScriptContext(plugin, scriptFile.getName());
  }

  public boolean initialize() {
    boolean initialized = false;
    try {
      initialized = doInitialize();
      return initialized;
    } finally {
      if (!initialized) {
        // onEnable may already have scheduled tasks or registered events, commands and the like
        releaseResources();
      }
    }
  }

  private boolean doInitialize() {
    try {
      // Check Folia compatibility
      if (!checkFoliaCompatibility()) {
        return false;
      }

      // Create instance with null-safe constructor handling
      Constructor<?> constructor = scriptClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      this.instance = constructor.newInstance();

      if (instance == null) {
        plugin.getLogger().severe("Failed to create instance for: " + scriptFile.getName());
        return false;
      }

      // Initialize API helpers
      // Use class name instead of file name to avoid issues with manual config creation
      String scriptKey = plugin.getScriptManager().getScriptKey(scriptFile);

      // Everything registered below is released by context.close() on unload or failed init.
      // Listeners and commands are owned first because onEnable may register them.
      context.own("event listeners", () -> listeners.unregisterListeners(instance));
      context.own("commands", listeners::unregisterCommands);

      this.scheduler = new ScriptScheduler(plugin, scriptKey);
      context.own("scheduled tasks", scheduler::close);
      this.config = new ScriptConfig(plugin, scriptKey);
      this.database = new DatabaseHelper(plugin, scriptKey);
      context.own("database", database::disconnect);
      this.placeholders = new PlaceholderHelper(plugin, scriptKey);
      context.own("placeholders", placeholders::unregisterAll);
      this.recipes = new RecipeHelper(plugin, scriptKey);
      context.own("recipes", recipes::removeAll);
      // Per script, so persistent action bars and boss bars end with the script that showed them
      this.actionBars = new ActionBarHelper(plugin);
      context.own("action bars", actionBars::shutdown);
      this.bossBars = new BossBarHelper(plugin);
      context.own("boss bars", bossBars::hideAll);
      this.dialog = plugin.getAPI().getDialogHelper();
      this.pdc = plugin.getAPI().getPDCHelper();
      this.holograms = new HologramHelper(plugin);
      context.own("holograms", holograms::removeAll);
      this.mannequins = new dev.mukulx.javaskript.api.MannequinHelper(plugin, scriptKey);
      context.own("mannequins", mannequins::removeAll);
      this.advancements = new dev.mukulx.javaskript.api.AdvancementHelper(plugin, scriptKey);
      context.own("advancements", advancements::removeAll);
      this.commands = new CommandHelper(plugin, scriptKey);
      context.own("fluent commands", commands::unregisterAll);
      this.items = new ItemHelper(plugin);
      this.cooldowns = new CooldownHelper(plugin, scriptKey);
      context.own("cooldowns", cooldowns::cleanup);
      this.events = new EventHelper(plugin, scriptKey);
      context.own("event subscriptions", events::unregisterAll);
      this.players = new PlayerHelper(plugin);
      this.chat = new ChatHelper(plugin, scriptKey);
      context.own("chat prompts", chat::cleanup);
      this.economy = plugin.getEconomyHelper();
      this.variables = plugin.getVariableHelper();
      this.scriptVariables =
          new dev.mukulx.javaskript.api.variable.ScriptVariables(variables, scriptKey);
      this.http = plugin.getHttpHelper();
      this.teams = plugin.getTeamHelper();

      // Inject API helpers into script instance
      plugin.debug("Injecting APIs into script: " + scriptKey);
      ScriptInjector.inject(plugin, this, instance);

      // Call onEnable method if it exists across class hierarchy
      Method onEnableMethod = findLifecycleMethod(scriptClass, "onEnable");
      if (onEnableMethod != null) {
        try {
          onEnableMethod.invoke(instance);
          plugin.debug("Called onEnable for: " + scriptFile.getName());
        } catch (Exception e) {
          dev.mukulx.javaskript.util.ScriptErrorFormatter.log(plugin, scriptKey, "onEnable()", e);
        }
      }

      // Register as event listener if applicable with profiling wrapper
      if (instance instanceof Listener listener) {
        listeners.registerEvents(listener);
      }

      // Register as command executor if applicable
      if (instance instanceof CommandExecutor) {
        listeners.registerCommand(instance);
      }

      return true;

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();

      // Check for common mistakes and provide helpful messages
      if (cause instanceof NullPointerException) {
        String message = cause.getMessage();
        if (message != null
            && (message.contains("scheduler")
                || message.contains("config")
                || message.contains("database")
                || message.contains("placeholders"))) {
          plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
          plugin.getLogger().severe("Script failed to load: " + scriptFile.getName());
          plugin
              .getLogger()
              .severe("ERROR: Trying to use API in constructor before it's injected!");
          plugin.getLogger().severe("");
          plugin
              .getLogger()
              .severe("APIs (scheduler, config, database, placeholders) are injected");
          plugin.getLogger().severe("AFTER the constructor runs. Don't use them in constructors!");
          plugin.getLogger().severe("");
          plugin
              .getLogger()
              .severe("Solution: Use APIs in event handlers or methods, not constructors.");
          plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
          plugin.getLogger().severe("Full error: " + cause.toString());
          return false;
        }
      } else if (cause instanceof UnsupportedOperationException) {
        String message = cause.getMessage();
        if (ServerUtil.isFolia()) {
          plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
          plugin.getLogger().severe("Script failed to load on Folia: " + scriptFile.getName());
          plugin.getLogger().severe("ERROR: Using Paper-only scheduler API on Folia!");
          plugin.getLogger().severe("");
          plugin
              .getLogger()
              .severe("You're using Bukkit.getScheduler() which doesn't work on Folia.");
          plugin.getLogger().severe("Use JavaSkript's ScriptScheduler instead (auto-injected).");
          plugin.getLogger().severe("");
          plugin.getLogger().severe("Also, don't call scheduler methods in constructors!");
          plugin.getLogger().severe("Use them in event handlers or methods instead.");
          plugin.getLogger().severe("");
          plugin.getLogger().severe("Mark this script with @PaperOnly if it can't support Folia.");
          plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
          plugin.getLogger().severe("Full error: " + cause.toString());
          return false;
        }
      }

      // Generic error with helpful context
      plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      plugin.getLogger().severe("Script failed to initialize: " + scriptFile.getName());
      plugin.getLogger().severe("Error in constructor: " + cause.getClass().getSimpleName());
      plugin.getLogger().severe("");
      plugin.getLogger().severe("Common causes:");
      plugin.getLogger().severe("  1. Using APIs (scheduler, config, database) in constructor");
      plugin.getLogger().severe("  2. Using Bukkit.getScheduler() on Folia (use ScriptScheduler)");
      plugin.getLogger().severe("  3. Null pointer - check all variables are initialized");
      plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      plugin.getLogger().log(Level.SEVERE, "Full error:", e);
      return false;

    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "Failed to initialize script: " + scriptFile.getName(), e);
      return false;
    }
  }

  private boolean checkFoliaCompatibility() {
    // Check if running on Folia
    if (!ServerUtil.isFolia()) {
      return true; // Not Folia, no compatibility issues
    }

    // Check for @PaperOnly annotation
    PaperOnly paperOnly = scriptClass.getAnnotation(PaperOnly.class);
    if (paperOnly != null) {
      plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      plugin.getLogger().severe("Script cannot load on Folia: " + scriptFile.getName());
      plugin.getLogger().severe("This script is marked as Paper-only.");
      plugin.getLogger().severe("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      return false;
    }

    // Check for @FoliaSupport annotation
    FoliaSupport foliaSupport = scriptClass.getAnnotation(FoliaSupport.class);
    if (foliaSupport == null || !foliaSupport.value()) {
      plugin.getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      plugin.getLogger().warning("Script may not work on Folia: " + scriptFile.getName());
      plugin.getLogger().warning("Not marked as Folia-compatible.");
      plugin.getLogger().warning("Add @FoliaSupport if this script supports Folia.");
      plugin.getLogger().warning("Add @PaperOnly if this script only works on Paper.");
      plugin.getLogger().warning("Loading anyway, but expect potential issues...");
      plugin.getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    } else {
      plugin.getLogger().info("Script is Folia-compatible: " + scriptFile.getName());
    }

    return true;
  }

  private Method findLifecycleMethod(Class<?> clazz, String methodName) {
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      try {
        Method method = current.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method;
      } catch (NoSuchMethodException e) {
        current = current.getSuperclass();
      }
    }
    return null;
  }

  public void unload() {
    String scriptName = scriptFile.getName();
    plugin.debug("Starting unload of: " + scriptName);

    // Call onDisable method if it exists across class hierarchy
    Method onDisableMethod = findLifecycleMethod(scriptClass, "onDisable");
    if (onDisableMethod != null) {
      try {
        onDisableMethod.invoke(instance);
        plugin.debug("Called onDisable for: " + scriptName);
      } catch (Exception e) {
        dev.mukulx.javaskript.util.ScriptErrorFormatter.log(plugin, scriptName, "onDisable()", e);
      }
    }

    releaseResources();
  }

  /**
   * Release everything the script registered with the server. Runs on unload and when
   * initialization fails part way through.
   */
  private void releaseResources() {
    String scriptName = scriptFile.getName();

    context.close();

    // Drop references so use after unload fails fast instead of touching released helpers
    scheduler = null;
    database = null;
    placeholders = null;
    recipes = null;
    actionBars = null;
    bossBars = null;
    holograms = null;
    mannequins = null;
    advancements = null;
    events = null;
    chat = null;
    config = null;

    // Automatic cleanup of common custom resources
    // This helps scripts that don't have onDisable() but use custom resources
    try {
      ScriptResourceCleanup.run(plugin, scriptFile, scriptClass, instance);
    } catch (Exception e) {
      plugin.getLogger().warning("Error during automatic cleanup (continuing): " + e.getMessage());
    }

    // Close and unload classloader resources
    try {
      if (classLoader != null) {
        classLoader.unloadAll();
      }
    } catch (Exception e) {
      // Ignore
    }

    // Clear instance
    try {
      instance = null;
    } catch (Exception e) {
      // Ignore
    }

    plugin.debug("Forced unload completed for: " + scriptName);
  }

  /**
   * Register a cleanup to run when this script unloads. Addons whose custom field injectors hand
   * scripts a resource that must not outlive them can release it here.
   */
  public void registerCleanup(String description, Runnable cleanup) {
    context.own(description, cleanup);
  }

  public File getScriptFile() {
    return scriptFile;
  }

  public Class<?> getScriptClass() {
    return scriptClass;
  }

  public Object getInstance() {
    return instance;
  }

  public String getName() {
    return scriptFile.getName();
  }

  public ScriptScheduler getScheduler() {
    return scheduler;
  }

  public ScriptConfig getConfig() {
    return config;
  }

  public DatabaseHelper getDatabase() {
    return database;
  }

  public PlaceholderHelper getPlaceholders() {
    return placeholders;
  }

  public DialogHelper getDialog() {
    return dialog;
  }

  public PDCHelper getPDC() {
    return pdc;
  }

  public PDCHelper getPdc() {
    return pdc;
  }

  public HologramHelper getHolograms() {
    return holograms;
  }

  public HologramHelper getHologramHelper() {
    return holograms;
  }

  public dev.mukulx.javaskript.api.MannequinHelper getMannequins() {
    return mannequins;
  }

  public dev.mukulx.javaskript.api.MannequinHelper getMannequinHelper() {
    return mannequins;
  }

  public dev.mukulx.javaskript.api.AdvancementHelper getAdvancements() {
    return advancements;
  }

  public dev.mukulx.javaskript.api.AdvancementHelper getAdvancementHelper() {
    return advancements;
  }

  public CommandHelper getCommands() {
    return commands;
  }

  public CommandHelper getCommandHelper() {
    return commands;
  }

  public ItemHelper getItems() {
    return items;
  }

  public ItemHelper getItemHelper() {
    return items;
  }

  public CooldownHelper getCooldowns() {
    return cooldowns;
  }

  public CooldownHelper getCooldownHelper() {
    return cooldowns;
  }

  public EventHelper getEvents() {
    return events;
  }

  public EventHelper getEventHelper() {
    return events;
  }

  public PlayerHelper getPlayers() {
    return players;
  }

  public PlayerHelper getPlayerHelper() {
    return players;
  }

  public ChatHelper getChat() {
    return chat;
  }

  public ChatHelper getChatHelper() {
    return chat;
  }

  public dev.mukulx.javaskript.api.economy.EconomyHelper getEconomy() {
    return economy;
  }

  public dev.mukulx.javaskript.api.economy.EconomyHelper getEconomyHelper() {
    return economy;
  }

  public dev.mukulx.javaskript.api.variable.VariableHelper getVariables() {
    return variables;
  }

  public dev.mukulx.javaskript.api.variable.VariableHelper getVariableHelper() {
    return variables;
  }

  public dev.mukulx.javaskript.api.http.HttpHelper getHttp() {
    return http;
  }

  public dev.mukulx.javaskript.api.http.HttpHelper getHttpHelper() {
    return http;
  }

  public dev.mukulx.javaskript.api.team.TeamHelper getTeams() {
    return teams;
  }

  public dev.mukulx.javaskript.api.team.TeamHelper getTeamHelper() {
    return teams;
  }

  public boolean isFoliaCompatible() {
    // Check if script has @PaperOnly annotation
    if (scriptClass.getAnnotation(PaperOnly.class) != null) {
      return false;
    }

    // Check if script has @FoliaSupport annotation
    FoliaSupport foliaSupport = scriptClass.getAnnotation(FoliaSupport.class);
    return foliaSupport != null && foliaSupport.value();
  }

  public boolean isPaperOnly() {
    return scriptClass.getAnnotation(PaperOnly.class) != null;
  }

  public String getFoliaCompatibilityStatus() {
    if (isPaperOnly()) {
      return "Paper-only";
    } else if (isFoliaCompatible()) {
      return "Folia-compatible";
    } else {
      return "Unknown (not marked)";
    }
  }

  public ScriptClassLoader getClassLoader() {
    return classLoader;
  }

  RecipeHelper recipes() {
    return recipes;
  }

  ActionBarHelper actionBars() {
    return actionBars;
  }

  BossBarHelper bossBars() {
    return bossBars;
  }

  dev.mukulx.javaskript.api.variable.ScriptVariables scriptVariables() {
    return scriptVariables;
  }
}
