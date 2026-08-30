package examples;

import dev.mukulx.javaskript.api.HologramHelper;
import dev.mukulx.javaskript.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Modern Display Entity Hologram Example for JavaSkript.
 *
 * <p>Demonstrates zero-lag floating text, 3D items, and blocks with full customization:
 *
 * <ul>
 *   <li>Dynamic auto-refreshing leaderboards
 *   <li>Floating 3D items with glow and scaling
 *   <li>Floating 3D blocks with custom billboard modes
 *   <li>Automatic cleanup on script reload/unload
 * </ul>
 */
public class HologramExample implements CommandExecutor {

  // Auto-injected by JavaSkript
  private HologramHelper holograms;

  private Hologram activeLeaderboard;
  private Hologram activeItemDisplay;
  private Hologram activeBlockDisplay;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("This command is for players only.");
      return true;
    }

    if (args.length == 0) {
      player.sendMessage("§eUsage: /" + label + " [leaderboard|item|block|clear]");
      return true;
    }

    Location loc = player.getLocation().add(0, 1.5, 0);

    switch (args[0].toLowerCase()) {
      case "leaderboard" -> {
        spawnLeaderboard(loc);
        player.sendMessage("§aSpawned dynamic leaderboard hologram!");
      }
      case "item" -> {
        spawnItemHologram(loc);
        player.sendMessage("§aSpawned floating 3D item showcase!");
      }
      case "block" -> {
        spawnBlockHologram(loc);
        player.sendMessage("§aSpawned floating 3D block showcase!");
      }
      case "clear" -> {
        holograms.removeAll();
        player.sendMessage("§cCleared all holograms!");
      }
      default -> player.sendMessage("§eUsage: /" + label + " [leaderboard|item|block|clear]");
    }
    return true;
  }

  /** 1. Multi-line dynamic leaderboard with auto-refresh */
  private void spawnLeaderboard(Location loc) {
    if (activeLeaderboard != null) {
      activeLeaderboard.remove();
    }

    activeLeaderboard =
        holograms
            .create(loc)
            .line("<gradient:#ff5555:#ffaa00><bold>✦ SERVER LEADERBOARD ✦</bold></gradient>")
            .line("<gray>Top Warriors of the Realm</gray>")
            .line("")
            .line("<gold>#1 <yellow>Mukul <gray>- <white>2,450 Kills</white>")
            .line("<silver>#2 <gray>Alex <gray>- <white>1,820 Kills</white>")
            .line("<brown>#3 <dark_gray>Steve <gray>- <white>950 Kills</white>")
            .line("")
            // Dynamic text supplier that updates automatically
            .line(
                () ->
                    "<gray>Online Players: <green>"
                        + Bukkit.getOnlinePlayers().size()
                        + "</green></gray>")
            .billboard(Display.Billboard.CENTER)
            .shadow(true)
            .seeThrough(false)
            .lineSpacing(0.28)
            // Auto refresh dynamic lines every 20 ticks (1 second)
            .updateInterval(20)
            .spawn();
  }

  /** 2. 3D Floating item with glow and title */
  private void spawnItemHologram(Location loc) {
    if (activeItemDisplay != null) {
      activeItemDisplay.remove();
    }

    ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);

    activeItemDisplay =
        holograms
            .create(loc)
            .line("<gradient:#aa00aa:#ff55ff><bold>Excalibur</bold></gradient>")
            .line("<gray>Forged in the Aether</gray>")
            // Floating item that is FIXED in world space (does NOT spin to follow camera!)
            // Note: Use .item(sword, 1.2, true) if you want it to follow camera
            .itemFixed(sword, 1.2)
            .billboard(Display.Billboard.CENTER) // Text faces player, item stays fixed!
            .glowing(true)
            .glowColor(org.bukkit.Color.PURPLE)
            .lineSpacing(0.4)
            .spawn();
  }

  /** 3. 3D Floating block showcase */
  private void spawnBlockHologram(Location loc) {
    if (activeBlockDisplay != null) {
      activeBlockDisplay.remove();
    }

    activeBlockDisplay =
        holograms
            .create(loc)
            .line("<aqua><bold>Enchantment Altar</bold></aqua>")
            .block(Material.ENCHANTING_TABLE)
            .billboard(Display.Billboard.CENTER)
            .scale(0.8)
            .lineSpacing(0.4)
            .spawn();
  }
}
