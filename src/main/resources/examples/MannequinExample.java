package examples;

import dev.mukulx.javaskript.api.MannequinHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;

/**
 * Modern Paper 1.21.11+ Mannequin API Example for JavaSkript.
 *
 * <p>Demonstrates full control over native Minecraft Mannequins without external plugins:
 *
 * <ul>
 *   <li>Interactive NPCs with clickable action triggers
 *   <li>Player skin resolution from player names, UUIDs, or Player instances
 *   <li>Custom description lines appearing below the name tag
 *   <li>Pose locking (standing, crouching, sleeping, swimming, gliding)
 *   <li>Equipment, skin layer toggles, and immovable physics
 *   <li>Automatic cleanup on script reload/unload
 * </ul>
 */
public class MannequinExample implements CommandExecutor {

  // Automatically injected by JavaSkript
  private MannequinHelper mannequins;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("This command can only be executed by players.");
      return true;
    }

    if (args.length == 0) {
      player.sendMessage("§6--- Mannequin Commands ---");
      player.sendMessage("§e/" + label + " npc §7- Spawn an interactive NPC merchant");
      player.sendMessage(
          "§e/"
              + label
              + " statue <pose> §7- Spawn a statue (standing, sneaking, swimming, sleeping, gliding)");
      player.sendMessage(
          "§e/" + label + " skin <name> §7- Spawn a mannequin with a specific player's skin");
      player.sendMessage("§e/" + label + " clear §7- Remove all mannequins spawned by this script");
      return true;
    }

    Location loc = player.getLocation();

    switch (args[0].toLowerCase()) {
      case "npc" -> {
        mannequins
            .create(loc)
            .name("<gradient:#ffaa00:#ff5555><bold>Quest Master</bold></gradient>")
            .description("<gray>Right-click to talk | Left-click to spar</gray>")
            .skin(player)
            .helmet(Material.GOLDEN_HELMET)
            .chestplate(Material.DIAMOND_CHESTPLATE)
            .leggings(Material.IRON_LEGGINGS)
            .boots(Material.DIAMOND_BOOTS)
            .mainHandItem(Material.DIAMOND_SWORD)
            .offHandItem(Material.SHIELD)
            .standing()
            .immovable()
            .invulnerable()
            .tag("quest_npc")
            .onClick(
                (p, m) -> {
                  p.sendMessage(
                      "§a[Quest Master] §fGreetings, §e"
                          + p.getName()
                          + "§f! Are you ready for an adventure?");
                  m.swingMainHand();
                })
            .onAttack(
                (p, m) -> {
                  p.sendMessage("§c[Quest Master] §fHey! Watch your sword!");
                  m.swingOffHand();
                })
            .spawn();

        player.sendMessage("§aSpawned interactive Quest Master NPC!");
      }
      case "statue" -> {
        String poseName = args.length > 1 ? args[1].toLowerCase() : "standing";
        Pose targetPose =
            switch (poseName) {
              case "sneaking", "crouch", "crouching" -> Pose.SNEAKING;
              case "swimming", "swim" -> Pose.SWIMMING;
              case "sleeping", "sleep" -> Pose.SLEEPING;
              case "gliding", "flying" -> Pose.FALL_FLYING;
              default -> Pose.STANDING;
            };

        mannequins
            .create(loc)
            .name("<aqua>Statue of Valor</aqua>")
            .description("<dark_gray>Pose: " + targetPose.name() + "</dark_gray>")
            .skin("Mukulx")
            .pose(targetPose)
            .helmet(Material.NETHERITE_HELMET)
            .chestplate(Material.NETHERITE_CHESTPLATE)
            .mainHandItem(Material.TRIDENT)
            .immovable()
            .invulnerable()
            .spawn();

        player.sendMessage("§aSpawned Statue of Valor with pose: §e" + targetPose.name());
      }
      case "skin" -> {
        String skinName = args.length > 1 ? args[1] : "Steve";
        mannequins.spawn(loc, "<yellow>" + skinName + "</yellow>", skinName);
        player.sendMessage("§aSpawned mannequin with skin: §e" + skinName);
      }
      case "clear" -> {
        int count = mannequins.getAll().size();
        mannequins.removeAll();
        player.sendMessage("§cRemoved " + count + " mannequin(s)!");
      }
      default -> player.sendMessage("§cUnknown subcommand. Use /" + label + " for help.");
    }

    return true;
  }
}
