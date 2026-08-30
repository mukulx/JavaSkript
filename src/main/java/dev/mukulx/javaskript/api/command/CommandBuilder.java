package dev.mukulx.javaskript.api.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/** Fluent builder for root commands. */
public class CommandBuilder extends SubcommandBuilder implements CommandExecutor, TabCompleter {

  private final CommandHelper helper;
  private final List<String> aliases = new ArrayList<>();
  private String usage = null;
  private boolean registered = false;

  public CommandBuilder(String name, CommandHelper helper) {
    super(name.toLowerCase());
    this.helper = helper;
  }

  public CommandBuilder aliases(String... aliases) {
    if (aliases != null) {
      this.aliases.addAll(Arrays.asList(aliases));
    }
    return this;
  }

  public CommandBuilder usage(String usage) {
    this.usage = usage;
    return this;
  }

  @Override
  public CommandBuilder description(String description) {
    super.description(description);
    return this;
  }

  @Override
  public CommandBuilder permission(String permission) {
    super.permission(permission);
    return this;
  }

  @Override
  public CommandBuilder permissionMessage(String permissionMessage) {
    super.permissionMessage(permissionMessage);
    return this;
  }

  @Override
  public CommandBuilder argument(CommandArgument<?> argument) {
    super.argument(argument);
    return this;
  }

  @Override
  public CommandBuilder optionalArgument(CommandArgument<?> argument) {
    super.optionalArgument(argument);
    return this;
  }

  @Override
  public CommandBuilder subcommand(String name, Consumer<SubcommandBuilder> consumer) {
    super.subcommand(name, consumer);
    return this;
  }

  @Override
  public CommandBuilder subcommand(SubcommandBuilder sub) {
    super.subcommand(sub);
    return this;
  }

  @Override
  public CommandBuilder executes(CommandCallback callback) {
    super.executes(callback);
    return this;
  }

  @Override
  public CommandBuilder executesPlayer(PlayerCallback callback) {
    super.executesPlayer(callback);
    return this;
  }

  @Override
  public CommandBuilder executesConsole(ConsoleCallback callback) {
    super.executesConsole(callback);
    return this;
  }

  public List<String> getAliases() {
    return aliases;
  }

  public String getUsage() {
    return usage;
  }

  public String getDescription() {
    return description;
  }

  public String getPermission() {
    return permission;
  }

  public String getPermissionMessage() {
    return permissionMessage;
  }

  public boolean isRegistered() {
    return registered;
  }

  /** Register this command dynamically with Bukkit / Paper. */
  public CommandBuilder register() {
    if (helper != null) {
      this.registered = helper.register(this);
    }
    return this;
  }

  // ==========================================
  // Bukkit CommandExecutor & TabCompleter
  // ==========================================

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    return execute(sender, label, args, 0);
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    return tabComplete(sender, label, args, 0);
  }

  @Override
  protected void sendUsage(CommandSender sender, String label) {
    if (usage != null && !usage.isEmpty()) {
      sender.sendMessage(Component.text("§cUsage: " + usage.replace("<label>", label)));
    } else if (!subcommands.isEmpty()) {
      sender.sendMessage(
          Component.text(
              "§eUsage: §f/" + label + " <" + String.join("|", subcommands.keySet()) + ">"));
    } else if (!arguments.isEmpty()) {
      StringBuilder sb = new StringBuilder("§cUsage: /").append(label);
      for (CommandArgument<?> arg : arguments) {
        sb.append(" ");
        if (arg.isOptional()) {
          sb.append("[").append(arg.getName()).append("]");
        } else {
          sb.append("<").append(arg.getName()).append(">");
        }
      }
      sender.sendMessage(Component.text(sb.toString()));
    }
  }
}
