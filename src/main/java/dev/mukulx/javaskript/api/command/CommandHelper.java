package dev.mukulx.javaskript.api.command;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

/** High-level Command API for registering fluent commands with automatic unregistration. */
public class CommandHelper {

  private final JavaSkriptPlugin plugin;
  private final String scriptKey;
  private final Set<String> registeredCommandNames = ConcurrentHashMap.newKeySet();

  public CommandHelper(JavaSkriptPlugin plugin, String scriptKey) {
    this.plugin = plugin;
    this.scriptKey = scriptKey != null ? scriptKey : "unknown";
  }

  /**
   * Start building a new fluent command.
   *
   * @param name Primary command name (without slash, e.g. "warp")
   * @return A new CommandBuilder
   */
  public CommandBuilder create(String name) {
    return new CommandBuilder(name, this);
  }

  /**
   * Register a built command. Called automatically by {@link CommandBuilder#register()}.
   *
   * @param builder The configured command builder
   * @return true if successfully registered with the server
   */
  public boolean register(CommandBuilder builder) {
    if (builder == null || builder.getName() == null) {
      return false;
    }

    String commandName = builder.getName().toLowerCase();

    // Wrap executor with profiler tracking
    CommandExecutor originalExecutor = builder;
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

    TabCompleter tabCompleter = builder;

    String[] aliasArray = builder.getAliases().toArray(new String[0]);

    boolean success =
        plugin
            .getCommandRegistry()
            .registerCommand(
                commandName,
                profiledExecutor,
                tabCompleter,
                builder.getDescription(),
                builder.getUsage(),
                builder.getPermission(),
                builder.getPermissionMessage(),
                builder.getAliases());

    if (success) {
      registeredCommandNames.add(commandName);
      for (String alias : builder.getAliases()) {
        registeredCommandNames.add(alias.toLowerCase());
      }
      plugin.debug("[" + scriptKey + "] Registered fluent command: /" + commandName);
    } else {
      plugin.getLogger().warning("[" + scriptKey + "] Failed to register command: /" + commandName);
    }

    return success;
  }

  /** Unregister a specific command by name. */
  public boolean unregister(String commandName) {
    if (commandName == null) return false;
    String lower = commandName.toLowerCase();
    boolean removed = plugin.getCommandRegistry().unregisterCommand(lower);
    registeredCommandNames.remove(lower);
    return removed;
  }

  /** Unregister all commands registered by this script. Automatically called on unload. */
  public void unregisterAll() {
    for (String cmd : registeredCommandNames) {
      try {
        plugin.getCommandRegistry().unregisterCommand(cmd);
      } catch (Exception e) {
        plugin
            .getLogger()
            .log(Level.WARNING, "[" + scriptKey + "] Error unregistering command /" + cmd, e);
      }
    }
    registeredCommandNames.clear();
  }

  /** Get all commands currently registered by this script helper. */
  public Set<String> getRegisteredCommands() {
    return Collections.unmodifiableSet(registeredCommandNames);
  }
}
