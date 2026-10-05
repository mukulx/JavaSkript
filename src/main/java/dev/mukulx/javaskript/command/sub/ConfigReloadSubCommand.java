package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ConfigReloadSubCommand extends SubCommand {

  public ConfigReloadSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    sender.sendMessage(Component.text("Reloading configuration...").color(NamedTextColor.YELLOW));

    try {
      plugin.reloadPluginConfig();
      sender.sendMessage(
          Component.text("Configuration reloaded successfully!").color(NamedTextColor.GREEN));
      sender.sendMessage(
          Component.text("Note: Some settings require a plugin restart to take effect")
              .color(NamedTextColor.GRAY));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Error reloading configuration: " + e.getMessage())
              .color(NamedTextColor.RED));
      plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
    }
  }
}
