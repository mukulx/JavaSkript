package dev.mukulx.javaskript.command;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;

/** Dynamically registers commands without needing plugin.yml entries */
public class DynamicCommandRegistry {

  private final JavaSkriptPlugin plugin;
  private final Map<String, Command> registeredCommands;

  public DynamicCommandRegistry(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.registeredCommands = new HashMap<>();
  }

  /**
   * Register a command dynamically
   *
   * @param commandName The name of the command (without /)
   * @param executor The command executor
   * @param aliases Optional aliases for the command
   * @return true if successful
   */
  public boolean registerCommand(String commandName, CommandExecutor executor, String... aliases) {
    if (commandName == null || commandName.isEmpty() || executor == null) {
      plugin.getLogger().warning("Cannot register command with null name or executor");
      return false;
    }

    try {
      // Create a PluginCommand instance
      Constructor<PluginCommand> constructor =
          PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
      constructor.setAccessible(true);

      PluginCommand command = constructor.newInstance(commandName.toLowerCase(), plugin);
      command.setExecutor(executor);

      // Set tab completer if executor implements it
      if (executor instanceof TabCompleter tabCompleter) {
        command.setTabCompleter(tabCompleter);
      }

      // Set aliases
      if (aliases != null && aliases.length > 0) {
        command.setAliases(Arrays.asList(aliases));
      }

      // Get the command map
      CommandMap commandMap = getCommandMap();
      if (commandMap == null) {
        plugin.getLogger().severe("Failed to get command map!");
        return false;
      }

      // Unregister existing command if present
      unregisterCommand(commandName);

      // Register the command
      boolean registered = commandMap.register(plugin.getName().toLowerCase(), command);

      if (registered) {
        registeredCommands.put(commandName.toLowerCase(), command);
        plugin.debug("Dynamically registered command: /" + commandName);

        // Sync commands to clients (for tab completion)
        syncCommands();

        return true;
      } else {
        plugin.getLogger().warning("Failed to register command: /" + commandName);
        return false;
      }

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error registering command: " + commandName, e);
      return false;
    }
  }

  /**
   * Unregister a command using dummy command replacement technique. This forces Paper/Folia's
   * Brigadier system to rebuild correctly.
   *
   * <p>Process: 1. Find command in map 2. Unregister original command 3. Remove from knownCommands
   * map 4. Sync commands (forces Brigadier rebuild)
   *
   * @param commandName The name of the command to unregister
   * @return true if successful
   */
  public boolean unregisterCommand(String commandName) {
    if (commandName == null || commandName.isEmpty()) {
      return false;
    }

    String lowerName = commandName.toLowerCase();

    try {
      CommandMap commandMap = getCommandMap();
      if (commandMap == null) {
        plugin.getLogger().warning("Could not get command map for unregistration");
        registeredCommands.remove(lowerName);
        return false;
      }

      // STEP 1: Get knownCommands map
      Map<String, Command> knownCommands = getKnownCommandsMap(commandMap);
      if (knownCommands == null) {
        plugin
            .getLogger()
            .severe("Could not access knownCommands map - commands will remain registered!");
        registeredCommands.remove(lowerName);
        return false;
      }

      // STEP 2: Find the command in the map
      Command command = null;
      List<String> keysToRemove = new ArrayList<>();
      List<String> keysToTry = new ArrayList<>();
      keysToTry.add(lowerName);
      keysToTry.add(plugin.getName().toLowerCase() + ":" + lowerName);
      keysToTry.add("javaskript:" + lowerName);
      keysToTry.add("js:" + lowerName);

      for (String key : keysToTry) {
        Command found = knownCommands.get(key);
        if (found != null) {
          command = found;
          keysToRemove.add(key);
        }
      }

      if (command == null) {
        plugin.debug("Command not found in map: " + commandName);
        registeredCommands.remove(lowerName);
        return false;
      }

      plugin.debug("Found command keys to remove: " + keysToRemove);

      // STEP 3: Unregister the command
      try {
        command.unregister(commandMap);
        plugin.debug("Called unregister() for: " + commandName);
      } catch (Exception e) {
        plugin.getLogger().warning("Error calling unregister (continuing): " + e.getMessage());
      }

      // STEP 4: Remove from knownCommands map
      for (String key : keysToRemove) {
        knownCommands.remove(key);
        plugin.debug("Removed command key: " + key);
      }

      // STEP 5: Remove aliases
      try {
        List<String> aliases = command.getAliases();
        if (aliases != null && !aliases.isEmpty()) {
          for (String alias : aliases) {
            String aliasLower = alias.toLowerCase();
            knownCommands.remove(aliasLower);
            knownCommands.remove(plugin.getName().toLowerCase() + ":" + aliasLower);
            knownCommands.remove("javaskript:" + aliasLower);
            knownCommands.remove("js:" + aliasLower);
          }
          plugin.debug("Removed " + aliases.size() + " alias(es)");
        }
      } catch (Exception e) {
        plugin.getLogger().warning("Error removing aliases (continuing): " + e.getMessage());
      }

      // Remove from our tracking map
      registeredCommands.remove(lowerName);

      // STEP 6: Sync commands to force Brigadier rebuild
      syncCommands();

      plugin.debug("Successfully unregistered command: /" + commandName);
      return true;

    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "Error unregistering command: " + commandName, e);
      registeredCommands.remove(lowerName);
    }

