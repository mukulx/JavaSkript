// Welcome to JavaSkript!
// This is an example script to help you get started.
//
// Documentation: https://github.com/mukulx/JavaSkript/tree/main/docs
// Quick Start: https://github.com/mukulx/JavaSkript/blob/main/docs/QUICKSTART.md
// Tutorial: https://github.com/mukulx/JavaSkript/blob/main/docs/TUTORIAL.md
// Examples: https://github.com/mukulx/JavaSkript/tree/main/src/main/resources/examples
//
// To create your own script:
// 1. Create a new .java file in plugins/JavaSkript/scripts/
// 2. Write your Java code (see examples in the GitHub repo)
// 3. Save the file - it will auto-reload!
//
// Commands:
// /js reload - Reload all scripts
// /js list - List all scripts
// /js info <script> - Show script information
//
// Need help? Check the documentation links above!

import dev.mukulx.javaskript.script.FoliaSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@FoliaSupport
public class Example implements Listener {

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (event == null || event.getPlayer() == null) return;

    var player = event.getPlayer();

    // Only show to ops
    if (!player.isOp()) return;

    player.sendMessage(Component.empty());
    player.sendMessage(
        Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
    player.sendMessage(
        Component.text("  JavaSkript", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(" - Write Java in script files", NamedTextColor.YELLOW)));
    player.sendMessage(Component.empty());

    player.sendMessage(Component.text("  Get Started:", NamedTextColor.AQUA, TextDecoration.BOLD));
    player.sendMessage(
        Component.text("  • ", NamedTextColor.DARK_GRAY)
            .append(
                Component.text("Quick Start Guide", NamedTextColor.GREEN)
                    .clickEvent(
                        ClickEvent.openUrl(
                            "https://github.com/mukulx/JavaSkript/blob/main/docs/QUICKSTART.md"))
                    .hoverEvent(Component.text("Click to open"))));

    player.sendMessage(
        Component.text("  • ", NamedTextColor.DARK_GRAY)
            .append(
                Component.text("Tutorial", NamedTextColor.GREEN)
                    .clickEvent(
                        ClickEvent.openUrl(
                            "https://github.com/mukulx/JavaSkript/blob/main/docs/TUTORIAL.md"))
                    .hoverEvent(Component.text("Click to open"))));

    player.sendMessage(
        Component.text("  • ", NamedTextColor.DARK_GRAY)
            .append(
                Component.text("Examples", NamedTextColor.GREEN)
                    .clickEvent(
                        ClickEvent.openUrl(
                            "https://github.com/mukulx/JavaSkript/tree/main/src/main/resources/examples"))
                    .hoverEvent(Component.text("Click to open"))));

    player.sendMessage(
        Component.text("  • ", NamedTextColor.DARK_GRAY)
            .append(
                Component.text("API Documentation", NamedTextColor.GREEN)
                    .clickEvent(
                        ClickEvent.openUrl(
                            "https://github.com/mukulx/JavaSkript/blob/main/docs/API.md"))
                    .hoverEvent(Component.text("Click to open"))));

    player.sendMessage(Component.empty());
    player.sendMessage(
        Component.text("  Scripts folder: ", NamedTextColor.GRAY)
            .append(Component.text("plugins/JavaSkript/scripts/", NamedTextColor.WHITE)));
    player.sendMessage(
        Component.text("  Commands: ", NamedTextColor.GRAY)
            .append(Component.text("/js reload, /js list, /js info", NamedTextColor.WHITE)));
    player.sendMessage(Component.empty());
    player.sendMessage(
        Component.text(
            "  Delete this script or disable it with: /js disable Example.java",
            NamedTextColor.DARK_GRAY,
            TextDecoration.ITALIC));
    player.sendMessage(
        Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
    player.sendMessage(Component.empty());
  }
}
