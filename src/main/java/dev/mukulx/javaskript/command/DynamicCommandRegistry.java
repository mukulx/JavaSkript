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

  @SuppressWarnings("deprecation")
  public synchronized boolean registerCommand(
      String commandName,
      CommandExecutor executor,
      TabCompleter tabCompleter,
      String description,
      String usage,
      String permission,
      String permissionMessage,
      List<String> aliases) {
    if (commandName == null || commandName.isEmpty() || executor == null) {
      plugin.getLogger().warning("Cannot register command with null name or executor");
      return false;
    }

    try {
      Constructor<PluginCommand> constructor =
          PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
      constructor.setAccessible(true);

      PluginCommand command = constructor.newInstance(commandName.toLowerCase(), plugin);

      // Safe error-isolated command executor
      CommandExecutor safeExecutor =
          (sender, cmd, label, args) -> {
            try {
              return executor.onCommand(sender, cmd, label, args);
            } catch (Throwable t) {
              plugin
                  .getLogger()
                  .log(
                      Level.SEVERE,
                      "Unhandled exception in command /" + commandName + ": " + t.getMessage(),
                      t);
              if (sender != null) {
                sender.sendMessage(
                    net.kyori.adventure.text.Component.text(
                        "§cAn error occurred while executing /" + label + ": " + t.getMessage()));
              }
              return true;
            }
          };
      command.setExecutor(safeExecutor);

      // Safe error-isolated tab completer
      TabCompleter effectiveTabCompleter =
          (tabCompleter != null) ? tabCompleter : (executor instanceof TabCompleter tc ? tc : null);

      if (effectiveTabCompleter != null) {
        TabCompleter safeTabCompleter =
            (sender, cmd, label, args) -> {
              try {
                List<String> results =
                    effectiveTabCompleter.onTabComplete(sender, cmd, label, args);
                return results != null ? results : java.util.Collections.emptyList();
              } catch (Throwable t) {
                plugin.debug("Error in tab completion for /" + commandName + ": " + t.getMessage());
                return java.util.Collections.emptyList();
              }
            };
        command.setTabCompleter(safeTabCompleter);
      }

      if (description != null && !description.isEmpty()) {
        command.setDescription(description);
      }
      if (usage != null && !usage.isEmpty()) {
        command.setUsage(usage);
      }
      if (permission != null && !permission.isEmpty()) {
        command.setPermission(permission);
      }
      if (permissionMessage != null && !permissionMessage.isEmpty()) {
        command.permissionMessage(net.kyori.adventure.text.Component.text(permissionMessage));
      }
      if (aliases != null && !aliases.isEmpty()) {
        command.setAliases(aliases);
      }

      CommandMap commandMap = getCommandMap();
      if (commandMap == null) {
        plugin.getLogger().severe("Failed to get command map!");
        return false;
      }

      unregisterCommandSilent(commandName);

      boolean claimedLabel = commandMap.register(plugin.getName().toLowerCase(), command);

      // The prefixed label is always registered, so track the command either way
      registeredCommands.put(commandName.toLowerCase(), command);
      if (!claimedLabel) {
        plugin
            .getLogger()
            .warning(
                "/"
                    + commandName
                    + " is already provided by another plugin and was left untouched. "
                    + "The script command is available as /"
                    + plugin.getName().toLowerCase()
                    + ":"
                    + commandName.toLowerCase());
      }
      plugin.debug("Dynamically registered command: /" + commandName);
      syncCommands();
      return true;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error registering command: " + commandName, e);
      return false;
    }
  }

  public synchronized boolean registerCommand(
      String commandName, CommandExecutor executor, String... aliases) {
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

      unregisterCommandSilent(commandName);

      boolean claimedLabel = commandMap.register(plugin.getName().toLowerCase(), command);

      // The prefixed label is always registered, so track the command either way
      registeredCommands.put(commandName.toLowerCase(), command);
      if (!claimedLabel) {
        plugin
            .getLogger()
            .warning(
                "/"
                    + commandName
                    + " is already provided by another plugin and was left untouched. "
                    + "The script command is available as /"
                    + plugin.getName().toLowerCase()
                    + ":"
                    + commandName.toLowerCase());
      }
      plugin.debug("Dynamically registered command: /" + commandName);
      syncCommands();
      return true;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error registering command: " + commandName, e);
      return false;
    }
  }

  public synchronized boolean unregisterCommand(String commandName) {
    if (commandName == null || commandName.isEmpty()) {
      return false;
    }

    boolean result = unregisterCommandSilent(commandName);
    if (result) {
      syncCommands();
    }
    return result;
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

  private final java.util.concurrent.atomic.AtomicBoolean syncPending =
      new java.util.concurrent.atomic.AtomicBoolean(false);

  public void syncCommands() {
    if (syncPending.compareAndSet(false, true)) {
      dev.mukulx.javaskript.util.ServerUtil.runLaterSync(plugin, this::runSyncCommands, 5L);
    }
  }

  private void runSyncCommands() {
    syncPending.set(false);
    try {
      try {
        var method = Bukkit.getServer().getClass().getMethod("syncCommands");
        method.invoke(Bukkit.getServer());
        plugin.debug("Synced commands via syncCommands()");
      } catch (NoSuchMethodException ignored) {
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
        } catch (Exception ignored) {
        }
      }

      if (updated > 0) {
        plugin.debug("Updated commands for " + updated + " player(s)");
      }
    } catch (Throwable t) {
      String msg = t.getCause() != null ? t.getCause().getMessage() : t.getMessage();
      plugin.debug(
          "Platform command sync note: " + (msg != null ? msg : t.getClass().getSimpleName()));
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
        // Only our own leftovers count; another plugin may legitimately own the same label
        if (knownCommands.get(key) instanceof PluginCommand pluginCommand
            && pluginCommand.getPlugin() == plugin) {
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

    // Only touch commands JavaSkript registered itself. Another plugin may own the same label
    // (Essentials' /heal, vanilla commands) and must never be removed from here.
    Command ours = registeredCommands.remove(commandName.toLowerCase());
    if (ours == null) {
      return false;
    }

    try {
      CommandMap commandMap = getCommandMap();
      if (commandMap == null) {
        return false;
      }

      try {
        ours.unregister(commandMap);
      } catch (Exception ignored) {
      }

      // Drops the plain label, the prefixed label and every alias that still points at ours
      Map<String, Command> knownCommands = getKnownCommandsMap(commandMap);
      if (knownCommands != null) {
        knownCommands.values().removeIf(command -> command == ours);
      }
      return true;

    } catch (Exception e) {
      return false;
    }
  }

  /** The command JavaSkript registered under this name, or null. */
  public Command getCommand(String commandName) {
    return commandName == null ? null : registeredCommands.get(commandName.toLowerCase());
  }

  /**
   * Unregister a command only if it is still the one the caller registered. A script that unloads
   * after another script replaced its command name must not remove the replacement.
   */
  public synchronized boolean unregisterCommand(String commandName, Command expected) {
    if (expected == null || getCommand(commandName) != expected) {
      return false;
    }
    return unregisterCommand(commandName);
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
