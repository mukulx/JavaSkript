package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class DisableSubCommand extends SubCommand {

  public DisableSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript disable <script|folder|all>")
              .color(NamedTextColor.RED));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    if (rawTarget.equalsIgnoreCase("all")) {
      sender.sendMessage(Component.text("Disabling all scripts...").color(NamedTextColor.YELLOW));
      int count = plugin.getScriptManager().disableAllScripts();
      sender.sendMessage(
          Component.text("Successfully disabled " + count + " script(s)!")
              .color(NamedTextColor.GREEN));
      return;
    }

    // Check if target is a directory
    if (plugin.getScriptManager().isDirectory(rawTarget)) {
      File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
      sender.sendMessage(
          Component.text("Disabling folder: " + rawTarget).color(NamedTextColor.YELLOW));
      boolean success = plugin.getScriptManager().disableDirectory(dir);
      if (success) {
        sender.sendMessage(
            Component.text("Successfully disabled folder: " + rawTarget)
                .color(NamedTextColor.GREEN));
        sender.sendMessage(
            Component.text("Folder renamed with '-' prefix; scripts unloaded")
                .color(NamedTextColor.GRAY));
      } else {
        sender.sendMessage(
            Component.text("Failed to disable folder (already disabled or error): " + rawTarget)
                .color(NamedTextColor.RED));
      }
      return;
    }

    String scriptKey = rawTarget;
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    // Check if already disabled
    if (plugin.getScriptManager().isScriptDisabled(scriptKey)) {
      sender.sendMessage(
          Component.text("Script is already disabled: " + scriptKey).color(NamedTextColor.YELLOW));
      return;
    }

    sender.sendMessage(
        Component.text("Disabling script: " + scriptKey).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().disableScript(scriptKey);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully disabled: " + scriptKey).color(NamedTextColor.GREEN));
      sender.sendMessage(
          Component.text("Script will not load until enabled again").color(NamedTextColor.GRAY));
    } else {
      sender.sendMessage(
          Component.text("Failed to disable: " + scriptKey).color(NamedTextColor.RED));
    }
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
