package dev.mukulx.javaskript.api.gui;

import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

/**
 * Universal, fluent, and highly extensible Item Builder.
 *
 * <p>Features: - MiniMessage and legacy color formatting for names and lore - Static factory
 * methods (of, from, skull) - Specialized meta support (Leather, Potion, Skull, Damage, PDC) - Open
 * generic Consumer mutator {@link #meta(Class, Consumer)} for 100% Bukkit flexibility - Cloned item
 * modifications
 */
public class ItemBuilder implements Cloneable {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.legacyAmpersand();

  private final ItemStack item;
  private ItemMeta meta;

  // ==========================================
  // Constructors & Factories
  // ==========================================

  public ItemBuilder(Material material) {
    this(material, 1);
  }

  public ItemBuilder(Material material, int amount) {
    this.item = new ItemStack(material, Math.max(1, amount));
    this.meta = item.getItemMeta();
  }

  public ItemBuilder(ItemStack item) {
    if (item == null || item.getType() == Material.AIR) {
      this.item = new ItemStack(Material.STONE, 1);
    } else {
      this.item = item.clone();
    }
    this.meta = this.item.getItemMeta();
  }

  public static ItemBuilder of(Material material) {
    return new ItemBuilder(material, 1);
  }

  public static ItemBuilder of(Material material, int amount) {
    return new ItemBuilder(material, amount);
  }

  public static ItemBuilder from(ItemStack itemStack) {
    return new ItemBuilder(itemStack);
  }

  public static ItemBuilder skull() {
    return new ItemBuilder(Material.PLAYER_HEAD);
  }

  public static ItemBuilder skull(OfflinePlayer player) {
    return skull().skullOwner(player);
  }

  public static ItemBuilder skull(String playerNameOrBase64) {
    ItemBuilder b = skull();
    if (playerNameOrBase64 != null && playerNameOrBase64.length() > 36) {
      return b.skullTexture(playerNameOrBase64);
    }
    return b.skullOwner(playerNameOrBase64);
  }

  // ==========================================
  // Basic Properties
  // ==========================================

  public ItemBuilder amount(int amount) {
    item.setAmount(Math.max(1, amount));
    return this;
  }

  public ItemBuilder material(Material material) {
    item.setType(material);
    this.meta = item.getItemMeta();
    return this;
  }

  // ==========================================
  // Display Name & Lore (MiniMessage + Legacy)
  // ==========================================

  public ItemBuilder name(Component name) {
    if (meta != null) {
      meta.displayName(name);
    }
    return this;
  }

  public ItemBuilder name(String name) {
    return name(parseComponent(name));
  }

  public ItemBuilder lore(Component... lore) {
    if (meta != null) {
      meta.lore(lore != null ? Arrays.asList(lore) : Collections.emptyList());
    }
    return this;
  }

  public ItemBuilder lore(String... lore) {
    if (meta != null) {
      List<Component> components = new ArrayList<>();
      if (lore != null) {
        for (String line : lore) {
          components.add(parseComponent(line));
        }
      }
      meta.lore(components);
    }
    return this;
  }

  public ItemBuilder lore(List<String> lore) {
    if (meta != null && lore != null) {
      List<Component> components = new ArrayList<>();
      for (String line : lore) {
        components.add(parseComponent(line));
      }
      meta.lore(components);
    }
    return this;
  }

  public ItemBuilder appendLore(String... lines) {
    if (meta != null && lines != null) {
      List<Component> existing = meta.lore();
      List<Component> updated = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
      for (String line : lines) {
        updated.add(parseComponent(line));
      }
      meta.lore(updated);
    }
    return this;
  }

  public ItemBuilder appendLore(Component... lines) {
    if (meta != null && lines != null) {
      List<Component> existing = meta.lore();
      List<Component> updated = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
      Collections.addAll(updated, lines);
      meta.lore(updated);
    }
    return this;
  }

  public ItemBuilder clearLore() {
    if (meta != null) {
      meta.lore(Collections.emptyList());
    }
    return this;
  }

  // ==========================================
  // Enchantments & Glow
  // ==========================================

  public ItemBuilder enchant(Enchantment enchantment) {
    return enchant(enchantment, 1, true);
  }

  public ItemBuilder enchant(Enchantment enchantment, int level) {
    return enchant(enchantment, level, true);
  }

  public ItemBuilder enchant(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
    if (meta != null && enchantment != null) {
      meta.addEnchant(enchantment, level, ignoreLevelRestriction);
    }
    return this;
  }

  public ItemBuilder removeEnchant(Enchantment enchantment) {
    if (meta != null && enchantment != null) {
      meta.removeEnchant(enchantment);
    }
    return this;
  }

  public ItemBuilder clearEnchants() {
    if (meta != null) {
      for (Enchantment e : new ArrayList<>(meta.getEnchants().keySet())) {
        meta.removeEnchant(e);
      }
    }
    return this;
  }

  public ItemBuilder glow() {
    return glow(true);
  }

