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

  /** Count total amount of a given material in the player's inventory. */
  public int count(Player player, Material material) {
    if (player == null || material == null || material.isAir()) return 0;
    int total = 0;
    for (ItemStack stack : player.getInventory().getContents()) {
      if (stack != null && stack.getType() == material) {
        total += stack.getAmount();
      }
    }
    return total;
  }

  /** Check if a player has at least the specified amount of a material. */
  public boolean has(Player player, Material material, int amount) {
    if (amount <= 0) return true;
    return count(player, material) >= amount;
  }

  /** Check if a player has at least 1 of a material. */
  public boolean has(Player player, Material material) {
    return has(player, material, 1);
  }

  /**
   * Safely remove a specified amount of a material across all inventory slots.
   *
   * @param player The player
   * @param material The material to remove
   * @param amount The quantity to remove
   * @return true if the items were found and removed, false if insufficient items
   */
  public boolean take(Player player, Material material, int amount) {
    if (player == null || material == null || amount <= 0) return false;
    if (!has(player, material, amount)) {
      return false;
    }

    int remaining = amount;
    ItemStack[] contents = player.getInventory().getContents();
    for (int i = 0; i < contents.length; i++) {
      ItemStack stack = contents[i];
      if (stack != null && stack.getType() == material) {
        int stackAmount = stack.getAmount();
        if (stackAmount <= remaining) {
          remaining -= stackAmount;
          contents[i] = null;
        } else {
          stack.setAmount(stackAmount - remaining);
          remaining = 0;
          break;
        }
      }
    }
    player.getInventory().setContents(contents);
    player.updateInventory();
    return true;
  }
}
