package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class EnableSubCommand extends SubCommand {

  public EnableSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript enable <script|folder|all>")
              .color(NamedTextColor.RED));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    if (rawTarget.equalsIgnoreCase("all")) {
      sender.sendMessage(Component.text("Enabling all scripts...").color(NamedTextColor.YELLOW));
      int count = plugin.getScriptManager().enableAllScripts();
      sender.sendMessage(
          Component.text("Successfully enabled all scripts (" + count + " script(s) loaded)!")
              .color(NamedTextColor.GREEN));
      return;
    }

    // Check if target is a directory
    if (plugin.getScriptManager().isDirectory(rawTarget)) {
      File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
      sender.sendMessage(
          Component.text("Enabling folder: " + rawTarget).color(NamedTextColor.YELLOW));
      int count = plugin.getScriptManager().enableDirectory(dir);
      if (count >= 0) {
        sender.sendMessage(
            Component.text(
                    "Successfully enabled folder: "
                        + rawTarget
                        + " ("
                        + count
                        + " script(s) loaded)")
                .color(NamedTextColor.GREEN));
      } else {
        sender.sendMessage(
            Component.text("Failed to enable folder: " + rawTarget).color(NamedTextColor.RED));
      }
      return;
    }

    String scriptKey = rawTarget;
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    // Check if already enabled
    if (!plugin.getScriptManager().isScriptDisabled(scriptKey)) {
      sender.sendMessage(
          Component.text("Script is already enabled: " + scriptKey).color(NamedTextColor.YELLOW));
      return;
    }

    sender.sendMessage(
        Component.text("Enabling script: " + scriptKey).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().enableScript(scriptKey);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully enabled: " + scriptKey).color(NamedTextColor.GREEN));
    } else {
      sender.sendMessage(
          Component.text("Failed to enable: " + scriptKey).color(NamedTextColor.RED));
    }
  }

  @Override
  public List<String> complete(CommandSender sender, String[] args) {
    return suggestScripts(args, true, plugin.getScriptManager().getDisabledScripts());
  }
}
