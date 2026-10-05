package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.io.File;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class RestartSubCommand extends SubCommand {

  public RestartSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /js restart <script|folder|all>").color(NamedTextColor.RED));
      sender.sendMessage(
          Component.text("  /js restart <script> - Restart a specific script")
              .color(NamedTextColor.GRAY));
      sender.sendMessage(
          Component.text("  /js restart <folder> - Restart all scripts in a folder")
              .color(NamedTextColor.GRAY));
      sender.sendMessage(
          Component.text("  /js restart all - Restart all scripts").color(NamedTextColor.GRAY));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    if (rawTarget.equalsIgnoreCase("all")) {
      // Restart all scripts
      sender.sendMessage(Component.text("Restarting all scripts...").color(NamedTextColor.YELLOW));

      try {
        plugin.getScriptManager().unloadAllScripts();
        sender.sendMessage(Component.text("All scripts unloaded").color(NamedTextColor.GRAY));

        // Small delay to ensure cleanup (Folia-safe via global region scheduler)
        Runnable loadTask =
            () -> {
              plugin.getScriptManager().loadAllScripts();
              int count = plugin.getScriptManager().getLoadedScripts().size();
              sender.sendMessage(
                  Component.text("Successfully restarted " + count + " script(s)!")
                      .color(NamedTextColor.GREEN));
            };

        if (ServerUtil.isFolia()) {
          plugin
              .getServer()
              .getGlobalRegionScheduler()
              .runDelayed(plugin, task -> loadTask.run(), 20L);
        } else {
          plugin.getServer().getScheduler().runTaskLater(plugin, loadTask, 20L);
        }
      } catch (Exception e) {
        sender.sendMessage(
            Component.text("Error restarting scripts: " + e.getMessage())
                .color(NamedTextColor.RED));
        plugin.getLogger().severe("Error restarting scripts: " + e.getMessage());
      }
    } else if (plugin.getScriptManager().isDirectory(rawTarget)) {
      File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
      int unloadedCount = plugin.getScriptManager().unloadDirectory(rawTarget);
      sender.sendMessage(
          Component.text(
                  "Restarting folder: "
                      + rawTarget
                      + " ("
                      + unloadedCount
                      + " script(s) unloaded)...")
              .color(NamedTextColor.YELLOW));

      Runnable loadTask =
          () -> {
            int count = plugin.getScriptManager().loadDirectory(dir);
            sender.sendMessage(
                Component.text(
                        "Successfully restarted folder: "
                            + rawTarget
                            + " ("
                            + count
                            + " script(s) loaded)!")
                    .color(NamedTextColor.GREEN));
          };

      if (ServerUtil.isFolia()) {
        plugin
            .getServer()
            .getGlobalRegionScheduler()
            .runDelayed(plugin, task -> loadTask.run(), 20L);
      } else {
        plugin.getServer().getScheduler().runTaskLater(plugin, loadTask, 20L);
      }
    } else {
      // Restart specific script
      String scriptKey = rawTarget;
      if (!scriptKey.endsWith(".java")) {
        scriptKey += ".java";
      }

      sender.sendMessage(
          Component.text("Restarting script: " + scriptKey).color(NamedTextColor.YELLOW));

      File scriptFile = plugin.getScriptManager().resolveScriptFile(scriptKey);

      if (!scriptFile.exists()) {
        sender.sendMessage(
            Component.text("Script not found: " + scriptKey).color(NamedTextColor.RED));
        return;
      }

      // Unload
      plugin.getScriptManager().unloadScript(scriptKey);
      sender.sendMessage(Component.text("Script unloaded").color(NamedTextColor.GRAY));

      // Reload after delay (Folia-safe)
      String finalScriptKey = scriptKey;
      Runnable reloadTask =
          () -> {
            boolean success = plugin.getScriptManager().loadScript(scriptFile);

            if (success) {
              sender.sendMessage(
                  Component.text("Successfully restarted: " + finalScriptKey)
                      .color(NamedTextColor.GREEN));
            } else {
              sender.sendMessage(
                  Component.text("Failed to restart: " + finalScriptKey).color(NamedTextColor.RED));
            }
          };

      if (ServerUtil.isFolia()) {
        plugin
            .getServer()
            .getGlobalRegionScheduler()
            .runDelayed(plugin, task -> reloadTask.run(), 20L);
      } else {
        plugin.getServer().getScheduler().runTaskLater(plugin, reloadTask, 20L);
      }
    }
  }

  @Override
  public List<String> complete(CommandSender sender, String[] args) {
    return suggestScripts(args, true, plugin.getScriptManager().getLoadedScripts().keySet());
  }
}
