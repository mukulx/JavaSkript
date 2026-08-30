package dev.mukulx.javaskript.api.command;

import java.util.Collections;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/** Holds the execution context of a command, including sender and parsed arguments. */
public class CommandContext {

  private final CommandSender sender;
  private final String label;
  private final String[] rawArgs;
  private final Map<String, Object> arguments;

  public CommandContext(
      CommandSender sender, String label, String[] rawArgs, Map<String, Object> arguments) {
    this.sender = sender;
    this.label = label;
    this.rawArgs = rawArgs;
    this.arguments = arguments != null ? arguments : Collections.emptyMap();
  }

  public CommandSender sender() {
    return sender;
  }

  public String label() {
    return label;
  }

  public String[] rawArgs() {
    return rawArgs;
  }

  public boolean isPlayer() {
    return sender instanceof Player;
  }

  public boolean isConsole() {
    return sender instanceof ConsoleCommandSender;
  }

  public Player player() {
    if (sender instanceof Player p) {
      return p;
    }
    return null;
  }

  public ConsoleCommandSender console() {
    if (sender instanceof ConsoleCommandSender c) {
      return c;
    }
    return null;
  }

  public boolean has(String name) {
    return arguments.containsKey(name.toLowerCase());
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String name) {
    return (T) arguments.get(name.toLowerCase());
  }

  @SuppressWarnings("unchecked")
  public <T> T getOrDefault(String name, T defaultValue) {
    Object val = arguments.get(name.toLowerCase());
    if (val == null) {
      return defaultValue;
    }
    return (T) val;
  }

  public String getString(String name) {
    Object val = arguments.get(name.toLowerCase());
    return val != null ? val.toString() : null;
  }

  public int getInt(String name) {
    Object val = arguments.get(name.toLowerCase());
    if (val instanceof Number n) {
      return n.intValue();
    }
    return 0;
  }

  public double getDouble(String name) {
    Object val = arguments.get(name.toLowerCase());
    if (val instanceof Number n) {
      return n.doubleValue();
    }
    return 0.0;
  }

  public boolean getBoolean(String name) {
    Object val = arguments.get(name.toLowerCase());
    if (val instanceof Boolean b) {
      return b;
    }
    return false;
  }

  public Player getPlayer(String name) {
    Object val = arguments.get(name.toLowerCase());
    if (val instanceof Player p) {
      return p;
    }
    return null;
  }

  public Map<String, Object> allArguments() {
    return Collections.unmodifiableMap(arguments);
  }

  // ==========================================
  // Reply Helpers
  // ==========================================

  public void reply(String message) {
    if (message == null || sender == null) return;
    sender.sendMessage(Component.text(message.replace('&', '§')));
  }

  public void reply(Component component) {
    if (component == null || sender == null) return;
    sender.sendMessage(component);
  }

  public void replySuccess(String message) {
    reply("§a✔ " + message);
  }

  public void replyError(String message) {
    reply("§c✖ " + message);
  }
}
