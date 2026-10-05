package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class AddonsSubCommand extends SubCommand {

  public AddonsSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    var addons = plugin.getAPI().getAddons();
    sender.sendMessage(
        Component.text("Registered Addons (" + addons.size() + ")").color(NamedTextColor.GOLD));
    if (addons.isEmpty()) {
      sender.sendMessage(
          Component.text("  No external addons registered.").color(NamedTextColor.GRAY));
      return;
    }
    for (var addon : addons) {
      sender.sendMessage(
          Component.text("  * " + addon.getName() + " v" + addon.getVersion())
              .color(NamedTextColor.GREEN)
              .append(Component.text(" by " + addon.getAuthor()).color(NamedTextColor.GRAY)));
      if (addon.getDescription() != null && !addon.getDescription().isEmpty()) {
        sender.sendMessage(
            Component.text("    " + addon.getDescription()).color(NamedTextColor.DARK_GRAY));
      }
    }
  }
}
