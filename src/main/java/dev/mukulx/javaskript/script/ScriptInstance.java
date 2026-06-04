package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.*;
import dev.mukulx.javaskript.script.loader.ScriptClassLoader;
import dev.mukulx.javaskript.util.ServerUtil;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class ScriptInstance {

  private final JavaSkriptPlugin plugin;
  private final File scriptFile;
  private final Class<?> scriptClass;
  private final ScriptClassLoader classLoader;
  private Object instance;
  private final List<String> registeredCommands;

  // API instances for this script
  private ScriptScheduler scheduler;
  private ScriptConfig config;
  private DatabaseHelper database;
  private PlaceholderHelper placeholders;

  public ScriptInstance(
      JavaSkriptPlugin plugin,
      File scriptFile,
      Class<?> scriptClass,
      ScriptClassLoader classLoader) {
    this.plugin = plugin;
    this.scriptFile = scriptFile;
    this.scriptClass = scriptClass;
    this.classLoader = classLoader;
    this.registeredCommands = new ArrayList<>();
  }

  public boolean initialize() {
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
      String scriptName = scriptFile.getName();
      this.scheduler = new ScriptScheduler(plugin);
      this.config = new ScriptConfig(plugin, scriptName);
      this.database = new DatabaseHelper(plugin, scriptName);
      this.placeholders = new PlaceholderHelper(plugin, scriptName);

      // Inject API helpers into script instance
      injectAPIs();

      // Call onEnable method if it exists
      try {
        var onEnableMethod = scriptClass.getDeclaredMethod("onEnable");
        onEnableMethod.setAccessible(true);
        onEnableMethod.invoke(instance);
        plugin.debug("Called onEnable for: " + scriptFile.getName());
      } catch (NoSuchMethodException e) {
        // No onEnable method, that's fine
      } catch (Exception e) {
        plugin
            .getLogger()
            .log(Level.WARNING, "Error calling onEnable for: " + scriptFile.getName(), e);
      }

      // Register as event listener if applicable
      if (instance instanceof Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        plugin.debug("Registered event listener: " + scriptClass.getSimpleName());
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
      // Try to inject API helpers via fields
      injectField("api", plugin.getAPI());
      injectField("scheduler", scheduler);
      injectField("config", config);
      injectField("database", database);
      injectField("db", database); // Alternative name
      injectField("placeholders", placeholders);
      injectField("papi", placeholders); // Alternative name
      injectField("actionBar", plugin.getAPI().getActionBarHelper());

    } catch (Exception e) {
      // Injection is optional, scripts can also get APIs manually
    }
  }

  private void injectField(String fieldName, Object value) {
    try {
      Field field = scriptClass.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(instance, value);
    } catch (NoSuchFieldException e) {

    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "Failed to inject " + fieldName + " into script", e);
    }
  }

  private void registerCommand() {
    try {
      // Extract command name from class name (e.g., HealCommand -> heal)
      String className = scriptClass.getSimpleName();
      String commandName = className.toLowerCase().replace("command", "").replace("cmd", "");

      if (commandName.isEmpty()) {
        commandName = className.toLowerCase();
      }

      // Use dynamic command registration
      boolean registered =
          plugin.getCommandRegistry().registerCommand(commandName, (CommandExecutor) instance);

      if (registered) {
        registeredCommands.add(commandName);
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
    plugin.debug("Starting forced unload of: " + scriptName);

    // Call onDisable method if it exists (but don't let it stop the unload)
    try {
      var onDisableMethod = scriptClass.getDeclaredMethod("onDisable");
      onDisableMethod.setAccessible(true);
      onDisableMethod.invoke(instance);
      plugin.debug("Called onDisable for: " + scriptName);
    } catch (NoSuchMethodException e) {
      // No onDisable method, that's fine
    } catch (Exception e) {
      plugin.getLogger().warning("Error calling onDisable (continuing unload): " + e.getMessage());
    }

    // Cancel all scheduled tasks (force it)
    try {
      if (scheduler != null) {
        scheduler.cancelAll();
        scheduler = null;
        plugin.debug("Cancelled scheduled tasks for: " + scriptName);
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Error cancelling tasks (continuing): " + e.getMessage());
    }

    // Disconnect database (force it)
    try {
      if (database != null) {
        database.disconnect();
        database = null;
        plugin.debug("Disconnected database for: " + scriptName);
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Error disconnecting database (continuing): " + e.getMessage());
    }

    // Unregister placeholders (force it)
    try {
      if (placeholders != null) {
        placeholders.unregisterAll();
        placeholders = null;
        plugin.debug("Unregistered placeholders for: " + scriptName);
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning("Error unregistering placeholders (continuing): " + e.getMessage());
    }

    // Unregister events if listener (force it)
    try {
      if (instance instanceof Listener) {
        HandlerList.unregisterAll((Listener) instance);
        plugin.debug("Unregistered event listener: " + scriptClass.getSimpleName());
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Error unregistering events (continuing): " + e.getMessage());
    }

    // Unregister commands (force each one individually)
    List<String> commandsCopy = new ArrayList<>(registeredCommands);
    for (String commandName : commandsCopy) {
      try {
        boolean removed = plugin.getCommandRegistry().unregisterCommand(commandName);

        // Verify it's actually gone
        if (plugin.getCommandRegistry().isCommandInMap(commandName)) {
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

    // Clear config reference
    try {
      config = null;
    } catch (Exception e) {
      // Ignore
    }

    // Automatic cleanup of common custom resources
    // This helps scripts that don't have onDisable() but use custom resources
    try {
      cleanupCustomResources();
    } catch (Exception e) {
      plugin.getLogger().warning("Error during automatic cleanup (continuing): " + e.getMessage());
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
      // Get all fields from the script class
      Field[] fields = scriptClass.getDeclaredFields();

      for (Field field : fields) {
        try {
          field.setAccessible(true);
          Object value = field.get(instance);

          if (value == null) {
            continue;
          }

          // Check for HikariDataSource (HikariCP)
          if (value.getClass().getName().equals("com.zaxxer.hikari.HikariDataSource")) {
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
              if (thread.isAlive()) {
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

          // Check for Closeable/AutoCloseable (but skip HikariDataSource since we handled it above)
          if (value instanceof AutoCloseable
              && !value.getClass().getName().equals("com.zaxxer.hikari.HikariDataSource")) {
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
}
