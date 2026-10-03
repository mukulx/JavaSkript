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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.RegisteredListener;

public class ScriptInstance {

  private final JavaSkriptPlugin plugin;
  private final File scriptFile;
  private final Class<?> scriptClass;
  private final ScriptClassLoader classLoader;
  private Object instance;
  private final Map<String, Command> registeredCommands;
  private final ScriptContext context;

  // API instances for this script
  private ScriptScheduler scheduler;
  private ScriptConfig config;
  private DatabaseHelper database;
  private PlaceholderHelper placeholders;
  private RecipeHelper recipes;
  private DialogHelper dialog;
  private PDCHelper pdc;
  private HologramHelper holograms;
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
    this.registeredCommands = new LinkedHashMap<>();
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
      context.own("event listeners", this::unregisterListeners);
      context.own("commands", this::unregisterRegisteredCommands);

      this.scheduler = new ScriptScheduler(plugin, scriptKey);
      context.own("scheduled tasks", scheduler::cancelAll);
      this.config = new ScriptConfig(plugin, scriptKey);
      this.database = new DatabaseHelper(plugin, scriptKey);
      context.own("database", database::disconnect);
      this.placeholders = new PlaceholderHelper(plugin, scriptKey);
      context.own("placeholders", placeholders::unregisterAll);
      this.recipes = new RecipeHelper(plugin, scriptKey);
      context.own("recipes", recipes::removeAll);
      this.dialog = plugin.getAPI().getDialogHelper();
      this.pdc = plugin.getAPI().getPDCHelper();
      this.holograms = new HologramHelper(plugin);
      context.own("holograms", holograms::removeAll);
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
      injectAPIs();

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
        registerProfiledEvents(listener);
      }

      // Register as command executor if applicable
      if (instance instanceof CommandExecutor) {
        registerCommand();
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

  private void injectAPIs() {
    try {
      // Collect and inject into all fields across the entire class hierarchy
      Class<?> current = scriptClass;
      while (current != null && current != Object.class) {
        for (Field field : current.getDeclaredFields()) {
          try {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                || java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
              continue;
            }
            field.setAccessible(true);

            // Skip if already initialized with non-null value
            if (field.get(instance) != null) {
              continue;
            }

            Class<?> type = field.getType();
            String name = field.getName().toLowerCase();

            // 1. Match by Type
            if (JavaSkriptPlugin.class.isAssignableFrom(type)) {
              field.set(instance, plugin);
            } else if (JavaSkriptAPI.class.isAssignableFrom(type)) {
              field.set(instance, plugin.getAPI());
            } else if (ScriptScheduler.class.isAssignableFrom(type)) {
              field.set(instance, scheduler);
            } else if (ScriptConfig.class.isAssignableFrom(type)) {
              field.set(instance, config);
            } else if (DatabaseHelper.class.isAssignableFrom(type)) {
              field.set(instance, database);
            } else if (PlaceholderHelper.class.isAssignableFrom(type)) {
              field.set(instance, placeholders);
            } else if (RecipeHelper.class.isAssignableFrom(type)) {
              field.set(instance, recipes);
            } else if (ActionBarHelper.class.isAssignableFrom(type)) {
              field.set(instance, plugin.getAPI().getActionBarHelper());
            } else if (TitleHelper.class.isAssignableFrom(type)) {
              field.set(instance, plugin.getAPI().getTitleHelper());
            } else if (BossBarHelper.class.isAssignableFrom(type)) {
              field.set(instance, plugin.getAPI().getBossBarHelper());
            } else if (SoundHelper.class.isAssignableFrom(type)) {
              field.set(instance, plugin.getAPI().getSoundHelper());
            } else if (DialogHelper.class.isAssignableFrom(type)) {
              field.set(instance, dialog);
            } else if (PDCHelper.class.isAssignableFrom(type)) {
              field.set(instance, pdc);
            } else if (HologramHelper.class.isAssignableFrom(type)) {
              field.set(instance, holograms);
            } else if (CommandHelper.class.isAssignableFrom(type)) {
              field.set(instance, commands);
            } else if (ItemHelper.class.isAssignableFrom(type)) {
              field.set(instance, items);
            } else if (CooldownHelper.class.isAssignableFrom(type)) {
              field.set(instance, cooldowns);
            } else if (EventHelper.class.isAssignableFrom(type)) {
              field.set(instance, events);
            } else if (PlayerHelper.class.isAssignableFrom(type)) {
              field.set(instance, players);
            } else if (ChatHelper.class.isAssignableFrom(type)) {
              field.set(instance, chat);
            } else if (dev.mukulx.javaskript.api.economy.EconomyHelper.class.isAssignableFrom(type)
                || dev.mukulx.javaskript.api.economy.EconomyProvider.class.isAssignableFrom(type)) {
              field.set(instance, economy);
            } else if (dev.mukulx.javaskript.api.variable.ScriptVariables.class.isAssignableFrom(
                type)) {
              field.set(instance, scriptVariables);
            } else if (dev.mukulx.javaskript.api.variable.VariableHelper.class.isAssignableFrom(
                type)) {
              field.set(instance, variables);
            } else if (dev.mukulx.javaskript.api.http.HttpHelper.class.isAssignableFrom(type)) {
              field.set(instance, http);
            } else if (dev.mukulx.javaskript.api.team.TeamHelper.class.isAssignableFrom(type)) {
              field.set(instance, teams);
            } else if (dev.mukulx.javaskript.api.message.MessageManager.class.isAssignableFrom(
                type)) {
              field.set(instance, plugin.getMessageManager());
            }
            // 2. Match by Name / Alias
            else if (name.equals("plugin") || name.equals("javaskript")) {
              field.set(instance, plugin);
            } else if (name.equals("api")) {
              field.set(instance, plugin.getAPI());
            } else if (name.equals("scheduler") || name.equals("tasks")) {
              field.set(instance, scheduler);
            } else if (name.equals("config") || name.equals("cfg")) {
              field.set(instance, config);
            } else if (name.equals("database") || name.equals("db")) {
              field.set(instance, database);
            } else if (name.equals("placeholders") || name.equals("papi")) {
              field.set(instance, placeholders);
            } else if (name.equals("recipes")) {
              field.set(instance, recipes);
            } else if (name.equals("actionbar")
                || name.equals("actionbars")
                || name.equals("actionbarhelper")) {
              field.set(instance, plugin.getAPI().getActionBarHelper());
            } else if (name.equals("title")
                || name.equals("titles")
                || name.equals("titlehelper")) {
              field.set(instance, plugin.getAPI().getTitleHelper());
            } else if (name.equals("bossbar")
                || name.equals("bossbars")
                || name.equals("bossbarhelper")) {
              field.set(instance, plugin.getAPI().getBossBarHelper());
            } else if (name.equals("sound")
                || name.equals("sounds")
                || name.equals("soundhelper")) {
              field.set(instance, plugin.getAPI().getSoundHelper());
            } else if (name.equals("dialog")
                || name.equals("dialogs")
                || name.equals("dialoghelper")) {
              field.set(instance, dialog);
            } else if (name.equals("pdc")
                || name.equals("pdchelper")
                || name.equals("persistentdata")
                || name.equals("nbt")) {
              field.set(instance, pdc);
            } else if (name.equals("hologram")
                || name.equals("holograms")
                || name.equals("holo")
                || name.equals("displays")) {
              field.set(instance, holograms);
            } else if (name.equals("commands")
                || name.equals("commandhelper")
                || name.equals("commandapi")
                || name.equals("cmd")) {
              field.set(instance, commands);
            } else if (name.equals("items")
                || name.equals("itemhelper")
                || name.equals("itembuilder")) {
              field.set(instance, items);
            } else if (name.equals("cooldowns")
                || name.equals("cooldown")
                || name.equals("cooldownhelper")) {
              field.set(instance, cooldowns);
            } else if (name.equals("events")
                || name.equals("eventhelper")
                || name.equals("eventapi")) {
              field.set(instance, events);
            } else if (name.equals("players")
                || name.equals("playerhelper")
                || name.equals("playerutil")) {
              field.set(instance, players);
            } else if (name.equals("chat") || name.equals("chathelper") || name.equals("chatapi")) {
              field.set(instance, chat);
            } else if (name.equals("economy")
                || name.equals("eco")
                || name.equals("economyhelper")
                || name.equals("vault")) {
              field.set(instance, economy);
            } else if (name.equals("scriptvariables") || name.equals("localvariables")) {
              field.set(instance, scriptVariables);
            } else if (name.equals("variables")
                || name.equals("vars")
                || name.equals("variablehelper")
                || name.equals("shared")
                || name.equals("state")) {
              field.set(instance, variables);
            } else if (name.equals("http") || name.equals("web") || name.equals("httphelper")) {
              field.set(instance, http);
            } else if (name.equals("teams")
                || name.equals("team")
                || name.equals("teamhelper")
                || name.equals("clan")
                || name.equals("party")) {
              field.set(instance, teams);
            } else if (name.equals("messages")
                || name.equals("messagemanager")
                || name.equals("messagehelper")) {
              field.set(instance, plugin.getMessageManager());
            } else {
              // Check external addons and plugins for custom registered field injectors
              Object custom = plugin.getAPI().resolveCustomInjection(this, type, name);
              if (custom != null) {
                field.set(instance, custom);
              }
            }
          } catch (Exception e) {
            plugin.debug("Could not inject into field " + field.getName() + ": " + e.getMessage());
          }
        }
        current = current.getSuperclass();
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.WARNING, "Error during API injection for " + scriptFile.getName(), e);
    }
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

  private void registerProfiledEvents(Listener listener) {
    String scriptKey = plugin.getScriptManager().getScriptKey(scriptFile);
    int registeredCount = 0;

    Set<Method> methods = new HashSet<>();
    for (Class<?> clazz = listener.getClass();
        clazz != null && clazz != Object.class;
        clazz = clazz.getSuperclass()) {
      for (Method m : clazz.getDeclaredMethods()) {
        methods.add(m);
      }
    }

    for (Method method : methods) {
      EventHandler handler = method.getAnnotation(EventHandler.class);
      if (handler == null) continue;
      if (method.getParameterCount() != 1) continue;
      Class<?> paramType = method.getParameterTypes()[0];
      if (!Event.class.isAssignableFrom(paramType)) continue;

      Class<? extends Event> eventClass = paramType.asSubclass(Event.class);
      method.setAccessible(true);

      EventExecutor executor =
          (l, event) -> {
            if (!eventClass.isInstance(event)) return;
            long t0 = System.nanoTime();
            try {
              method.invoke(l, event);
            } catch (InvocationTargetException ite) {
              Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
              dev.mukulx.javaskript.util.ScriptErrorFormatter.log(
                  plugin, scriptKey, "event handler " + method.getName(), cause);
            } catch (Exception ex) {
              dev.mukulx.javaskript.util.ScriptErrorFormatter.log(
                  plugin, scriptKey, "event dispatching " + eventClass.getSimpleName(), ex);
            } finally {
              long elapsed = System.nanoTime() - t0;
              plugin.getProfiler().record(scriptKey, "EVENT", eventClass.getSimpleName(), elapsed);
            }
          };

      Bukkit.getPluginManager()
          .registerEvent(
              eventClass,
              listener,
              handler.priority(),
              executor,
              plugin,
              handler.ignoreCancelled());
      registeredCount++;
    }

    plugin.debug(
        "Registered "
            + registeredCount
            + " profiled event handlers for: "
            + scriptClass.getSimpleName());
  }

  private void registerCommand() {
    try {
      // Extract command name from class name (e.g., HealCommand -> heal)
      String className = scriptClass.getSimpleName();
      String commandName = className.toLowerCase().replace("command", "").replace("cmd", "");

      if (commandName.isEmpty()) {
        commandName = className.toLowerCase();
      }

      String scriptKey = plugin.getScriptManager().getScriptKey(scriptFile);
      CommandExecutor originalExecutor = (CommandExecutor) instance;

      // Profiled command execution
      CommandExecutor profiledExecutor =
          (sender, cmd, label, args) -> {
            long t0 = System.nanoTime();
            try {
              return originalExecutor.onCommand(sender, cmd, label, args);
            } finally {
              long elapsed = System.nanoTime() - t0;
              plugin.getProfiler().record(scriptKey, "COMMAND", "/" + label, elapsed);
            }
          };

      // Use dynamic command registration. Pass the script's own TabCompleter explicitly: the
      // profiling lambda above is not one, so the registry cannot discover it by itself.
      TabCompleter tabCompleter = instance instanceof TabCompleter tc ? tc : null;
      boolean registered =
          plugin
              .getCommandRegistry()
              .registerCommand(
                  commandName, profiledExecutor, tabCompleter, null, null, null, null, null);

      if (registered) {
        registeredCommands.put(commandName, plugin.getCommandRegistry().getCommand(commandName));
      } else {
        plugin.getLogger().warning("Failed to register command: /" + commandName);
      }

    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.WARNING, "Failed to register command for: " + scriptClass.getSimpleName(), e);
    }
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
    holograms = null;
    events = null;
    chat = null;
    config = null;

    // Automatic cleanup of common custom resources
    // This helps scripts that don't have onDisable() but use custom resources
    try {
      cleanupCustomResources();
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
   * Automatically clean up common custom resources by scanning instance fields This helps scripts
   * that don't implement onDisable() but use resources like HikariCP, ExecutorService, etc.
   */
  private void cleanupCustomResources() {
    if (instance == null) {
      return;
    }

    String scriptName = scriptFile.getName();
    int cleanedCount = 0;

    try {
      // Walk the class hierarchy (matches injectAPIs) so inherited resources also clean up.
      Class<?> current = scriptClass;
      while (current != null && current != Object.class) {
        for (Field field : current.getDeclaredFields()) {
          try {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
              continue;
            }
            field.setAccessible(true);
            Object value = field.get(instance);

            if (value == null) {
              continue;
            }

            // Skip framework-owned helpers: they are cleaned explicitly above, and some
            // (economy, variables, http, teams) are shared across scripts — closing them
            // here would break every other script.
            Class<?> valueType = value.getClass();
            String typeName = valueType.getName();
            if (typeName.startsWith("dev.mukulx.javaskript.")
                || valueType == JavaSkriptPlugin.class) {
              continue;
            }

            // The shared HttpClient from the HTTP helper is AutoCloseable on Java 21. Closing it
            // here would break HTTP calls in every other script.
            if (value instanceof java.net.http.HttpClient) {
              continue;
            }

            // Check for HikariDataSource (HikariCP)
            if (typeName.equals("com.zaxxer.hikari.HikariDataSource")) {
              try {
                // Check if already closed
                var isClosedMethod = value.getClass().getMethod("isClosed");
                boolean isClosed = (boolean) isClosedMethod.invoke(value);

                if (!isClosed) {
                  var closeMethod = value.getClass().getMethod("close");
                  closeMethod.invoke(value);
                  plugin
                      .getLogger()
                      .info(
                          "Auto-closed HikariDataSource in field '"
                              + field.getName()
                              + "' for: "
                              + scriptName);
                  cleanedCount++;
                }
              } catch (Exception e) {
                // Ignore - might already be closed
              }
            }

            // Check for ExecutorService
            if (value instanceof java.util.concurrent.ExecutorService) {
              try {
                java.util.concurrent.ExecutorService executor =
                    (java.util.concurrent.ExecutorService) value;
                if (!executor.isShutdown()) {
                  executor.shutdown();
                  try {
                    if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                      executor.shutdownNow();
                    }
                  } catch (InterruptedException ie) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                  }
                  plugin
                      .getLogger()
                      .info(
                          "Auto-shutdown ExecutorService in field '"
                              + field.getName()
                              + "' for: "
                              + scriptName);
                  cleanedCount++;
                }
              } catch (Exception e) {
                // Ignore
              }
            }

            // Check for Thread
            if (value instanceof Thread) {
              try {
                Thread thread = (Thread) value;
                if (thread.isAlive() && !isServerThread(thread)) {
                  thread.interrupt();
                  plugin
                      .getLogger()
                      .info(
                          "Auto-interrupted Thread in field '"
                              + field.getName()
                              + "' for: "
                              + scriptName);
                  cleanedCount++;
                }
              } catch (Exception e) {
                // Ignore
              }
            }

            // Check for Closeable/AutoCloseable (but skip HikariDataSource since handled above)
            if (value instanceof AutoCloseable
                && !typeName.equals("com.zaxxer.hikari.HikariDataSource")) {
              try {
                ((AutoCloseable) value).close();
                plugin
                    .getLogger()
                    .info(
                        "Auto-closed "
                            + value.getClass().getSimpleName()
                            + " in field '"
                            + field.getName()
                            + "' for: "
                            + scriptName);
                cleanedCount++;
              } catch (Exception e) {
                // Ignore - might already be closed
              }
            }
          } catch (Exception e) {
            // Ignore individual field errors
          }
        }
        current = current.getSuperclass();
      }

      if (cleanedCount > 0) {
        plugin
            .getLogger()
            .info("Auto-cleaned " + cleanedCount + " custom resource(s) for: " + scriptName);
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning("Error scanning for custom resources (continuing): " + e.getMessage());
    }
  }

  /** Unregister the script instance itself and every other listener the script registered. */
  private void unregisterListeners() {
    if (instance instanceof Listener listener) {
      HandlerList.unregisterAll(listener);
    }
    unregisterScriptListeners();
  }

  /** Unregister the commands the script class registered by implementing CommandExecutor. */
  private void unregisterRegisteredCommands() {
    for (Map.Entry<String, Command> entry : new ArrayList<>(registeredCommands.entrySet())) {
      String commandName = entry.getKey();
      try {
        // Skipped when another script replaced the command, so we never remove its version
        boolean removed =
            plugin.getCommandRegistry().unregisterCommand(commandName, entry.getValue());

        // Verify it's actually gone
        if (removed && plugin.getCommandRegistry().isCommandInMap(commandName)) {
          plugin.getLogger().severe("Command still exists after unregister: /" + commandName);
        } else {
          plugin.debug("Verified command removed: /" + commandName);
        }
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning(
                "Error unregistering command /" + commandName + " (continuing): " + e.getMessage());
      }
    }
    registeredCommands.clear();
  }

  /**
   * Register a cleanup to run when this script unloads. Addons whose custom field injectors hand
   * scripts a resource that must not outlive them can release it here.
   */
  public void registerCleanup(String description, Runnable cleanup) {
    context.own(description, cleanup);
  }

  /**
   * Remove every event handler whose listener class was defined by this script's classloader.
   * Unregistering only the main instance left other listener objects the script registered with the
   * plugin manager running after unload, and reloads stacked duplicates of them.
   */
  private void unregisterScriptListeners() {
    if (classLoader == null) {
      return;
    }
    for (HandlerList handlerList : HandlerList.getHandlerLists()) {
      for (RegisteredListener registered : handlerList.getRegisteredListeners()) {
        if (registered.getListener().getClass().getClassLoader() == classLoader) {
          handlerList.unregister(registered);
        }
      }
    }
  }

  /** Threads the server or scheduler owns. A script holding one in a field must not stop it. */
  private static boolean isServerThread(Thread thread) {
    if (thread == Thread.currentThread()) {
      return true;
    }
    String name = thread.getName();
    return name.startsWith("Server thread")
        || name.startsWith("Region Scheduler Thread")
        || name.startsWith("Craft Scheduler Thread")
        || name.startsWith("Folia")
        || name.startsWith("Paper");
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
}
