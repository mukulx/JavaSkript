package dev.mukulx.javaskript.api.command;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/** Builder for subcommands and nested arguments. */
public class SubcommandBuilder {

  protected final String name;
  protected String description = "";
  protected String permission = null;
  protected String permissionMessage = "§cYou do not have permission to execute this command.";
  protected final List<CommandArgument<?>> arguments = new ArrayList<>();
  protected final Map<String, SubcommandBuilder> subcommands = new LinkedHashMap<>();
  protected CommandCallback callback = null;
  protected boolean playerOnly = false;
  protected boolean consoleOnly = false;

  public SubcommandBuilder(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public SubcommandBuilder description(String description) {
    this.description = description;
    return this;
  }

  public SubcommandBuilder permission(String permission) {
    this.permission = permission;
    return this;
  }

  public SubcommandBuilder permissionMessage(String permissionMessage) {
    this.permissionMessage = permissionMessage;
    return this;
  }

  public SubcommandBuilder argument(CommandArgument<?> argument) {
    this.arguments.add(argument);
    return this;
  }

  public SubcommandBuilder optionalArgument(CommandArgument<?> argument) {
    if (argument.isOptional()) {
      this.arguments.add(argument);
    } else {
      this.arguments.add(new OptionalArgumentWrapper<>(argument));
    }
    return this;
  }

  public SubcommandBuilder subcommand(String name, Consumer<SubcommandBuilder> consumer) {
    SubcommandBuilder sub = new SubcommandBuilder(name.toLowerCase());
    consumer.accept(sub);
    this.subcommands.put(name.toLowerCase(), sub);
    return this;
  }

  public SubcommandBuilder subcommand(SubcommandBuilder sub) {
    this.subcommands.put(sub.getName().toLowerCase(), sub);
    return this;
  }

  public SubcommandBuilder executes(CommandCallback callback) {
    this.callback = callback;
    return this;
  }

  public SubcommandBuilder executesPlayer(PlayerCallback callback) {
    this.playerOnly = true;
    this.callback =
        (sender, ctx) -> {
          if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("§cOnly players can execute this command."));
            return;
          }
          callback.execute(p, ctx);
        };
    return this;
  }

  public SubcommandBuilder executesConsole(ConsoleCallback callback) {
    this.consoleOnly = true;
    this.callback =
        (sender, ctx) -> {
          if (!(sender instanceof ConsoleCommandSender c)) {
            sender.sendMessage(Component.text("§cThis command can only be executed from console."));
            return;
          }
          callback.execute(c, ctx);
        };
    return this;
  }

  // ==========================================
  // Execution & Parsing
  // ==========================================

  public boolean execute(CommandSender sender, String label, String[] args, int offset) {
    if (sender == null) return false;
    if (args == null) args = new String[0];

    if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
      sender.sendMessage(Component.text(permissionMessage));
      return true;
    }

    if (playerOnly && !(sender instanceof Player)) {
      sender.sendMessage(Component.text("§cOnly players can execute this command."));
      return true;
    }

    if (consoleOnly && !(sender instanceof ConsoleCommandSender)) {
      sender.sendMessage(Component.text("§cThis command can only be executed from console."));
      return true;
    }

    int remaining = args.length - offset;

    // Check if next token matches a nested subcommand
    if (remaining > 0) {
      String subToken = args[offset].toLowerCase();
      SubcommandBuilder sub = subcommands.get(subToken);
      if (sub != null) {
        return sub.execute(sender, label, args, offset + 1);
      }
    }

    // Parse arguments
    Map<String, Object> parsed = new HashMap<>();
    int argIndex = 0;

    for (int i = offset; i < args.length; i++) {
      if (argIndex >= arguments.size()) {
        break;
      }
      CommandArgument<?> currentArg = arguments.get(argIndex);

      if (currentArg.isGreedy()) {
        // Collect all remaining args into one string
        StringBuilder greedy = new StringBuilder();
        for (int g = i; g < args.length; g++) {
          if (greedy.length() > 0) greedy.append(" ");
          greedy.append(args[g]);
        }
        try {
          Object val = currentArg.parse(sender, greedy.toString());
          parsed.put(currentArg.getName().toLowerCase(), val);
        } catch (CommandArgumentException e) {
          sender.sendMessage(Component.text("§c" + e.getMessage()));
          return true;
        }
        argIndex++;
        break; // greedy consumes everything
      } else {
        try {
          Object val = currentArg.parse(sender, args[i]);
          parsed.put(currentArg.getName().toLowerCase(), val);
        } catch (CommandArgumentException e) {
          sender.sendMessage(Component.text("§c" + e.getMessage()));
          return true;
        }
        argIndex++;
      }
    }

    // Validate required arguments
    for (int a = argIndex; a < arguments.size(); a++) {
      CommandArgument<?> missing = arguments.get(a);
      if (!missing.isOptional()) {
        sender.sendMessage(
            Component.text("§cMissing required argument: <" + missing.getName() + ">"));
        sendUsage(sender, label);
        return true;
      }
    }

    // Execute callback
    if (callback != null) {
      CommandContext ctx = new CommandContext(sender, label, args, parsed);
      try {
        callback.execute(sender, ctx);
      } catch (Exception e) {
        sender.sendMessage(
            Component.text("§cAn error occurred executing this command: " + e.getMessage()));
        e.printStackTrace();
      }
      return true;
    }

    sendUsage(sender, label);
    return true;
  }

  // ==========================================
  // Tab Completion
  // ==========================================

  public List<String> tabComplete(CommandSender sender, String label, String[] args, int offset) {
    if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
      return List.of();
    }

    int remaining = args.length - offset;

    // Completing subcommands
    if (remaining == 1) {
      String partial = args[offset].toLowerCase();
      List<String> list = new ArrayList<>();

      // Add subcommands matching partial
      for (Map.Entry<String, SubcommandBuilder> entry : subcommands.entrySet()) {
        if (entry.getValue().permission == null
            || sender.hasPermission(entry.getValue().permission)) {
          if (entry.getKey().startsWith(partial)) {
            list.add(entry.getKey());
          }
        }
      }

      // If there are arguments at index 0, also tab complete them
      if (!arguments.isEmpty()) {
        list.addAll(arguments.get(0).tabComplete(sender, args[offset]));
      }

      return list.stream().distinct().sorted().collect(Collectors.toList());
    }

    // Nested subcommand completion
    if (remaining > 1) {
      String subToken = args[offset].toLowerCase();
      SubcommandBuilder sub = subcommands.get(subToken);
      if (sub != null) {
        return sub.tabComplete(sender, label, args, offset + 1);
      }

      // Argument completions
      int argIdx = remaining - 1;
      if (argIdx < arguments.size()) {
        CommandArgument<?> arg = arguments.get(argIdx);
        return arg.tabComplete(sender, args[args.length - 1]);
      }
    }

    return List.of();
  }

  protected void sendUsage(CommandSender sender, String label) {
    if (!subcommands.isEmpty()) {
      sender.sendMessage(
          Component.text("§eAvailable subcommands: §f" + String.join(", ", subcommands.keySet())));
    }
  }

  // Wrapper for optional arguments
  private static class OptionalArgumentWrapper<T> implements CommandArgument<T> {
    private final CommandArgument<T> delegate;

    public OptionalArgumentWrapper(CommandArgument<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public boolean isOptional() {
      return true;
    }

    @Override
    public boolean isGreedy() {
      return delegate.isGreedy();
    }

    @Override
    public T parse(CommandSender sender, String input) throws CommandArgumentException {
      return delegate.parse(sender, input);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
      return delegate.tabComplete(sender, input);
    }
  }
}
