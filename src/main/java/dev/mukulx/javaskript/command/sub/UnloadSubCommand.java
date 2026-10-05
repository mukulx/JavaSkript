package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class UnloadSubCommand extends SubCommand {

  public UnloadSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript unload <script|folder|all>")
              .color(NamedTextColor.RED));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    if (rawTarget.equalsIgnoreCase("all")) {
      int count = plugin.getScriptManager().getLoadedScripts().size();
      sender.sendMessage(
          Component.text("Unloading all " + count + " script(s)...").color(NamedTextColor.YELLOW));
      plugin.getScriptManager().unloadAllScripts();
      sender.sendMessage(
          Component.text("Successfully unloaded " + count + " script(s)!")
              .color(NamedTextColor.GREEN));
      return;
    }

    // Check if target is a directory
    if (plugin.getScriptManager().isDirectory(rawTarget)) {
      sender.sendMessage(
          Component.text("Unloading folder: " + rawTarget).color(NamedTextColor.YELLOW));
      int count = plugin.getScriptManager().unloadDirectory(rawTarget);
      sender.sendMessage(
          Component.text("Successfully unloaded " + count + " script(s) in folder: " + rawTarget)
              .color(NamedTextColor.GREEN));
      return;
    }

    String scriptKey = rawTarget;
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    sender.sendMessage(
        Component.text("Unloading script: " + scriptKey).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().unloadScript(scriptKey);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully unloaded: " + scriptKey).color(NamedTextColor.GREEN));
    } else {
      sender.sendMessage(
          Component.text("Script not loaded: " + scriptKey).color(NamedTextColor.RED));
    }
  }

  @Override
  public List<String> complete(CommandSender sender, String[] args) {
    return suggestScripts(args, true, plugin.getScriptManager().getLoadedScripts().keySet());
  }
}
