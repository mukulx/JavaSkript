package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptInstance;
import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class InfoSubCommand extends SubCommand {

  public InfoSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript info <script>").color(NamedTextColor.RED));
      return;
    }

    String scriptKey = joinArgs(args, 1);
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    ScriptInstance instance = plugin.getScriptManager().getScript(scriptKey);

    if (instance == null) {
      // Check if disabled
      if (plugin.getScriptManager().isScriptDisabled(scriptKey)) {
        sender.sendMessage(
            Component.text("Script is disabled: " + scriptKey).color(NamedTextColor.RED));
        sender.sendMessage(
            Component.text("Use /js enable " + scriptKey + " to enable it")
                .color(NamedTextColor.YELLOW));
      } else {
        sender.sendMessage(
            Component.text("Script not loaded: " + scriptKey).color(NamedTextColor.RED));
      }
      return;
    }

    sender.sendMessage(Component.text("Script Information:").color(NamedTextColor.GOLD));
    sender.sendMessage(
        Component.text("  Name: " + instance.getName()).color(NamedTextColor.YELLOW));
    sender.sendMessage(Component.text("  Path: " + scriptKey).color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  Class: " + instance.getScriptClass().getName())
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  File: " + instance.getScriptFile().getAbsolutePath())
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(Component.text("  Status: Enabled").color(NamedTextColor.GREEN));
    sender.sendMessage(
        Component.text("  Compatibility: " + instance.getFoliaCompatibilityStatus())
            .color(NamedTextColor.YELLOW));

    Class<?> clazz = instance.getScriptClass();
    List<String> interfaces =
        Arrays.stream(clazz.getInterfaces()).map(Class::getSimpleName).collect(Collectors.toList());

    if (!interfaces.isEmpty()) {
      sender.sendMessage(
          Component.text("  Implements: " + String.join(", ", interfaces))
              .color(NamedTextColor.YELLOW));
    }
  }

  @Override
  public List<String> complete(CommandSender sender, String[] args) {
    return suggestScripts(args, false, plugin.getScriptManager().getAllScriptKeys());
  }
}
