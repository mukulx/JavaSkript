package dev.mukulx.javaskript.api.item;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.gui.ItemBuilder;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** High-level Item Helper API injected into scripts for quick custom item crafting. */
public class ItemHelper {

  private final JavaSkriptPlugin plugin;

  public ItemHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  public ItemBuilder create(Material material) {
    return new ItemBuilder(material);
  }

  public ItemBuilder create(Material material, int amount) {
    return new ItemBuilder(material, amount);
  }

  public ItemBuilder of(Material material) {
    return new ItemBuilder(material);
  }

  public ItemBuilder of(Material material, int amount) {
    return new ItemBuilder(material, amount);
  }

  public ItemBuilder from(ItemStack itemStack) {
    return ItemBuilder.from(itemStack);
  }

  public ItemBuilder skull() {
    return ItemBuilder.skull();
  }

  public ItemBuilder skull(OfflinePlayer player) {
    return ItemBuilder.skull(player);
  }

  public ItemBuilder skull(String playerNameOrBase64) {
    return ItemBuilder.skull(playerNameOrBase64);
  }

  /** Give items directly to a player, dropping any leftovers that don't fit in their inventory. */
  public void give(Player player, ItemStack... items) {
    if (player == null || !player.isOnline() || items == null) return;
    for (ItemStack it : items) {
      if (it == null || it.getType() == Material.AIR) continue;
      Map<Integer, ItemStack> leftover = player.getInventory().addItem(it);
      for (ItemStack drop : leftover.values()) {
        player.getWorld().dropItemNaturally(player.getLocation(), drop);
      }
    }
  }

  /** Give ItemBuilders directly to a player. */
  public void give(Player player, ItemBuilder... builders) {
    if (player == null || builders == null) return;
    for (ItemBuilder b : builders) {
      if (b != null) {
        give(player, b.build());
      }
    }
  }
}
