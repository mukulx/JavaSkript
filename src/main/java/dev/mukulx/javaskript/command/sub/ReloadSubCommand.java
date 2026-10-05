package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ReloadSubCommand extends SubCommand {

  public ReloadSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /js reload <script|folder|all>").color(NamedTextColor.RED));
      sender.sendMessage(
          Component.text("  /js reload <script> - Reload a specific script")
              .color(NamedTextColor.GRAY));
      sender.sendMessage(
          Component.text("  /js reload <folder> - Reload all scripts in a folder")
              .color(NamedTextColor.GRAY));
      sender.sendMessage(
          Component.text("  /js reload all - Reload all scripts").color(NamedTextColor.GRAY));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    if (rawTarget.equalsIgnoreCase("all")) {
      // Reload all scripts
      sender.sendMessage(Component.text("Reloading all scripts...").color(NamedTextColor.YELLOW));

      try {
        plugin.getScriptManager().reloadAllScripts();
        int count = plugin.getScriptManager().getLoadedScripts().size();
        sender.sendMessage(
            Component.text("Successfully reloaded " + count + " script(s)!")
                .color(NamedTextColor.GREEN));
      } catch (Exception e) {
        sender.sendMessage(
            Component.text("Error reloading scripts: " + e.getMessage()).color(NamedTextColor.RED));
        plugin.getLogger().severe("Error reloading scripts: " + e.getMessage());
      }
    } else {
      // Check if target is a directory
      if (plugin.getScriptManager().isDirectory(rawTarget)) {
        File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
        sender.sendMessage(
            Component.text("Reloading folder: " + rawTarget).color(NamedTextColor.YELLOW));
        int count = plugin.getScriptManager().reloadDirectory(dir);
        sender.sendMessage(
            Component.text("Successfully reloaded " + count + " script(s) in folder: " + rawTarget)
                .color(NamedTextColor.GREEN));
        return;
      }

      // Reload specific script
      String scriptKey = rawTarget;
      if (!scriptKey.endsWith(".java")) {
        scriptKey += ".java";
      }

      sender.sendMessage(
          Component.text("Reloading script: " + scriptKey).color(NamedTextColor.YELLOW));

      File scriptFile = plugin.getScriptManager().resolveScriptFile(scriptKey);

      if (!scriptFile.exists()) {
        sender.sendMessage(
            Component.text("Script not found: " + scriptKey).color(NamedTextColor.RED));
        return;
      }

      final String key = scriptKey;
      plugin
          .getScriptManager()
          .loadScriptAsync(
              scriptFile, result -> sendLoadResult(sender, key, "reloaded", "reload", result));
    }
  }

  @Override
  public List<String> complete(CommandSender sender, String[] args) {
    return suggestScripts(args, true, plugin.getScriptManager().getLoadedScripts().keySet());
  }
}