  public ItemBuilder glow(boolean glow) {
    if (glow) {
      enchant(Enchantment.UNBREAKING, 1);
      flags(ItemFlag.HIDE_ENCHANTS);
    } else {
      removeEnchant(Enchantment.UNBREAKING);
    }
    return this;
  }

  // ==========================================
  // Flags & Durability
  // ==========================================

  public ItemBuilder flags(ItemFlag... flags) {
    if (meta != null && flags != null) {
      meta.addItemFlags(flags);
    }
    return this;
  }

  public ItemBuilder removeFlags(ItemFlag... flags) {
    if (meta != null && flags != null) {
      meta.removeItemFlags(flags);
    }
    return this;
  }

  public ItemBuilder hideAll() {
    if (meta != null) {
      meta.addItemFlags(ItemFlag.values());
    }
    return this;
  }

  public ItemBuilder unbreakable() {
    return unbreakable(true);
  }

  public ItemBuilder unbreakable(boolean unbreakable) {
    if (meta != null) {
      meta.setUnbreakable(unbreakable);
    }
    return this;
  }

  public ItemBuilder damage(int damage) {
    if (meta instanceof Damageable damageable) {
      damageable.setDamage(Math.max(0, damage));
    }
    return this;
  }

  public ItemBuilder durability(int durability) {
    return damage(durability);
  }

  public ItemBuilder customModelData(Integer data) {
    if (meta != null) {
      meta.setCustomModelData(data);
    }
    return this;
  }

  // ==========================================
  // Skull Meta
  // ==========================================

  public ItemBuilder skullOwner(OfflinePlayer player) {
    if (meta instanceof SkullMeta skullMeta && player != null) {
      skullMeta.setOwningPlayer(player);
    }
    return this;
  }

  public ItemBuilder skullOwner(String playerName) {
    if (meta instanceof SkullMeta skullMeta && playerName != null) {
      skullMeta.setOwner(playerName);
    }
    return this;
  }

  public ItemBuilder skullTexture(String base64OrUrl) {
    if (meta instanceof SkullMeta skullMeta && base64OrUrl != null && !base64OrUrl.isEmpty()) {
      try {
        // 1. Try Paper PlayerProfile API
        Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
        java.lang.reflect.Method createProfileMethod =
            Bukkit.class.getMethod("createProfile", UUID.class);
        Object profile = createProfileMethod.invoke(null, UUID.randomUUID());

        Class<?> propertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
        java.lang.reflect.Constructor<?> propCtor =
            propertyClass.getConstructor(String.class, String.class);
        Object prop = propCtor.newInstance("textures", base64OrUrl);

        java.lang.reflect.Method setPropMethod =
            profileClass.getMethod("setProperty", propertyClass);
        setPropMethod.invoke(profile, prop);

        java.lang.reflect.Method setPlayerProfileMethod =
            SkullMeta.class.getMethod("setPlayerProfile", profileClass);
        setPlayerProfileMethod.invoke(skullMeta, profile);
      } catch (Exception e) {
        // 2. Fallback to Mojang GameProfile reflection
        try {
          Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
          java.lang.reflect.Constructor<?> gpCtor =
              gameProfileClass.getConstructor(UUID.class, String.class);
          Object gameProfile = gpCtor.newInstance(UUID.randomUUID(), "CustomSkull");

          Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
          java.lang.reflect.Constructor<?> propCtor =
              propertyClass.getConstructor(String.class, String.class);
          Object prop = propCtor.newInstance("textures", base64OrUrl);

          java.lang.reflect.Method getPropertiesMethod =
              gameProfileClass.getMethod("getProperties");
          Object propertyMap = getPropertiesMethod.invoke(gameProfile);
          java.lang.reflect.Method putMethod =
              propertyMap.getClass().getMethod("put", Object.class, Object.class);
          putMethod.invoke(propertyMap, "textures", prop);

          java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
          profileField.setAccessible(true);
          profileField.set(skullMeta, gameProfile);
        } catch (Exception ignored) {
        }
      }
    }
    return this;
  }

  // ==========================================
  // Leather Armor Color
  // ==========================================

  public ItemBuilder color(Color color) {
    if (meta instanceof LeatherArmorMeta leatherMeta && color != null) {
      leatherMeta.setColor(color);
    }
    return this;
  }

  public ItemBuilder color(int red, int green, int blue) {
    return color(Color.fromRGB(red, green, blue));
  }

  public ItemBuilder color(int rgbHex) {
    return color(Color.fromRGB(rgbHex));
  }

  // ==========================================
  // Potion Meta
  // ==========================================

  public ItemBuilder potionType(PotionType type) {
    if (meta instanceof PotionMeta potionMeta && type != null) {
      potionMeta.setBasePotionType(type);
    }
    return this;
  }

  public ItemBuilder potionColor(Color color) {
    if (meta instanceof PotionMeta potionMeta && color != null) {
      potionMeta.setColor(color);
    }
    return this;
  }

