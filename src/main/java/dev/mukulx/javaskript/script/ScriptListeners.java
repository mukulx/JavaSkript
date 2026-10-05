package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.*;
import dev.mukulx.javaskript.script.loader.ScriptClassLoader;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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

/** Registers a script's event handlers and command with profiling, and removes them again. */
final class ScriptListeners {

  private final JavaSkriptPlugin plugin;
  private final File scriptFile;
  private final Class<?> scriptClass;
  private final ScriptClassLoader classLoader;
  private final Map<String, Command> registeredCommands = new LinkedHashMap<>();

  ScriptListeners(
      JavaSkriptPlugin plugin,
      File scriptFile,
      Class<?> scriptClass,
      ScriptClassLoader classLoader) {
    this.plugin = plugin;
    this.scriptFile = scriptFile;
    this.scriptClass = scriptClass;
    this.classLoader = classLoader;
  }

  void registerEvents(Listener listener) {
    String scriptKey = plugin.getScriptManager().getScriptKey(scriptFile);
    int registeredCount = 0;

    // Walk from the subclass up and keep the first method per signature. An overridden handler
    // must register once: invoking the superclass Method dispatches to the override anyway.
    Map<String, Method> bySignature = new LinkedHashMap<>();
    for (Class<?> clazz = listener.getClass();
        clazz != null && clazz != Object.class;
        clazz = clazz.getSuperclass()) {
      for (Method m : clazz.getDeclaredMethods()) {
        if (m.isBridge() || m.isSynthetic()) continue;
        String signature = m.getName() + java.util.Arrays.toString(m.getParameterTypes());
        bySignature.putIfAbsent(signature, m);
      }
    }
    java.util.Collection<Method> methods = bySignature.values();

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

  void registerCommand(Object instance) {
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

  /** Unregister the script instance itself and every other listener the script registered. */
  void unregisterListeners(Object instance) {
    if (instance instanceof Listener listener) {
      HandlerList.unregisterAll(listener);
    }
    unregisterScriptListeners();
  }

  /** Unregister the commands the script class registered by implementing CommandExecutor. */
  void unregisterCommands() {
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
}
