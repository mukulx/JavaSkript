package dev.mukulx.javaskript;

import dev.mukulx.javaskript.api.JavaSkriptAPI;
import dev.mukulx.javaskript.api.gui.GUIManager;
import dev.mukulx.javaskript.command.DynamicCommandRegistry;
import dev.mukulx.javaskript.command.JavaSkriptCommand;
import dev.mukulx.javaskript.dependency.DependencyManager;
import dev.mukulx.javaskript.permission.DynamicPermissionRegistry;
import dev.mukulx.javaskript.script.ScriptManager;
import dev.mukulx.javaskript.util.ServerUtil;
import dev.mukulx.javaskript.watcher.FileWatcher;
import java.io.File;
import java.util.Set;
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
  private Thread folderMonitorThread;
  private boolean debugMode;

  @Override
  public void onEnable() {
    instance = this;

    try {
      displayLogo();

      // Ensure config.yml exists on disk
      saveDefaultConfig();

      // Cache debug flag to minimize runtime disk reads
      this.debugMode = getConfig().getBoolean("debug.enabled", false);
      if (debugMode) {
        getLogger().info("Debug mode is ENABLED. Enjoy the log pollution.");
      }

      // Anonymous metric collection via bStats
      int pluginId = 31615;
      Metrics metrics = new Metrics(this, pluginId);
      if (debugMode) {
        getLogger().info("bStats metrics enabled!");
      }

      // Folia regional multithreading will break non-thread-safe scripts
      getLogger().info("Running on: " + ServerUtil.getServerType());
      if (ServerUtil.isFolia()) {
        getLogger().info("Folia detected! Scripts without @FoliaSupport will completely break.");
      }

      // Handle dynamic runtime Maven dependency downloads
      this.dependencyManager = new DependencyManager(this);
      getLogger().info("Dependency manager initialized");

      // Inject script commands directly into the server routing table
      this.commandRegistry = new DynamicCommandRegistry(this);

      // Node-based permission trees
      this.permissionRegistry = new DynamicPermissionRegistry(this);

      // Register listener for dynamic GUI inventory packet clicks
      getServer().getPluginManager().registerEvents(new GUIManager(), this);

      // Core engine lifecycle manager
      this.scriptManager = new ScriptManager(this);

      // Developer API exposed for cross-plugin hooks
      this.api = new JavaSkriptAPI(this);

      // Manual command injection via Paper's CommandMap to bypass plugin.yml requirements
      var command = new JavaSkriptCommand(this);
      getServer()
          .getCommandMap()
          .register(
              "javaskript",
              new org.bukkit.command.defaults.BukkitCommand(
                  "javaskript",
                  "JavaSkript main command",
                  "/javaskript [reload|list|load|unload|enable|disable|info] [script]",
                  java.util.List.of("js", "jskript")) {
                @Override
                public boolean execute(
                    org.bukkit.command.CommandSender sender, String label, String[] args) {
                  // Direct delegation to sub-command system before the main thread suffers an
                  // existential crisis
                  return command.onCommand(sender, this, label, args);
                }

                @Override
                public java.util.List<String> tabComplete(
                    org.bukkit.command.CommandSender sender, String alias, String[] args) {
                  // Dynamic completions because expecting users to type correctly is a myth
                  return command.onTabComplete(sender, this, alias, args);
                }
              });

      // Synchronous boot-time execution of stored scripts
      if (getConfig().getBoolean("scripts.auto-load", true)) {
        scriptManager.loadAllScripts();
      }

      // Asynchronous NIO hot-swapper loop for live script edits
      if (getConfig().getBoolean("file-watcher.enabled", true)) {
        this.fileWatcher = new FileWatcher(this, scriptManager.getScriptsFolder());
        fileWatcher.start();
        getLogger().info("File watcher enabled - live reloads might trigger a mini-heart-attack.");
      } else {
        getLogger()
            .info("File watcher disabled - enjoy typing /js reload manually 400 times a day.");
      }

      // Run filesystem monitor to keep layout clean
      startFolderMonitoring();

      getLogger()
          .info(
              "JavaSkript has been enabled! Loaded "
                  + scriptManager.getLoadedScripts().size()
                  + " scripts. It's functional. Somehow.");

    } catch (Exception e) {
      // Emergency kill switch to prevent data leaks or corrupted state
      getLogger().log(Level.SEVERE, "Failed to enable JavaSkript! Everything is on fire.", e);
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    try {
      // Terminate background file listeners safely
      if (folderMonitorThread != null && folderMonitorThread.isAlive()) {
        folderMonitorThread.interrupt();
      }

      // Close open NIO watch keys
      if (fileWatcher != null) {
        fileWatcher.stop();
      }

      // Cleanup action bar tasks
      if (api != null && api.getActionBarHelper() != null) {
        api.getActionBarHelper().shutdown();
      }

      // Unload active maps to clear object references for the Garbage Collector
      if (scriptManager != null) {
        scriptManager.unloadAllScripts();
      }
      if (commandRegistry != null) {
        commandRegistry.unregisterAll();
      }
      if (permissionRegistry != null) {
        permissionRegistry.unregisterAll();
      }
      getLogger().info("JavaSkript has been disabled! Turning off the lights.");
    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Error during plugin disable! Even dying failed.", e);
    }
  }

  public static JavaSkriptPlugin getInstance() {
    return instance;
  }

  public ScriptManager getScriptManager() {
    return scriptManager;
  }

  public JavaSkriptAPI getAPI() {
    return api;
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

  private void startFolderMonitoring() {
    // Separate thread lifecycle because Folia will panic if you block regional tickers
    folderMonitorThread =
        new Thread(
            () -> {
              while (!Thread.currentThread().isInterrupted()) {
                try {
                  Thread.sleep(300000); // 5-minute file safety checks
                  checkForMisplacedFolders();
                } catch (InterruptedException e) {
                  break;
                }
              }
            },
            "JavaSkript-FolderMonitor");
    folderMonitorThread.setDaemon(true); // Allow the JVM to shut down without getting stuck
    folderMonitorThread.start();
  }

  private void checkForMisplacedFolders() {
    // Escape hatch for layout anarchy
    if (getConfig().getBoolean("scripts.allow-unrestricted-folders", false)) {
      return;
    }

    File dataFolder = getDataFolder();
    if (!dataFolder.exists()) {
      return;
    }

    // Whitelisted core layout folders managed by the engine
    Set<String> systemFolders = Set.of("scripts", "libs", "temp", ".git", ".github");

    File[] files = dataFolder.listFiles();
    if (files == null) {
      return;
    }

    for (File file : files) {
      if (!file.isDirectory()) {
        continue;
      }

      String folderName = file.getName();

      if (systemFolders.contains(folderName)) {
        continue;
      }

      if (folderName.equals("script-data")) {
        continue;
      }

      // Spit out massive warning frames to scold devs who cannot organize files
      getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      getLogger().warning("MISPLACED FOLDER DETECTED! ANARCHY!");
      getLogger().warning("");
      getLogger().warning("Folder: " + folderName);
      getLogger().warning("Location: plugins/JavaSkript/" + folderName);
      getLogger().warning("");
      getLogger().warning("Scripts should NOT create folders directly in the JavaSkript folder!");
      getLogger().warning("ALL script data must go in the script-data folder:");
      getLogger().warning("");
      getLogger()
          .warning(
              "  File scriptData = new File(JavaSkriptPlugin.getInstance().getDataFolder(),"
                  + " \"script-data\");");
      getLogger().warning("  File myFolder = new File(scriptData, \"MyScriptName\");");
      getLogger().warning("  myFolder.mkdirs();");
      getLogger().warning("");
      getLogger()
          .warning("This keeps all script data organized in: plugins/JavaSkript/script-data/");
      getLogger().warning("");
      getLogger()
          .warning(
              "To disable this check, set 'scripts.allow-unrestricted-folders: true' in"
                  + " config.yml");
      getLogger().warning("(Not recommended - keeping things messy is a skill issue.)");
      getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
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
