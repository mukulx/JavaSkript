package dev.mukulx.javaskript.command;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.command.sub.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/** Routes {@code /js <subcommand>} to its handler in {@link dev.mukulx.javaskript.command.sub}. */
public class JavaSkriptCommand implements CommandExecutor, TabCompleter {

  private final JavaSkriptPlugin plugin;
  private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
  private final SubCommand help;

  public JavaSkriptCommand(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.help = new HelpSubCommand(plugin);
    subCommands.put("reload", new ReloadSubCommand(plugin));
    subCommands.put("restart", new RestartSubCommand(plugin));
    subCommands.put("configreload", new ConfigReloadSubCommand(plugin));
    subCommands.put("list", new ListSubCommand(plugin));
    subCommands.put("load", new LoadSubCommand(plugin));
    subCommands.put("unload", new UnloadSubCommand(plugin));
    subCommands.put("enable", new EnableSubCommand(plugin));
    subCommands.put("disable", new DisableSubCommand(plugin));
    subCommands.put("info", new InfoSubCommand(plugin));
    subCommands.put("profile", new ProfileSubCommand(plugin));
    subCommands.put("benchmark", new BenchmarkSubCommand(plugin));
    subCommands.put("timings", new TimingsSubCommand(plugin));
    subCommands.put("debug", new DebugSubCommand(plugin));
    subCommands.put("addons", new AddonsSubCommand(plugin));
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("javaskript.admin")) {
      sender.sendMessage(
          Component.text("You don't have permission to use this command!")
              .color(NamedTextColor.RED));
      return true;
    }

    if (args.length == 0) {
      help.execute(sender, args);
      return true;
    }

    try {
      subCommands.getOrDefault(args[0].toLowerCase(), help).execute(sender, args);
    } catch (Throwable t) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "Unexpected error executing /js " + String.join(" ", args), t);
      sender.sendMessage(
          Component.text("§cAn error occurred executing this command: " + t.getMessage()));
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("javaskript.admin")) {
      return List.of();
    }

    if (args.length == 1) {
      return subCommands.keySet().stream()
          .filter(s -> s.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }

    SubCommand sub = subCommands.get(args[0].toLowerCase());
    return sub == null ? List.of() : sub.complete(sender, args);
  }
}
