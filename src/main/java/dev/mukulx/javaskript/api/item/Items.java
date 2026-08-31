package dev.mukulx.javaskript.api.item;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Static shorthand facade for item creation, inspection, and safe inventory manipulation.
 *
 * <p>Enables 1-line item crafting and inventory checks anywhere:
 *
 * <pre>{@code
 * if (Items.has(player, Material.DIAMOND, 5)) {
 *     Items.take(player, Material.DIAMOND, 5);
 *     Items.give(player, Items.of(Material.NETHERITE_INGOT).build());
 * }
 * }</pre>
 */
public final class Items {

  private static ItemHelper helper;

  private Items() {}

  private static ItemHelper get() {
    if (helper == null) {
      helper = new ItemHelper(JavaSkriptPlugin.getInstance());
    }
    return helper;
  }

  public static void setInstance(ItemHelper h) {
    helper = h;
  }

  public static ItemBuilder of(Material material) {
    return get().of(material);
  }

  public static ItemBuilder of(Material material, int amount) {
    return get().of(material, amount);
  }

  public static ItemBuilder create(Material material) {
    return get().create(material);
  }

  public static ItemBuilder create(Material material, int amount) {
    return get().create(material, amount);
  }

  public static ItemBuilder from(ItemStack itemStack) {
    return get().from(itemStack);
  }

  public static ItemBuilder skull() {
    return get().skull();
  }

  public static ItemBuilder skull(OfflinePlayer player) {
    return get().skull(player);
  }

  public static ItemBuilder skull(String playerNameOrBase64) {
    return get().skull(playerNameOrBase64);
  }

  /** Count total amount of a given material in the player's inventory. */
  public static int count(Player player, Material material) {
    return get().count(player, material);
  }

  /** Check if a player has at least the specified amount of a material. */
  public static boolean has(Player player, Material material, int amount) {
    return get().has(player, material, amount);
  }

  /** Check if a player has at least 1 of a material. */
  public static boolean has(Player player, Material material) {
    return get().has(player, material);
  }

  /** Safely remove a specified amount of a material across all inventory slots. */
  public static boolean take(Player player, Material material, int amount) {
    return get().take(player, material, amount);
  }

  /** Safely give items to a player, dropping any leftovers that don't fit in their inventory. */
  public static void give(Player player, ItemStack... items) {
    get().give(player, items);
  }

  /** Safely give ItemBuilders to a player. */
  public static void give(Player player, ItemBuilder... builders) {
    get().give(player, builders);
  }
}
