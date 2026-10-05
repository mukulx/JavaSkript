package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class DebugSubCommand extends SubCommand {

  public DebugSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    boolean currentMode = plugin.isDebugMode();
    boolean newMode = !currentMode;

    plugin.setDebugMode(newMode);

    if (newMode) {
      sender.sendMessage(Component.text("Debug mode ENABLED").color(NamedTextColor.GREEN));
      sender.sendMessage(
          Component.text("Detailed logging is now active").color(NamedTextColor.GRAY));
    } else {
      sender.sendMessage(Component.text("Debug mode DISABLED").color(NamedTextColor.YELLOW));
      sender.sendMessage(Component.text("Logging returned to normal").color(NamedTextColor.GRAY));
    }
  }
}
