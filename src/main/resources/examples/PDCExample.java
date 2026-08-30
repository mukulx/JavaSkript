package examples;

import dev.mukulx.javaskript.api.PDCHelper;
import dev.mukulx.javaskript.api.gui.ItemBuilder;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * PersistentDataContainer (PDC / NBT) Example for JavaSkript.
 *
 * <p>Demonstrates permanent, invisible custom data on Items and Entities with zero boilerplate:
 *
 * <ul>
 *   <li>Attaching custom stats (damage, rarity, UUIDs, levels) to items
 *   <li>Reading custom stats during combat events
 *   <li>Tagging custom mobs and bosses for custom drops/events
 * </ul>
 */
public class PDCExample implements CommandExecutor, Listener {

  // Auto-injected by JavaSkript
  private PDCHelper pdc;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("This command is for players only.");
      return true;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("give")) {
      giveHyperionSword(player);
      return true;
    }

    if (args[0].equalsIgnoreCase("spawn")) {
      spawnCustomBoss(player);
      return true;
    }

    player.sendMessage("§eUsage: /" + label + " [give|spawn]");
    return true;
  }

  /** 1. Create a custom item with embedded PDC / NBT tags */
  private void giveHyperionSword(Player player) {
    ItemStack sword =
        new ItemBuilder(Material.NETHERITE_SWORD)
            .name("<gradient:#ff5555:#ffaa00><bold>Hyperion Blade</bold></gradient>")
            .lore(
                "<gray>A mythical blade forged in dragon fire.</gray>",
                "",
                "<red>⚔ +25.0 Extra Fire Damage</red>",
                "<yellow>✦ Soulbound to: " + player.getName() + "</yellow>")
            .glow()
            .unbreakable(true)
            // Storing custom data directly via ItemBuilder or pdc.set(...)
            .pdc("custom_id", "hyperion_blade")
            .pdc("bonus_damage", 25.0)
            .pdc("soulbound", player.getUniqueId().toString())
            .build();

    player.getInventory().addItem(sword);
    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
    player.sendMessage("§aYou received the §6Hyperion Blade§a!");
  }

  /** 2. Spawn an Entity and tag it with custom persistent data */
  private void spawnCustomBoss(Player player) {
    LivingEntity zombie =
        (LivingEntity)
            player
                .getWorld()
                .spawnEntity(player.getLocation(), org.bukkit.entity.EntityType.ZOMBIE);
    zombie.customName(
        net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
            .deserialize("<red><bold>Inferno Titan</bold></red>"));
    zombie.setCustomNameVisible(true);

    // Tag the boss mob
    pdc.set(zombie, "boss_type", "inferno_titan");
    pdc.set(zombie, "drop_multiplier", 3);
    pdc.set(zombie, "spawner_uuid", player.getUniqueId());

    player.sendMessage("§cSpawned Inferno Titan with custom boss tags!");
  }

  /** 3. Read item tags on combat */
  @EventHandler
  public void onAttack(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player player)) {
      return;
    }

    ItemStack weapon = player.getInventory().getItemInMainHand();

    // Check if item has our custom tag
    if (pdc.has(weapon, "custom_id")
        && "hyperion_blade".equals(pdc.getString(weapon, "custom_id"))) {
      double bonusDamage = pdc.getDouble(weapon, "bonus_damage", 0.0);
      event.setDamage(event.getDamage() + bonusDamage);

      // Verify soulbound ownership
      UUID owner = pdc.getUUID(weapon, "soulbound");
      if (owner != null && !owner.equals(player.getUniqueId())) {
        player.sendMessage("§cThis blade is soulbound to another warrior!");
      }

      // Visual / Audio feedback
      if (event.getEntity() instanceof LivingEntity victim) {
        victim.setFireTicks(100);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.7f, 1.8f);
      }
    }
  }

  /** 4. Read entity tags on death */
  @EventHandler
  public void onBossDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();

    // Check if the entity has our boss tag
    if (pdc.has(entity, "boss_type")) {
      String bossType = pdc.getString(entity, "boss_type");
      int multiplier = pdc.getInt(entity, "drop_multiplier", 1);

      if ("inferno_titan".equals(bossType)) {
        event.getDrops().add(new ItemStack(Material.NETHERITE_INGOT, multiplier));
        entity
            .getWorld()
            .sendMessage(
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<gold>The <red>Inferno Titan</red> has been vanquished!</gold>"));
      }
    }
  }
}
