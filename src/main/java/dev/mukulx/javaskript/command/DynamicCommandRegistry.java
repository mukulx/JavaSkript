package dev.mukulx.javaskript.command;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;

public class DynamicCommandRegistry {

  private final JavaSkriptPlugin plugin;
  private final Map<String, Command> registeredCommands;

  public DynamicCommandRegistry(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.registeredCommands = new ConcurrentHashMap<>();
  }

  public boolean registerCommand(String commandName, CommandExecutor executor, String... aliases) {
    if (commandName == null || commandName.isEmpty() || executor == null) {
      plugin.getLogger().warning("Cannot register command with null name or executor");
      return false;
    }

    try {
      Constructor<PluginCommand> constructor =
          PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
      constructor.setAccessible(true);

      PluginCommand command = constructor.newInstance(commandName.toLowerCase(), plugin);
      command.setExecutor(executor);

      if (executor instanceof TabCompleter tabCompleter) {
        command.setTabCompleter(tabCompleter);
      }

      if (aliases != null && aliases.length > 0) {
        command.setAliases(Arrays.asList(aliases));
      }

      CommandMap commandMap = getCommandMap();
      if (commandMap == null) {
        plugin.getLogger().severe("Failed to get command map!");
        return false;
      }

      unregisterCommand(commandName);

      boolean registered = commandMap.register(plugin.getName().toLowerCase(), command);

      if (registered) {
        registeredCommands.put(commandName.toLowerCase(), command);
        plugin.debug("Dynamically registered command: /" + commandName);
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

      Map<String, Command> knownCommands = getKnownCommandsMap(commandMap);
      if (knownCommands == null) {
        plugin
            .getLogger()
            .severe("Could not access knownCommands map - commands will remain registered!");
        registeredCommands.remove(lowerName);
        return false;
      }

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

      try {
        command.unregister(commandMap);
        plugin.debug("Called unregister() for: " + commandName);
      } catch (Exception e) {
        plugin.getLogger().warning("Error calling unregister (continuing): " + e.getMessage());
      }

      for (String key : keysToRemove) {
        knownCommands.remove(key);
        plugin.debug("Removed command key: " + key);
      }

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

      registeredCommands.remove(lowerName);

      syncCommands();
      plugin.debug("Successfully unregistered command: /" + commandName);
      return true;

    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "Error unregistering command: " + commandName, e);
      registeredCommands.remove(lowerName);
    }

    return false;
  }

  private java.lang.reflect.Method cachedGetKnownCommandsMethod;
  private java.lang.reflect.Field cachedKnownCommandsField;

  private Map<String, Command> getKnownCommandsMap(CommandMap commandMap) {
    if (commandMap == null) return null;

    if (cachedGetKnownCommandsMethod != null) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Command> commands =
            (Map<String, Command>) cachedGetKnownCommandsMethod.invoke(commandMap);
        return commands;
      } catch (Exception ignored) {
      }
    }

    if (cachedKnownCommandsField != null) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Command> commands =
            (Map<String, Command>) cachedKnownCommandsField.get(commandMap);
        return commands;
      } catch (Exception ignored) {
      }
    }

    try {
      var method = commandMap.getClass().getMethod("getKnownCommands");
      method.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, Command> commands = (Map<String, Command>) method.invoke(commandMap);
      cachedGetKnownCommandsMethod = method;
      return commands;
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      plugin.getLogger().warning("Error calling getKnownCommands(): " + e.getMessage());
    }

    String[] fieldNames = {"knownCommands", "commands"};
    Class<?> currentClass = commandMap.getClass();
    while (currentClass != null && currentClass != Object.class) {
      for (String fieldName : fieldNames) {
        try {
          Field field = currentClass.getDeclaredField(fieldName);
          field.setAccessible(true);
          @SuppressWarnings("unchecked")
          Map<String, Command> commands = (Map<String, Command>) field.get(commandMap);
          cachedKnownCommandsField = field;
          return commands;
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
          plugin.getLogger().warning("Error accessing field " + fieldName + ": " + e.getMessage());
        }
      }
      currentClass = currentClass.getSuperclass();
    }

    return null;
  }

  private void syncCommands() {
    try {
      try {
        var method = Bukkit.getServer().getClass().getMethod("syncCommands");
        method.invoke(Bukkit.getServer());
        plugin.debug("Synced commands via syncCommands()");
      } catch (NoSuchMethodException e) {
      }

      int updated = 0;
      for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
        try {
          if (player != null && player.isOnline()) {
            if (dev.mukulx.javaskript.util.ServerUtil.isFolia()) {
              player.getScheduler().run(plugin, task -> player.updateCommands(), null);
            } else {
              player.updateCommands();
            }
            updated++;
          }
        } catch (Exception e) {
        }
      }

      if (updated > 0) {
        plugin.debug("Updated commands for " + updated + " player(s)");
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Error syncing commands: " + e.getMessage());
    }
  }

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

  public void unregisterAll() {
    if (registeredCommands.isEmpty()) return;

    List<String> commands = new ArrayList<>(registeredCommands.keySet());
    for (String commandName : commands) {
      unregisterCommandSilent(commandName);
    }
    syncCommands();
  }

  private boolean unregisterCommandSilent(String commandName) {
    if (commandName == null || commandName.isEmpty()) {
      return false;
    }

    String lowerName = commandName.toLowerCase();

    try {
      CommandMap commandMap = getCommandMap();
      if (commandMap == null) {
        registeredCommands.remove(lowerName);
        return false;
      }

      Map<String, Command> knownCommands = getKnownCommandsMap(commandMap);
      if (knownCommands == null) {
        registeredCommands.remove(lowerName);
        return false;
      }

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
        registeredCommands.remove(lowerName);
        return false;
      }

      try {
        command.unregister(commandMap);
      } catch (Exception e) {
      }

      for (String key : keysToRemove) {
        knownCommands.remove(key);
      }

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
        }
      } catch (Exception e) {
      }

      registeredCommands.remove(lowerName);
      return true;

    } catch (Exception e) {
      registeredCommands.remove(lowerName);
    }

    return false;
  }

  public Map<String, Command> getRegisteredCommands() {
    return Collections.unmodifiableMap(registeredCommands);
  }

  public boolean isCommandRegistered(String commandName) {
    if (commandName == null || commandName.isEmpty()) {
      return false;
    }
    return registeredCommands.containsKey(commandName.toLowerCase());
  }

  private CommandMap cachedCommandMap;

  private CommandMap getCommandMap() {
    if (cachedCommandMap != null) {
      return cachedCommandMap;
    }

    try {
      try {
        var method = Bukkit.getServer().getClass().getMethod("getCommandMap");
        method.setAccessible(true);
        cachedCommandMap = (CommandMap) method.invoke(Bukkit.getServer());
        if (cachedCommandMap != null) return cachedCommandMap;
      } catch (NoSuchMethodException ignored) {
      }

      Class<?> serverClass = Bukkit.getServer().getClass();
      while (serverClass != null && serverClass != Object.class) {
        try {
          Field commandMapField = serverClass.getDeclaredField("commandMap");
          commandMapField.setAccessible(true);
          cachedCommandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
          if (cachedCommandMap != null) return cachedCommandMap;
        } catch (NoSuchFieldException ignored) {
          serverClass = serverClass.getSuperclass();
        }
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to get command map", e);
    }
    return cachedCommandMap;
  }
}