  public ItemBuilder addPotionEffect(PotionEffect effect) {
    return addPotionEffect(effect, true);
  }

  public ItemBuilder addPotionEffect(PotionEffect effect, boolean overwrite) {
    if (meta instanceof PotionMeta potionMeta && effect != null) {
      potionMeta.addCustomEffect(effect, overwrite);
    }
    return this;
  }

  // ==========================================
  // PersistentDataContainer (PDC)
  // ==========================================

  public ItemBuilder pdc(String key, String value) {
    if (meta != null && key != null && value != null) {
      meta.getPersistentDataContainer()
          .set(
              new NamespacedKey("javaskript", key.toLowerCase().replace(" ", "_")),
              PersistentDataType.STRING,
              value);
    }
    return this;
  }

  public ItemBuilder pdc(String key, int value) {
    if (meta != null && key != null) {
      meta.getPersistentDataContainer()
          .set(
              new NamespacedKey("javaskript", key.toLowerCase().replace(" ", "_")),
              PersistentDataType.INTEGER,
              value);
    }
    return this;
  }

  public ItemBuilder pdc(String key, double value) {
    if (meta != null && key != null) {
      meta.getPersistentDataContainer()
          .set(
              new NamespacedKey("javaskript", key.toLowerCase().replace(" ", "_")),
              PersistentDataType.DOUBLE,
              value);
    }
    return this;
  }

  public ItemBuilder pdc(String key, boolean value) {
    if (meta != null && key != null) {
      meta.getPersistentDataContainer()
          .set(
              new NamespacedKey("javaskript", key.toLowerCase().replace(" ", "_")),
              PersistentDataType.BYTE,
              (byte) (value ? 1 : 0));
    }
    return this;
  }

  public <T, Z> ItemBuilder pdc(String key, PersistentDataType<T, Z> type, Z value) {
    if (meta != null && key != null && type != null && value != null) {
      meta.getPersistentDataContainer()
          .set(new NamespacedKey("javaskript", key.toLowerCase().replace(" ", "_")), type, value);
    }
    return this;
  }

  public boolean hasPdc(String key) {
    if (meta == null || key == null) return false;
    return meta.getPersistentDataContainer()
        .has(new NamespacedKey("javaskript", key.toLowerCase().replace(" ", "_")));
  }

  // ==========================================
  // Generic Open Meta Mutator (Unlimited Extensibility)
  // ==========================================

  /** Mutate raw ItemMeta directly with a lambda for 100% Bukkit flexibility. */
  public ItemBuilder meta(Consumer<ItemMeta> consumer) {
    if (meta != null && consumer != null) {
      consumer.accept(meta);
    }
    return this;
  }

  /**
   * Mutate a specialized subtype of ItemMeta (e.g. BookMeta, BannerMeta, CrossbowMeta, ArmorMeta).
   */
  @SuppressWarnings("unchecked")
  public <M extends ItemMeta> ItemBuilder meta(Class<M> metaClass, Consumer<M> consumer) {
    if (meta != null && metaClass != null && metaClass.isInstance(meta) && consumer != null) {
      consumer.accept((M) meta);
    }
    return this;
  }

  // ==========================================
  // Build, Give & Clone
  // ==========================================

  public ItemStack build() {
    if (meta != null) {
      item.setItemMeta(meta);
    }
    return item.clone();
  }

  /**
   * Convenience method to directly give this item to a player, dropping it if inventory is full.
   */
  public ItemStack give(Player player) {
    ItemStack built = build();
    if (player != null && player.isOnline()) {
      Map<Integer, ItemStack> leftover = player.getInventory().addItem(built);
      for (ItemStack drop : leftover.values()) {
        player.getWorld().dropItemNaturally(player.getLocation(), drop);
      }
    }
    return built;
  }

  @Override
  public ItemBuilder clone() {
    return new ItemBuilder(this.build());
  }

  // ==========================================
  // Helper Utilities
  // ==========================================

  private static Component parseComponent(String input) {
    if (input == null || input.isEmpty()) return Component.empty();
    Component comp;
    if (input.contains("<") && input.contains(">")) {
      try {
        comp = MINI_MESSAGE.deserialize(input);
      } catch (Exception ignored) {
        comp = LEGACY_AMPERSAND.deserialize(input.replace('§', '&'));
      }
    } else {
      comp = LEGACY_AMPERSAND.deserialize(input.replace('§', '&'));
    }

    // Strip default Minecraft item italic styling unless explicitly configured
    if (!comp.hasDecoration(net.kyori.adventure.text.format.TextDecoration.ITALIC)) {
      comp = comp.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }
    return comp;
  }

  private static String extractUrlFromBase64(String base64) {
    try {
      String decoded = new String(Base64.getDecoder().decode(base64));
      int urlIndex = decoded.indexOf("\"url\":\"");
      if (urlIndex != -1) {
        int start = urlIndex + 7;
        int end = decoded.indexOf("\"", start);
        if (end != -1) {
          return decoded.substring(start, end);
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }
}
