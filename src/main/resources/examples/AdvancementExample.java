package examples;

import dev.mukulx.javaskript.api.AdvancementHelper;
import dev.mukulx.javaskript.api.advancement.CustomAdvancement;
import io.papermc.paper.advancement.AdvancementDisplay;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Modern Paper 1.21.11+ Advancement API Example for JavaSkript.
 *
 * <p>Demonstrates full control over native Minecraft Advancements without datapacks:
 *
 * <ul>
 *   <li>Instant custom toast notifications in the top-right corner
 *   <li>Custom advancement trees with root tabs and custom backgrounds
 *   <li>Task, Goal, and Challenge frame types with MiniMessage rich text
 *   <li>Multi-criteria requirements (AND / OR logic)
 *   <li>Completion reward callbacks
 *   <li>Automatic lifecycle cleanup on script reload/unload
 * </ul>
 */
public class AdvancementExample implements CommandExecutor {

  // Automatically injected by JavaSkript
  private AdvancementHelper advancements;

  public void onEnable() {
    // 1. Register a Root Advancement Tab with Adventure background
    advancements
        .createRoot("custom_root", CustomAdvancement.Backgrounds.ADVENTURE)
        .title("<gold><bold>Server Quests</bold></gold>")
        .description("<yellow>Begin your journey on the server</yellow>")
        .icon(Material.BOOK)
        .task()
        .toast(false)
        .announce(false)
        .register();

    // 2. Child Challenge Advancement with completion callback
    advancements
        .create("dragon_slayer")
        .title("<dark_purple><bold>Dragon Slayer</bold></dark_purple>")
        .description("<gray>Defeat the beast and claim the crown</gray>")
        .icon(Material.DRAGON_HEAD)
        .challenge()
        .parent(advancements.createKey("custom_root"))
        .onComplete(
            (player, adv) -> {
              player.sendMessage("§6[Quest] §aCongratulations! You earned 1,000 XP.");
              player.giveExp(1000);
            })
        .register();

    // 3. Multi-step Goal Advancement (Requires all criteria)
    advancements
        .create("mineral_collector")
        .title("<aqua>Geologist</aqua>")
        .description("<gray>Collect both diamond and netherite</gray>")
        .icon(Material.DIAMOND)
        .goal()
        .parent(advancements.createKey("custom_root"))
        .criteria("found_diamond", "found_netherite")
        .requireAll()
        .register();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("This command can only be executed by players.");
      return true;
    }

    if (args.length == 0) {
      player.sendMessage("§6--- Advancement Commands ---");
      player.sendMessage("§e/" + label + " toast §7- Show instant challenge toast notification");
      player.sendMessage("§e/" + label + " grant §7- Award dragon slayer advancement");
      player.sendMessage("§e/" + label + " revoke §7- Revoke dragon slayer advancement");
      player.sendMessage("§e/" + label + " step1 §7- Complete first criterion for Geologist");
      player.sendMessage("§e/" + label + " step2 §7- Complete second criterion for Geologist");
      player.sendMessage("§e/" + label + " progress §7- Check current quest progress");
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "toast" -> {
        // Send a custom toast popup without creating permanent achievements
        advancements.toast(
            player,
            Material.NETHER_STAR,
            "<gradient:#ffaa00:#ff5555><bold>Level 100 Reached!</bold></gradient>",
            AdvancementDisplay.Frame.CHALLENGE);
      }
      case "grant" -> {
        boolean granted = advancements.grant(player, "dragon_slayer");
        player.sendMessage(
            granted ? "§aGranted Dragon Slayer!" : "§cAlready completed or not found.");
      }
      case "revoke" -> {
        boolean revoked = advancements.revoke(player, "dragon_slayer");
        player.sendMessage(
            revoked ? "§eRevoked Dragon Slayer." : "§cNot awarded yet or not found.");
      }
      case "step1" -> {
        advancements.grant(player, "mineral_collector", "found_diamond");
        player.sendMessage("§bCompleted criterion: found_diamond");
      }
      case "step2" -> {
        advancements.grant(player, "mineral_collector", "found_netherite");
        player.sendMessage("§bCompleted criterion: found_netherite");
      }
      case "progress" -> {
        double percent =
            advancements.getProgressPercent(player, advancements.createKey("mineral_collector"));
        boolean done = advancements.has(player, "mineral_collector");
        player.sendMessage("§7Geologist Progress: §e" + (int) percent + "% §7(Done: " + done + ")");
      }
      default -> player.sendMessage("§cUnknown subcommand. Type /" + label + " for help.");
    }

    return true;
  }
}
