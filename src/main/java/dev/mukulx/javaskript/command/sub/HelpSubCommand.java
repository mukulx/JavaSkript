package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class HelpSubCommand extends SubCommand {

  public HelpSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    sender.sendMessage(Component.text("JavaSkript Commands:").color(NamedTextColor.GOLD));
    sender.sendMessage(
        Component.text("  /js reload <script|folder|all> - Reload a script, folder, or all scripts")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text(
                "  /js restart <script|folder|all> - Restart a script, folder, or all scripts")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js configreload - Reload the configuration file")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js addons - List registered external addons")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js list - List all scripts (loaded and disabled)")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js load <script|folder|all> - Load a script, folder, or all scripts")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js unload <script|folder|all> - Unload a script, folder, or all scripts")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text(
                "  /js enable <script|folder|all> - Enable a disabled script, folder, or all scripts")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text(
                "  /js disable <script|folder|all> - Disable a script, folder, or all scripts")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js info <script> - Show script information")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js profile [start|stop|top|<script>] - Live performance profiler")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js benchmark <script> - Run synthetic benchmark test")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js timings - Quick view of top slowest handlers")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js debug - Toggle debug mode").color(NamedTextColor.YELLOW));
    sender.sendMessage(Component.text("").color(NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text("Tip: Organize scripts in folders: scripts/pvp/PvP.java")
            .color(NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text("Tip: Prefix with '-' to disable: -PvP.java or folder -examples/")
            .color(NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text("Tip: In code, use @Disabled or // @disabled to disable")
            .color(NamedTextColor.GRAY));
  }
}
