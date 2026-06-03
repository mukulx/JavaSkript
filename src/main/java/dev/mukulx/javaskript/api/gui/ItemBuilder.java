package dev.mukulx.javaskript.api.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Easy item builder for GUIs */
public class ItemBuilder {

  private final ItemStack item;
  private final ItemMeta meta;

  /**
   * Create an item builder
   *
   * @param material The material
   */
  public ItemBuilder(Material material) {
    this.item = new ItemStack(material);
    this.meta = item.getItemMeta();
  }

  /**
   * Create an item builder with amount
   *
   * @param material The material
   * @param amount The amount
   */
  public ItemBuilder(Material material, int amount) {
    this.item = new ItemStack(material, amount);
    this.meta = item.getItemMeta();
  }

  /**
   * Set the display name
   *
   * @param name The name
   * @return This builder
   */
  public ItemBuilder name(Component name) {
    if (meta != null) {
      meta.displayName(name);
    }
    return this;
  }

  /**
   * Set the display name (String)
   *
   * @param name The name
   * @return This builder
   */
  public ItemBuilder name(String name) {
    return name(Component.text(name));
  }

  /**
   * Set the lore
   *
   * @param lore The lore lines
   * @return This builder
   */
  public ItemBuilder lore(Component... lore) {
    if (meta != null) {
      meta.lore(Arrays.asList(lore));
    }
    return this;
  }

  /**
   * Set the lore (Strings)
   *
   * @param lore The lore lines
   * @return This builder
   */
  public ItemBuilder lore(String... lore) {
    if (meta != null) {
      List<Component> components = new ArrayList<>();
      for (String line : lore) {
        components.add(Component.text(line));
      }
      meta.lore(components);
    }
    return this;
  }

  /**
   * Set the lore (List)
   *
   * @param lore The lore lines
   * @return This builder
   */
  public ItemBuilder lore(List<String> lore) {
    if (meta != null && lore != null) {
      List<Component> components = new ArrayList<>();
      for (String line : lore) {
        components.add(Component.text(line));
      }
      meta.lore(components);
    }
    return this;
  }

  /**
   * Add an enchantment
   *
   * @param enchantment The enchantment
   * @param level The level
   * @return This builder
   */
  public ItemBuilder enchant(Enchantment enchantment, int level) {
    if (meta != null) {
      meta.addEnchant(enchantment, level, true);
    }
    return this;
  }

  /**
   * Add item flags
   *
   * @param flags The flags
   * @return This builder
   */
  public ItemBuilder flags(ItemFlag... flags) {
    if (meta != null) {
      meta.addItemFlags(flags);
    }
    return this;
  }

  /**
   * Hide all flags
   *
   * @return This builder
   */
  public ItemBuilder hideAll() {
    if (meta != null) {
      meta.addItemFlags(ItemFlag.values());
    }
    return this;
  }

  /**
   * Make the item glow (adds enchantment and hides it)
   *
   * @return This builder
   */
  public ItemBuilder glow() {
    enchant(Enchantment.UNBREAKING, 1);
    flags(ItemFlag.HIDE_ENCHANTS);
    return this;
  }

  /**
   * Set the amount
   *
   * @param amount The amount
   * @return This builder
   */
  public ItemBuilder amount(int amount) {
    item.setAmount(amount);
    return this;
  }

  /**
   * Set unbreakable
   *
   * @param unbreakable Whether unbreakable
   * @return This builder
   */
  public ItemBuilder unbreakable(boolean unbreakable) {
    if (meta != null) {
      meta.setUnbreakable(unbreakable);
    }
    return this;
  }

  /**
   * Set custom model data
   *
   * @param data The custom model data
   * @return This builder
   */
  public ItemBuilder customModelData(int data) {
    if (meta != null) {
      meta.setCustomModelData(data);
    }
    return this;
  }

  /**
   * Build the item
   *
   * @return The built ItemStack
   */
  public ItemStack build() {
    if (meta != null) {
      item.setItemMeta(meta);
    }
    return item;
  }
}