    return false;
  }

  /**
   * Get the knownCommands map from CommandMap using multiple methods. Tries getKnownCommands()
   * method first, then falls back to reflection.
   */
  private Map<String, Command> getKnownCommandsMap(CommandMap commandMap) {
    // Try getKnownCommands() method first
    try {
      var method = commandMap.getClass().getMethod("getKnownCommands");
      @SuppressWarnings("unchecked")
      Map<String, Command> commands = (Map<String, Command>) method.invoke(commandMap);
      return commands;
    } catch (NoSuchMethodException e) {
      // Method doesn't exist, try reflection
    } catch (Exception e) {
      plugin.getLogger().warning("Error calling getKnownCommands(): " + e.getMessage());
    }

    // Try reflection on field
    String[] fieldNames = {"knownCommands", "commands"};
    for (String fieldName : fieldNames) {
      try {
        Field field = commandMap.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Command> commands = (Map<String, Command>) field.get(commandMap);
        field.setAccessible(false);
        return commands;
      } catch (NoSuchFieldException e) {
        // Try next field name
      } catch (Exception e) {
        plugin.getLogger().warning("Error accessing field " + fieldName + ": " + e.getMessage());
      }
    }

    // Try direct access to SimpleCommandMap.knownCommands as last resort
    try {
      Class<?> simpleCommandMapClass = Class.forName("org.bukkit.command.SimpleCommandMap");
      Field field = simpleCommandMapClass.getDeclaredField("knownCommands");
      field.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, Command> commands = (Map<String, Command>) field.get(commandMap);
      field.setAccessible(false);
      plugin.getLogger().info("Accessed knownCommands via SimpleCommandMap");
      return commands;
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to access knownCommands: " + e.getMessage());
    }

    return null;
  }

  /**
   * Sync commands to force Brigadier rebuild. This updates the command tree for all online players.
   */
  private void syncCommands() {
    try {
      // Try to call syncCommands() on server (Paper/Folia)
      try {
        var method = Bukkit.getServer().getClass().getMethod("syncCommands");
        method.invoke(Bukkit.getServer());
        plugin.debug("Synced commands via syncCommands()");
      } catch (NoSuchMethodException e) {
        // Method doesn't exist on this server version
      }

      // Update commands for all online players
      int updated = 0;
      for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
        try {
          player.updateCommands();
          updated++;
        } catch (Exception e) {
          // Ignore individual player errors
        }
      }

      if (updated > 0) {
        plugin.debug("Updated commands for " + updated + " player(s)");
      }

      // Small delay to let sync complete
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

    } catch (Exception e) {
      plugin.getLogger().warning("Error syncing commands: " + e.getMessage());
    }
  }

  /**
   * Debug method to check if a command still exists in the command map
   *
   * @param commandName The command name to check
   * @return true if command still exists
   */
  public boolean isCommandInMap(String commandName) {
    try {
      CommandMap commandMap = getCommandMap();
      if (commandMap == null) {
        return false;
      }

      Map<String, Command> knownCommands = getKnownCommandsMap(commandMap);
      if (knownCommands == null) {
        return false;
      }

      String lowerName = commandName.toLowerCase();

      // Check all possible variations
      List<String> keysToCheck = new ArrayList<>();
      keysToCheck.add(lowerName);
      keysToCheck.add(plugin.getName().toLowerCase() + ":" + lowerName);
      keysToCheck.add("javaskript:" + lowerName);
      keysToCheck.add("js:" + lowerName);

      for (String key : keysToCheck) {
        if (knownCommands.containsKey(key)) {
          plugin.getLogger().warning("Command still in map: " + key);
          return true;
        }
      }

      return false;

    } catch (Exception e) {
      return false;
    }
  }

  /** Unregister all commands registered by this registry */
  public void unregisterAll() {
    List<String> commands = new ArrayList<>(registeredCommands.keySet());
    for (String commandName : commands) {
      unregisterCommand(commandName);
    }
  }

  /**
   * Get all registered commands
   *
   * @return Map of command names to Command objects
   */
  public Map<String, Command> getRegisteredCommands() {
    return Collections.unmodifiableMap(registeredCommands);
  }

  /**
   * Check if a command is registered
   *
   * @param commandName The command name to check
   * @return true if registered
   */
  public boolean isCommandRegistered(String commandName) {
    if (commandName == null || commandName.isEmpty()) {
      return false;
    }
    return registeredCommands.containsKey(commandName.toLowerCase());
  }

  /** Get the server's command map using reflection */
  private CommandMap getCommandMap() {
    try {
      Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
      commandMapField.setAccessible(true);
      return (CommandMap) commandMapField.get(Bukkit.getServer());
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to get command map", e);
      return null;
    }
  }
}
