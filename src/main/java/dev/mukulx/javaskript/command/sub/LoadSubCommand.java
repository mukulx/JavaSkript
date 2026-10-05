package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class LoadSubCommand extends SubCommand {

  public LoadSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript load <script|folder|all>").color(NamedTextColor.RED));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    if (rawTarget.equalsIgnoreCase("all")) {
      sender.sendMessage(Component.text("Loading all scripts...").color(NamedTextColor.YELLOW));
      plugin.getScriptManager().loadAllScripts();
      int count = plugin.getScriptManager().getLoadedScripts().size();
      sender.sendMessage(
          Component.text("Successfully loaded " + count + " script(s)!")
              .color(NamedTextColor.GREEN));
      return;
    }

    // Check if target is a directory
    if (plugin.getScriptManager().isDirectory(rawTarget)) {
      File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
      sender.sendMessage(
          Component.text("Loading folder: " + rawTarget).color(NamedTextColor.YELLOW));
      int count = plugin.getScriptManager().loadDirectory(dir);
      sender.sendMessage(
          Component.text("Successfully loaded " + count + " script(s) from folder: " + rawTarget)
              .color(NamedTextColor.GREEN));
      return;
    }

    String scriptKey = rawTarget;
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    // Check if already loaded
    if (plugin.getScriptManager().getScript(scriptKey) != null) {
      sender.sendMessage(
          Component.text("Script already loaded: " + scriptKey).color(NamedTextColor.RED));
      sender.sendMessage(
          Component.text("Use /js reload " + scriptKey + " to reload it")
              .color(NamedTextColor.YELLOW));
      return;
    }

    File scriptFile = plugin.getScriptManager().resolveScriptFile(scriptKey);

    if (!scriptFile.exists()) {
      sender.sendMessage(
          Component.text("Script not found: " + scriptKey).color(NamedTextColor.RED));
      return;
    }

    sender.sendMessage(Component.text("Loading script: " + scriptKey).color(NamedTextColor.YELLOW));

    final String key = scriptKey;
    plugin
        .getScriptManager()
        .loadScriptAsync(
            scriptFile, result -> sendLoadResult(sender, key, "loaded", "load", result));
  }

  @Override
  public List<String> complete(CommandSender sender, String[] args) {
    return suggestScripts(
        args,
        true,
        plugin.getScriptManager().getAllScriptKeys().stream()
            .filter(key -> !plugin.getScriptManager().isScriptDisabled(key))
            .toList());
  }
}
