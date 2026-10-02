package dev.mukulx.javaskript;

import dev.mukulx.javaskript.api.JavaSkriptAPI;
import dev.mukulx.javaskript.api.gui.GUIManager;
import dev.mukulx.javaskript.command.DynamicCommandRegistry;
import dev.mukulx.javaskript.command.JavaSkriptCommand;
import dev.mukulx.javaskript.dependency.DependencyManager;
import dev.mukulx.javaskript.permission.DynamicPermissionRegistry;
import dev.mukulx.javaskript.profiler.ScriptProfiler;
import dev.mukulx.javaskript.script.ScriptManager;
import dev.mukulx.javaskript.update.UpdateChecker;
import dev.mukulx.javaskript.util.ServerUtil;
import dev.mukulx.javaskript.watcher.FileWatcher;
import java.util.logging.Level;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class JavaSkriptPlugin extends JavaPlugin {

  private static JavaSkriptPlugin instance;
  private ScriptManager scriptManager;
  private JavaSkriptAPI api;
  private DynamicCommandRegistry commandRegistry;
  private DynamicPermissionRegistry permissionRegistry;
  private FileWatcher fileWatcher;
  private DependencyManager dependencyManager;
  private UpdateChecker updateChecker;
  private ScriptProfiler profiler;
  private dev.mukulx.javaskript.api.economy.EconomyHelper economyHelper;
  private dev.mukulx.javaskript.api.variable.VariableHelper variableHelper;
  private dev.mukulx.javaskript.api.event.ScriptEventBus eventBus;
  private dev.mukulx.javaskript.api.http.HttpHelper httpHelper;
  private dev.mukulx.javaskript.api.message.MessageManager messageManager;
  private dev.mukulx.javaskript.api.team.TeamHelper teamHelper;
  private dev.mukulx.javaskript.command.TeamCommand teamCommand;
  private boolean debugMode;

  @Override
  public void onEnable() {
    instance = this;

    try {
      displayLogo();

      // Ensure config.yml and messages.yml exist on disk
      saveDefaultConfig();
      this.messageManager = new dev.mukulx.javaskript.api.message.MessageManager(this);

      // Cache debug flag to minimize runtime disk reads
      this.debugMode = getConfig().getBoolean("debug.enabled", false);
      if (debugMode) {
        getLogger().info("Debug mode is ENABLED. Enjoy the log pollution.");
      }

      // Anonymous metric collection via bStats (fails gracefully if offline)
      try {
        int pluginId = 31615;
        new Metrics(this, pluginId);
      } catch (Throwable t) {
        debug("bStats metrics could not be initialized: " + t.getMessage());
      }

      debug("Running on: " + ServerUtil.getServerType());
      if (ServerUtil.isFolia()) {
        getLogger().info("Folia detected! Scripts without @FoliaSupport will break.");
      }

      // Initialize performance profiler and benchmark engine
      this.profiler = new ScriptProfiler(this);

      this.dependencyManager = new DependencyManager(this);

      // Inject script commands directly into the server routing table
      this.commandRegistry = new DynamicCommandRegistry(this);

      // Node-based permission trees
      this.permissionRegistry = new DynamicPermissionRegistry(this);

      // Register listener for dynamic GUI inventory packet clicks
      try {
        getServer().getPluginManager().registerEvents(new GUIManager(), this);
      } catch (Throwable t) {
        getLogger().warning("Failed to register GUI manager events: " + t.getMessage());
      }

      // Core engine lifecycle manager
      this.scriptManager = new ScriptManager(this);

      // Developer API exposed for cross-plugin hooks
      this.api = new JavaSkriptAPI(this);
      dev.mukulx.javaskript.api.player.Players.setInstance(
          new dev.mukulx.javaskript.api.player.PlayerHelper(this));

      // Inter-script event bus
      this.eventBus = new dev.mukulx.javaskript.api.event.ScriptEventBus(this);

      // Shared inter-script variable storage
      this.variableHelper = new dev.mukulx.javaskript.api.variable.VariableHelper(this);
      dev.mukulx.javaskript.api.variable.Variables.setInstance(variableHelper);

      // Built-in async HTTP & Discord Webhook client
      this.httpHelper = new dev.mukulx.javaskript.api.http.HttpHelper(this);
      dev.mukulx.javaskript.api.http.Http.setInstance(httpHelper);

      // Economy subsystem (Vault & Built-in SQLite)
      try {
        this.economyHelper = new dev.mukulx.javaskript.api.economy.EconomyHelper(this);
        dev.mukulx.javaskript.api.economy.Economy.setInstance(economyHelper);
      } catch (Throwable t) {
        getLogger().warning("Failed to initialize Economy subsystem: " + t.getMessage());
      }

      // Team / Clan subsystem
      try {
        if (getConfig().getBoolean("teams.enabled", true)) {
          this.teamHelper = new dev.mukulx.javaskript.api.team.TeamHelper(this);
          dev.mukulx.javaskript.api.team.Teams.setInstance(teamHelper);
          if (getConfig().getBoolean("teams.register-command", true)) {
            registerTeamCommand();
          }
        }
      } catch (Throwable t) {
        getLogger().warning("Failed to initialize Team subsystem: " + t.getMessage());
      }

      // Register main command handler
      this.commandRegistry.registerCommand(
          "javaskript", new JavaSkriptCommand(this), "js", "jskript");

      // Register internal interactive chat action handler
      this.commandRegistry.registerCommand(
          "__jsk_action",
          (sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player && args.length > 0) {
              dev.mukulx.javaskript.api.chat.ChatHelper.executeGlobalAction(player, args[0]);
            }
            return true;
          });

      // Boot-time scripts load one tick later so enable finishes fast and never
      // blocks the server while compiling. Heavy compile work stays off the enable path.
      if (getConfig().getBoolean("scripts.auto-load", true)) {
        try {
          dev.mukulx.javaskript.util.ServerUtil.runLaterSync(
              this,
              () -> {
                try {
                  scriptManager.loadAllScripts();
                } catch (Throwable t) {
                  getLogger()
                      .severe(
                          "Error occurred during script auto-load (plugin remains running): "
                              + t.getMessage());
                }
              },
              1L);
        } catch (Throwable t) {
          getLogger()
              .severe(
                  "Error occurred during script auto-load (plugin remains running): "
                      + t.getMessage());
        }
      }

      // Asynchronous NIO hot-swapper loop for live script edits
      if (getConfig().getBoolean("file-watcher.enabled", true)) {
        try {
          this.fileWatcher = new FileWatcher(this, scriptManager.getScriptsFolder());
          fileWatcher.start();
          debug("File watcher enabled");
        } catch (Throwable t) {
          getLogger()
              .warning("Failed to start file watcher (live reload disabled): " + t.getMessage());
        }
      }

      if (getConfig().getBoolean("update-checker.enabled", true)) {
        try {
          this.updateChecker = new UpdateChecker(this);
          updateChecker.checkAsync();
        } catch (Throwable t) {
          debug("Update checker check failed: " + t.getMessage());
        }
      }

      int loadedCount = scriptManager != null ? scriptManager.getLoadedScripts().size() : 0;
      getLogger().info("Enabled! Loaded " + loadedCount + " script(s)");

    } catch (Exception e) {
      // Emergency kill switch to prevent data leaks or corrupted state
      getLogger().log(Level.SEVERE, "Failed to enable JavaSkript! Everything is on fire.", e);
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    // Close open menus first so click handlers still exist, then drop the registry.
    // Without this players keep ghost menus they can take items from after reload.
    try {
      dev.mukulx.javaskript.api.gui.GUIManager.closeAll();
    } catch (Throwable t) {
      debug("Error closing GUIs: " + t.getMessage());
    }

    // Close open NIO watch keys
    try {
      if (fileWatcher != null) {
        fileWatcher.stop();
      }
    } catch (Throwable t) {
      debug("Error stopping file watcher: " + t.getMessage());
    }

    // Cleanup action bar tasks
    try {
      if (api != null && api.getActionBarHelper() != null) {
        api.getActionBarHelper().shutdown();
      }
    } catch (Throwable t) {
      debug("Error shutting down action bar helper: " + t.getMessage());
    }

    // Unload active scripts
    try {
      if (scriptManager != null) {
        scriptManager.unloadAllScripts();
      }
    } catch (Throwable t) {
      getLogger().log(Level.SEVERE, "Error unloading scripts on disable: " + t.getMessage(), t);
    }

    // Unregister custom commands
    try {
      if (commandRegistry != null) {
        commandRegistry.unregisterAll();
      }
    } catch (Throwable t) {
      getLogger().warning("Error unregistering commands on disable: " + t.getMessage());
    }

    // Unregister permissions
    try {
      if (permissionRegistry != null) {
        permissionRegistry.unregisterAll();
      }
    } catch (Throwable t) {
      getLogger().warning("Error unregistering permissions on disable: " + t.getMessage());
    }

    // Shutdown economy subsystem
    try {
      if (economyHelper != null) {
        economyHelper.shutdown();
      }
    } catch (Throwable t) {
      debug("Error shutting down economy helper: " + t.getMessage());
    }

    // Shutdown team subsystem
    try {
      if (teamHelper != null) {
        teamHelper.shutdown();
      }
    } catch (Throwable t) {
      debug("Error shutting down team helper: " + t.getMessage());
    }

    // Save shared persistent variables
    try {
      if (variableHelper != null) {
        variableHelper.shutdown();
      }
    } catch (Throwable t) {
      debug("Error shutting down variable helper: " + t.getMessage());
    }

    // Clear event bus
    try {
      if (eventBus != null) {
        eventBus.clear();
      }
    } catch (Throwable t) {
      debug("Error clearing event bus: " + t.getMessage());
    }

    // Shutdown addons and custom injectors
    try {
      if (api != null && api.getAddonRegistry() != null) {
        api.getAddonRegistry().shutdown();
      }
    } catch (Throwable t) {
      debug("Error shutting down addon registry: " + t.getMessage());
    }

    // Drop static reference so a reload cannot reuse a disabled plugin instance.
    instance = null;

    getLogger().info("JavaSkript has been disabled!");
  }

  public static JavaSkriptPlugin getInstance() {
    return instance;
  }

  public ScriptManager getScriptManager() {
    return scriptManager;
  }

  public dev.mukulx.javaskript.api.economy.EconomyHelper getEconomyHelper() {
    return economyHelper;
  }

  public dev.mukulx.javaskript.api.team.TeamHelper getTeamHelper() {
    return teamHelper;
  }

  /**
   * Reload config.yml and refresh all cached runtime settings derived from it. Call this instead of
   * Bukkit's reloadConfig() so the cached debug flag, message bundle, economy provider, team
   * subsystem, and file-watcher delay stay in sync with disk.
   */
  public synchronized void reloadPluginConfig() {
    reloadConfig();
    this.debugMode = getConfig().getBoolean("debug.enabled", false);
    if (debugMode) {
      getLogger().info("Debug mode is ENABLED. Enjoy the log pollution.");
    }
    if (messageManager != null) {
      messageManager.reload();
    }
    if (economyHelper != null) {
      economyHelper.reload();
    }
    reloadTeamHelper();
    boolean watcherEnabled = getConfig().getBoolean("file-watcher.enabled", true);
    if (fileWatcher != null) {
      if (!watcherEnabled && fileWatcher.isRunning()) {
        fileWatcher.stop();
      } else if (watcherEnabled && fileWatcher.isRunning()) {
        fileWatcher.refreshSettings();
      } else if (watcherEnabled) {
        // A stopped watcher cannot restart (executor and WatchService are closed), recreate it.
        try {
          fileWatcher = new FileWatcher(this, scriptManager.getScriptsFolder());
          fileWatcher.start();
        } catch (Throwable t) {
          getLogger().warning("Failed to restart file watcher: " + t.getMessage());
        }
      }
    } else if (watcherEnabled && scriptManager != null) {
      try {
        fileWatcher = new FileWatcher(this, scriptManager.getScriptsFolder());
        fileWatcher.start();
      } catch (Throwable t) {
        getLogger().warning("Failed to start file watcher: " + t.getMessage());
      }
    }
  }

  /** Dynamically reloads or enables/disables the Team subsystem based on config.yml. */
  public synchronized void reloadTeamHelper() {
    boolean enabled = getConfig().getBoolean("teams.enabled", true);
    boolean regCmd = getConfig().getBoolean("teams.register-command", true);
    if (enabled) {
      if (this.teamHelper == null) {
        try {
          this.teamHelper = new dev.mukulx.javaskript.api.team.TeamHelper(this);
          dev.mukulx.javaskript.api.team.Teams.setInstance(teamHelper);
          getLogger().info("Team subsystem enabled and initialized on reload.");
        } catch (Throwable t) {
          getLogger().warning("Failed to initialize Team subsystem on reload: " + t.getMessage());
        }
      } else {
        this.teamHelper.reload();
      }
      if (regCmd) {
        registerTeamCommand();
      } else {
        unregisterTeamCommand();
      }
    } else {
      unregisterTeamCommand();
      if (this.teamHelper != null) {
        try {
          this.teamHelper.shutdown();
        } catch (Throwable t) {
          debug("Error shutting down team helper: " + t.getMessage());
        }
        this.teamHelper = null;
        dev.mukulx.javaskript.api.team.Teams.setInstance(null);
        getLogger().info("Team subsystem disabled on reload.");
      }
    }
  }

  public synchronized void registerTeamCommand() {
    if (this.teamCommand == null) {
      this.teamCommand = new dev.mukulx.javaskript.command.TeamCommand(this);
    }
    this.commandRegistry.registerCommand(
        "team",
        teamCommand,
        teamCommand,
        "Universal Team and Clan management command",
        "/team <help|create|invite|join|leave|kick|disband|transfer|sethome|home|chat|deposit|withdraw|ff|info>",
        "javaskript.team",
        null,
        java.util.List.of("clan", "party", "t"));
  }

  public synchronized void unregisterTeamCommand() {
    if (this.commandRegistry != null) {
      this.commandRegistry.unregisterCommand("team");
    }
  }

  public JavaSkriptAPI getAPI() {
    return api;
  }

  public dev.mukulx.javaskript.api.variable.VariableHelper getVariableHelper() {
    return variableHelper;
  }

  public dev.mukulx.javaskript.api.event.ScriptEventBus getEventBus() {
    return eventBus;
  }

  public dev.mukulx.javaskript.api.http.HttpHelper getHttpHelper() {
    return httpHelper;
  }

  public dev.mukulx.javaskript.api.message.MessageManager getMessageManager() {
    return messageManager;
  }

  public DynamicCommandRegistry getCommandRegistry() {
    return commandRegistry;
  }

  public DynamicPermissionRegistry getPermissionRegistry() {
    return permissionRegistry;
  }

  public FileWatcher getFileWatcher() {
    return fileWatcher;
  }

  public DependencyManager getDependencyManager() {
    return dependencyManager;
  }

  public ScriptProfiler getProfiler() {
    return profiler;
  }

  private void displayLogo() {
    var logger = getComponentLogger();
    var orange = net.kyori.adventure.text.format.NamedTextColor.GOLD;

    String[] lines = {
      "       ██╗ █████╗ ██╗   ██╗ █████╗ ███████╗██╗  ██╗██████╗ ██╗██████╗████████╗",
      "       ██║██╔══██╗██║   ██║██╔══██╗██╔════╝██║ ██╔╝██╔══██╗██║██╔══██╗╚══██╔══╝",
      "       ██║███████║██║   ██║███████║███████╗█████╔╝ ██████╔╝██║██████╔╝   ██║",
      "  ██   ██║██╔══██║╚██╗ ██╔╝██╔══██║╚════██║██╔═██╗ ██╔══██╗██║██╔═══╝    ██║",
      "  ╚█████╔╝██║  ██║ ╚████╔╝ ██║  ██║███████║██║  ██╗██║  ██║██║██║        ██║",
      "   ╚════╝ ╚═╝  ╚═╝  ╚═══╝  ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝╚═╝        ╚═╝",
    };

    logger.info(net.kyori.adventure.text.Component.empty());
    for (String line : lines) {
      logger.info(net.kyori.adventure.text.Component.text(line, orange));
    }
    logger.info(net.kyori.adventure.text.Component.empty());
    logger.info(
        net.kyori.adventure.text.Component.text(
            "https://github.com/mukulx/javaskript",
            net.kyori.adventure.text.format.NamedTextColor.GOLD));
    logger.info(net.kyori.adventure.text.Component.empty());
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public void setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
    getConfig().set("debug.enabled", debugMode);
    saveConfig(); // Flush dynamic debug configurations straight to disk
  }

  public void debug(String message) {
    if (debugMode) {
      getLogger().info("[DEBUG] " + message);
    }
  }
}
