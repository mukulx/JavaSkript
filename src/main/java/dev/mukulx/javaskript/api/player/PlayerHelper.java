package dev.mukulx.javaskript.api.player;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Universal, expressive Player utility for JavaSkript.
 *
 * <p>Streamlines player communication, audio-visual feedback, stats manipulation, potion effects,
 * inventory handing, and spatial queries into clean 1-line operations.
 *
 * <p>Usable both via injection ({@code private PlayerHelper players;}) and statically ({@link
 * Players#msg(Player, String)}).
 */
public class PlayerHelper {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.builder().character('&').hexColors().build();

  private static final Map<String, PotionEffectType> EFFECT_CACHE = new ConcurrentHashMap<>();

  private static final Set<String> NEGATIVE_POTION_EFFECT_NAMES =
      Set.of(
          "POISON",
          "WITHER",
          "SLOWNESS",
          "SLOW",
          "BLINDNESS",
          "WEAKNESS",
          "NAUSEA",
          "CONFUSION",
          "DARKNESS",
          "MINING_FATIGUE",
          "SLOW_DIGGING",
          "HUNGER",
          "LEVITATION",
          "UNLUCK",
          "BAD_OMEN",
          "TRIAL_OMEN",
          "RAID_OMEN",
          "WIND_CHARGED",
          "WEAVING",
          "OOZING",
          "INFESTED",
          "INSTANT_DAMAGE",
          "HARM");

  private static final Random RANDOM = new Random();
  private final JavaSkriptPlugin plugin;

  public PlayerHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  // ==========================================
  // Messaging & Formatting
  // ==========================================

  /**
   * Parse formatted text with support for MiniMessage tags and legacy ampersand codes. Default
   * italics are disabled.
   */
  public Component parse(String input) {
    return dev.mukulx.javaskript.util.TextUtil.parse(input);
  }

  /** Send formatted message to a player or command sender. */
  public void msg(CommandSender sender, String message) {
    if (sender != null && message != null) {
      sender.sendMessage(parse(message));
    }
  }

  /** Send multiple formatted messages to a sender. */
  public void msg(CommandSender sender, String... messages) {
    if (sender != null && messages != null) {
      for (String m : messages) {
        msg(sender, m);
      }
    }
  }

  /** Broadcast a formatted message to all online players and console. */
  public void broadcast(String message) {
    if (message == null) return;
    Component comp = parse(message);
    Bukkit.broadcast(comp);
  }

  /** Broadcast a formatted message to all online players with a specific permission. */
  public void broadcast(String message, String permission) {
    if (message == null || permission == null) return;
    Component comp = parse(message);
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (p.hasPermission(permission)) {
        p.sendMessage(comp);
      }
    }
    Bukkit.getConsoleSender().sendMessage(comp);
  }

  // ==========================================
  // Audio & Sounds
  // ==========================================

  /** Play a sound to a player with default volume (1.0f) and pitch (1.0f). */
  public void sound(Player player, Sound sound) {
    sound(player, sound, 1.0f, 1.0f);
  }

  /** Play a sound to a player with specified volume and pitch. */
  public void sound(Player player, Sound sound, float volume, float pitch) {
    if (player != null && player.isOnline() && sound != null) {
      player.playSound(player.getLocation(), sound, volume, pitch);
    }
  }

  /** Play a sound at a world location to all nearby players. */
  public void sound(Location location, Sound sound, float volume, float pitch) {
    if (location != null && location.getWorld() != null && sound != null) {
      location.getWorld().playSound(location, sound, volume, pitch);
    }
  }

  // ==========================================
  // Titles & ActionBars
  // ==========================================

  /** Send title and subtitle with standard duration (0.5s fade in, 2.5s stay, 0.5s fade out). */
  public void title(Player player, String title, String subtitle) {
    title(player, title, subtitle, 10, 50, 10);
  }

  /** Send title and subtitle with precise tick timings. */
  public void title(
      Player player,
      String title,
      String subtitle,
      int fadeInTicks,
      int stayTicks,
      int fadeOutTicks) {
    if (player == null || !player.isOnline()) return;

    Component titleComp = parse(title);
    Component subComp = parse(subtitle);

    Title.Times times =
        Title.Times.times(
            Duration.ofMillis(fadeInTicks * 50L),
            Duration.ofMillis(stayTicks * 50L),
            Duration.ofMillis(fadeOutTicks * 50L));

    player.showTitle(Title.title(titleComp, subComp, times));
  }

  /** Send an action bar message to a player. */
  public void actionBar(Player player, String message) {
    if (player != null && player.isOnline() && message != null) {
      player.sendActionBar(parse(message));
    }
  }

  // ==========================================
  // Stats, Healing & Feeding
  // ==========================================

  /** Fully restore player health, extinguish fire, and clear negative potion effects. */
  public void heal(Player player) {
    if (player == null || !player.isOnline()) return;

    double maxHealth = 20.0;
    var attr = player.getAttribute(Attribute.MAX_HEALTH);
    if (attr != null) {
      maxHealth = attr.getValue();
    }
    player.setHealth(maxHealth);
    player.setFireTicks(0);

    clearNegativeEffects(player);
  }

  /** Fully restore player hunger and saturation. */
  public void feed(Player player) {
    if (player == null || !player.isOnline()) return;
    player.setFoodLevel(20);
    player.setSaturation(20.0f);
  }

  // ==========================================
  // Potion Effects & Status (Paper 1.21 & 26.2)
  // ==========================================

  /**
   * Parse a potion effect type from string name, namespace, or alias. Results are cached in-memory
   * for sub-microsecond lookups.
   */
  @SuppressWarnings("deprecation")
  public PotionEffectType parseEffect(String name) {
    if (name == null || name.isBlank()) return null;
    String clean = name.trim().toLowerCase();
    if (clean.startsWith("minecraft:")) {
      clean = clean.substring("minecraft:".length());
    }

    return EFFECT_CACHE.computeIfAbsent(
        clean,
        k -> {
          // 1. Direct NamespacedKey via Registry
          NamespacedKey key = NamespacedKey.minecraft(k);
          PotionEffectType fromRegistry = Registry.POTION_EFFECT_TYPE.get(key);
          if (fromRegistry != null) return fromRegistry;

          // 2. Bukkit legacy / alias mapping
          String enumName = k.toUpperCase().replace("-", "_");
          try {
            PotionEffectType fromName = PotionEffectType.getByName(enumName);
            if (fromName != null) return fromName;
          } catch (Throwable ignored) {
          }

          // 3. User friendly aliases (including 1.21 & Paper 26.2)
          return switch (k) {
            case "speed" -> PotionEffectType.SPEED;
            case "slow", "slowness" -> PotionEffectType.SLOWNESS;
            case "haste", "fast_digging" -> PotionEffectType.HASTE;
            case "fatigue", "mining_fatigue", "slow_digging" -> PotionEffectType.MINING_FATIGUE;
            case "strength", "increase_damage" -> PotionEffectType.STRENGTH;
            case "heal", "instant_health", "instant_heal" -> PotionEffectType.INSTANT_HEALTH;
            case "harm", "instant_damage", "damage" -> PotionEffectType.INSTANT_DAMAGE;
            case "jump", "jump_boost", "leap" -> PotionEffectType.JUMP_BOOST;
            case "nausea", "confusion" -> PotionEffectType.NAUSEA;
            case "regen", "regeneration" -> PotionEffectType.REGENERATION;
            case "resistance", "damage_resistance" -> PotionEffectType.RESISTANCE;
            case "fire_res", "fire_resistance" -> PotionEffectType.FIRE_RESISTANCE;
            case "water_breathing", "breath" -> PotionEffectType.WATER_BREATHING;
            case "invis", "invisibility" -> PotionEffectType.INVISIBILITY;
            case "blind", "blindness" -> PotionEffectType.BLINDNESS;
            case "night_vision", "vision" -> PotionEffectType.NIGHT_VISION;
            case "hunger", "starve" -> PotionEffectType.HUNGER;
            case "weak", "weakness" -> PotionEffectType.WEAKNESS;
            case "poison" -> PotionEffectType.POISON;
            case "wither" -> PotionEffectType.WITHER;
            case "health_boost", "extra_health" -> PotionEffectType.HEALTH_BOOST;
            case "absorb", "absorption" -> PotionEffectType.ABSORPTION;
            case "saturation", "food" -> PotionEffectType.SATURATION;
            case "glow", "glowing" -> PotionEffectType.GLOWING;
            case "levitate", "levitation" -> PotionEffectType.LEVITATION;
            case "luck" -> PotionEffectType.LUCK;
            case "unluck", "bad_luck" -> PotionEffectType.UNLUCK;
            case "slow_fall", "slow_falling" -> PotionEffectType.SLOW_FALLING;
            case "conduit", "conduit_power" -> PotionEffectType.CONDUIT_POWER;
            case "dolphin", "dolphins_grace", "dolphin_grace" -> PotionEffectType.DOLPHINS_GRACE;
            case "bad_omen", "omen" -> PotionEffectType.BAD_OMEN;
            case "hero", "hero_of_the_village" -> PotionEffectType.HERO_OF_THE_VILLAGE;
            case "darkness", "dark" -> PotionEffectType.DARKNESS;
            case "trial_omen" -> PotionEffectType.TRIAL_OMEN;
            case "raid_omen" -> PotionEffectType.RAID_OMEN;
            case "wind_charged", "wind" -> PotionEffectType.WIND_CHARGED;
            case "weaving", "cobweb" -> PotionEffectType.WEAVING;
            case "oozing", "slime" -> PotionEffectType.OOZING;
            case "infested", "silverfish" -> PotionEffectType.INFESTED;
            case "nautilus", "breath_of_the_nautilus" ->
                Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("breath_of_the_nautilus"));
            default -> null;
          };
        });
  }

  /** Check if a potion effect is considered harmful / negative. */
  public boolean isHarmful(PotionEffectType type) {
    if (type == null) return false;
    try {
      if (type.getEffectCategory() == PotionEffectType.Category.HARMFUL) {
        return true;
      }
    } catch (Throwable ignored) {
    }
    String key = type.getKey().getKey().toUpperCase();
    return NEGATIVE_POTION_EFFECT_NAMES.contains(key);
  }

  /** Apply a potion effect to a player with full customization. */
  public void addEffect(
      Player player,
      PotionEffectType type,
      int durationSeconds,
      int amplifier,
      boolean ambient,
      boolean particles,
      boolean icon) {
    if (player == null || !player.isOnline() || type == null) return;
    int durationTicks =
        (durationSeconds <= -1 || durationSeconds == Integer.MAX_VALUE)
            ? PotionEffect.INFINITE_DURATION
            : Math.max(1, durationSeconds * 20);
    player.addPotionEffect(
        new PotionEffect(type, durationTicks, Math.max(0, amplifier), ambient, particles, icon));
  }

  /** Apply a potion effect to a player with particles and icon enabled. */
  public void addEffect(Player player, PotionEffectType type, int durationSeconds, int amplifier) {
    addEffect(player, type, durationSeconds, amplifier, false, true, true);
  }

  /** Apply a level 1 (amplifier 0) potion effect to a player. */
  public void addEffect(Player player, PotionEffectType type, int durationSeconds) {
    addEffect(player, type, durationSeconds, 0);
  }

  /** Apply a potion effect by string name or alias. */
  public void addEffect(Player player, String effectName, int durationSeconds, int amplifier) {
    PotionEffectType type = parseEffect(effectName);
    if (type != null) {
      addEffect(player, type, durationSeconds, amplifier);
    }
  }

  /** Apply a level 1 potion effect by string name or alias. */
  public void addEffect(Player player, String effectName, int durationSeconds) {
    addEffect(player, effectName, durationSeconds, 0);
  }

  /** Apply a custom {@link PotionEffect} instance. */
  public void addEffect(Player player, PotionEffect effect) {
    if (player != null && player.isOnline() && effect != null) {
      player.addPotionEffect(effect);
    }
  }

  /** Remove a specific potion effect from a player. */
  public void removeEffect(Player player, PotionEffectType type) {
    if (player != null && player.isOnline() && type != null) {
      player.removePotionEffect(type);
    }
  }

  /** Remove a potion effect by string name or alias from a player. */
  public void removeEffect(Player player, String effectName) {
    PotionEffectType type = parseEffect(effectName);
    if (type != null) {
      removeEffect(player, type);
    }
  }

  /** Clear ALL active potion effects from a player. */
  public void clearEffects(Player player) {
    if (player == null || !player.isOnline()) return;
    for (PotionEffect effect : player.getActivePotionEffects()) {
      if (effect != null && effect.getType() != null) {
        player.removePotionEffect(effect.getType());
      }
    }
  }

  /** Clear all harmful / negative potion effects from a player (including 1.21 & 26.2 effects). */
  public void clearNegativeEffects(Player player) {
    if (player == null || !player.isOnline()) return;
    for (PotionEffect effect : player.getActivePotionEffects()) {
      if (effect != null && effect.getType() != null && isHarmful(effect.getType())) {
        player.removePotionEffect(effect.getType());
      }
    }
  }

  /** Check if a player currently has an active potion effect. */
  public boolean hasEffect(Player player, PotionEffectType type) {
    return player != null && player.isOnline() && type != null && player.hasPotionEffect(type);
  }

  /** Check if a player currently has an active potion effect by string name or alias. */
  public boolean hasEffect(Player player, String effectName) {
    PotionEffectType type = parseEffect(effectName);
    return type != null && hasEffect(player, type);
  }

  /** Get an active potion effect on a player. */
  public Optional<PotionEffect> getEffect(Player player, PotionEffectType type) {
    if (player == null || !player.isOnline() || type == null) return Optional.empty();
    return Optional.ofNullable(player.getPotionEffect(type));
  }

  /** Get all active potion effects on a player. */
  public Collection<PotionEffect> getActiveEffects(Player player) {
    if (player == null || !player.isOnline()) return Collections.emptyList();
    return player.getActivePotionEffects();
  }

  // --- Dedicated 1-Line Effect Helpers (All Paper 1.21 & 26.2 Effects) ---

  public void speed(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.SPEED, seconds, amplifier);
  }

  public void speed(Player player, int seconds) {
    speed(player, seconds, 0);
  }

  public void slowness(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.SLOWNESS, seconds, amplifier);
  }

  public void slowness(Player player, int seconds) {
    slowness(player, seconds, 0);
  }

  public void haste(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.HASTE, seconds, amplifier);
  }

  public void haste(Player player, int seconds) {
    haste(player, seconds, 0);
  }

  public void miningFatigue(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.MINING_FATIGUE, seconds, amplifier);
  }

  public void miningFatigue(Player player, int seconds) {
    miningFatigue(player, seconds, 0);
  }

  public void strength(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.STRENGTH, seconds, amplifier);
  }

  public void strength(Player player, int seconds) {
    strength(player, seconds, 0);
  }

  public void instantHealth(Player player, int amplifier) {
    addEffect(player, PotionEffectType.INSTANT_HEALTH, 1, amplifier);
  }

  public void instantHealth(Player player) {
    instantHealth(player, 0);
  }

  public void instantDamage(Player player, int amplifier) {
    addEffect(player, PotionEffectType.INSTANT_DAMAGE, 1, amplifier);
  }

  public void instantDamage(Player player) {
    instantDamage(player, 0);
  }

  public void jumpBoost(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.JUMP_BOOST, seconds, amplifier);
  }

  public void jumpBoost(Player player, int seconds) {
    jumpBoost(player, seconds, 0);
  }

  public void nausea(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.NAUSEA, seconds, amplifier);
  }

  public void nausea(Player player, int seconds) {
    nausea(player, seconds, 0);
  }

  public void regeneration(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.REGENERATION, seconds, amplifier);
  }

  public void regeneration(Player player, int seconds) {
    regeneration(player, seconds, 0);
  }

  public void resistance(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.RESISTANCE, seconds, amplifier);
  }

  public void resistance(Player player, int seconds) {
    resistance(player, seconds, 0);
  }

  public void fireResistance(Player player, int seconds) {
    addEffect(player, PotionEffectType.FIRE_RESISTANCE, seconds, 0);
  }

  public void waterBreathing(Player player, int seconds) {
    addEffect(player, PotionEffectType.WATER_BREATHING, seconds, 0);
  }

  public void invisibility(Player player, int seconds) {
    addEffect(player, PotionEffectType.INVISIBILITY, seconds, 0);
  }

  public void blindness(Player player, int seconds) {
    addEffect(player, PotionEffectType.BLINDNESS, seconds, 0);
  }

  public void nightVision(Player player, int seconds) {
    addEffect(player, PotionEffectType.NIGHT_VISION, seconds, 0);
  }

  public void hunger(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.HUNGER, seconds, amplifier);
  }

  public void hunger(Player player, int seconds) {
    hunger(player, seconds, 0);
  }

  public void weakness(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.WEAKNESS, seconds, amplifier);
  }

  public void weakness(Player player, int seconds) {
    weakness(player, seconds, 0);
  }

  public void poison(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.POISON, seconds, amplifier);
  }

  public void poison(Player player, int seconds) {
    poison(player, seconds, 0);
  }

  public void wither(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.WITHER, seconds, amplifier);
  }

  public void wither(Player player, int seconds) {
    wither(player, seconds, 0);
  }

  public void healthBoost(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.HEALTH_BOOST, seconds, amplifier);
  }

  public void healthBoost(Player player, int seconds) {
    healthBoost(player, seconds, 0);
  }

  public void absorption(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.ABSORPTION, seconds, amplifier);
  }

  public void absorption(Player player, int seconds) {
    absorption(player, seconds, 0);
  }

  public void saturation(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.SATURATION, seconds, amplifier);
  }

  public void saturation(Player player, int seconds) {
    saturation(player, seconds, 0);
  }

  public void glowing(Player player, int seconds) {
    addEffect(player, PotionEffectType.GLOWING, seconds, 0);
  }

  public void levitation(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.LEVITATION, seconds, amplifier);
  }

  public void levitation(Player player, int seconds) {
    levitation(player, seconds, 0);
  }

  public void luck(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.LUCK, seconds, amplifier);
  }

  public void luck(Player player, int seconds) {
    luck(player, seconds, 0);
  }

  public void unluck(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.UNLUCK, seconds, amplifier);
  }

  public void unluck(Player player, int seconds) {
    unluck(player, seconds, 0);
  }

  public void slowFalling(Player player, int seconds) {
    addEffect(player, PotionEffectType.SLOW_FALLING, seconds, 0);
  }

  public void conduitPower(Player player, int seconds) {
    addEffect(player, PotionEffectType.CONDUIT_POWER, seconds, 0);
  }

  public void dolphinsGrace(Player player, int seconds) {
    addEffect(player, PotionEffectType.DOLPHINS_GRACE, seconds, 0);
  }

  public void badOmen(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.BAD_OMEN, seconds, amplifier);
  }

  public void badOmen(Player player, int seconds) {
    badOmen(player, seconds, 0);
  }

  public void heroOfTheVillage(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.HERO_OF_THE_VILLAGE, seconds, amplifier);
  }

  public void heroOfTheVillage(Player player, int seconds) {
    heroOfTheVillage(player, seconds, 0);
  }

  public void darkness(Player player, int seconds) {
    addEffect(player, PotionEffectType.DARKNESS, seconds, 0);
  }

  // --- 1.21 Trial Chambers Effects ---

  public void trialOmen(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.TRIAL_OMEN, seconds, amplifier);
  }

  public void trialOmen(Player player, int seconds) {
    trialOmen(player, seconds, 0);
  }

  public void raidOmen(Player player, int seconds, int amplifier) {
    addEffect(player, PotionEffectType.RAID_OMEN, seconds, amplifier);
  }

  public void raidOmen(Player player, int seconds) {
    raidOmen(player, seconds, 0);
  }

  public void windCharged(Player player, int seconds) {
    addEffect(player, PotionEffectType.WIND_CHARGED, seconds, 0);
  }

  public void weaving(Player player, int seconds) {
    addEffect(player, PotionEffectType.WEAVING, seconds, 0);
  }

  public void oozing(Player player, int seconds) {
    addEffect(player, PotionEffectType.OOZING, seconds, 0);
  }

  public void infested(Player player, int seconds) {
    addEffect(player, PotionEffectType.INFESTED, seconds, 0);
  }

  // --- Paper 26.2 Effect ---

  public void breathOfTheNautilus(Player player, int seconds, int amplifier) {
    PotionEffectType type = parseEffect("breath_of_the_nautilus");
    if (type != null) {
      addEffect(player, type, seconds, amplifier);
    }
  }

  public void breathOfTheNautilus(Player player, int seconds) {
    breathOfTheNautilus(player, seconds, 0);
  }

  /** Clear a player's inventory, armor, and offhand slots. */
  public void clearInventory(Player player) {
    if (player != null && player.isOnline()) {
      player.getInventory().clear();
      player.getInventory().setArmorContents(null);
      player.getInventory().setItemInOffHand(null);
    }
  }

  /**
   * Safely give items to a player, dropping any items naturally on the ground if inventory is full.
   */
  public void give(Player player, ItemStack... items) {
    if (player == null || !player.isOnline() || items == null) return;
    for (ItemStack item : items) {
      if (item == null || item.getType().isAir()) continue;
      Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
      for (ItemStack drop : leftover.values()) {
        player.getWorld().dropItemNaturally(player.getLocation(), drop);
      }
    }
  }

  // ==========================================
  // World Interactions & Lightning
  // ==========================================

  /** Strike real lightning at player's location. */
  public void lightning(Player player) {
    if (player != null && player.isOnline()) {
      lightning(player.getLocation());
    }
  }

  /** Strike real lightning at a location. */
  public void lightning(Location location) {
    if (location != null && location.getWorld() != null) {
      location.getWorld().strikeLightning(location);
    }
  }

  /** Strike cosmetic visual lightning (no damage, no fire) at a player's location. */
  public void lightningEffect(Player player) {
    if (player != null && player.isOnline()) {
      lightningEffect(player.getLocation());
    }
  }

  /** Strike cosmetic visual lightning (no damage, no fire) at a location. */
  public void lightningEffect(Location location) {
    if (location != null && location.getWorld() != null) {
      location.getWorld().strikeLightningEffect(location);
    }
  }

  /** Teleport player safely (Folia-compatible async teleport). */
  public void teleport(Player player, Location location) {
    if (player == null || !player.isOnline() || location == null) return;
    try {
      player.teleportAsync(location);
    } catch (Throwable t) {
      player.teleport(location);
    }
  }

  /** Teleport player to another entity's location. */
  public void teleport(Player player, Entity target) {
    if (target != null) {
      teleport(player, target.getLocation());
    }
  }

  /** Teleport player to their world spawn. */
  public void teleportSpawn(Player player) {
    if (player != null && player.isOnline()) {
      teleport(player, player.getWorld().getSpawnLocation());
    }
  }

  // ==========================================
  // Queries & Lookups
  // ==========================================

  /** Check if a player has permission, or is OP. */
  public boolean hasPermission(Player player, String permission) {
    return player != null && (player.isOp() || player.hasPermission(permission));
  }

  /** Get all online players. */
  public Collection<? extends Player> all() {
    return Bukkit.getOnlinePlayers();
  }

  /** Pick a random online player. */
  public Optional<Player> random() {
    List<? extends Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
    if (online.isEmpty()) return Optional.empty();
    return Optional.of(online.get(RANDOM.nextInt(online.size())));
  }

  /** Find an online player by name or UUID string. */
  public Optional<Player> find(String nameOrUuid) {
    if (nameOrUuid == null || nameOrUuid.isBlank()) return Optional.empty();

    // 1. Try UUID
    try {
      UUID uuid = UUID.fromString(nameOrUuid.trim());
      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.isOnline()) return Optional.of(p);
    } catch (IllegalArgumentException ignored) {
    }

    // 2. Exact Name
    Player p = Bukkit.getPlayerExact(nameOrUuid.trim());
    if (p != null && p.isOnline()) return Optional.of(p);

    // 3. Match Name
    p = Bukkit.getPlayer(nameOrUuid.trim());
    if (p != null && p.isOnline()) return Optional.of(p);

    return Optional.empty();
  }

  /** Get all online players within a radius of a center location. */
  public List<Player> nearby(Location center, double radius) {
    if (center == null || center.getWorld() == null || radius <= 0) {
      return Collections.emptyList();
    }
    double radiusSq = radius * radius;
    List<Player> result = new ArrayList<>();
    for (Player p : center.getWorld().getPlayers()) {
      if (p != null && p.isOnline() && p.getWorld().equals(center.getWorld())) {
        if (p.getLocation().distanceSquared(center) <= radiusSq) {
          result.add(p);
        }
      }
    }
    return result;
  }
}
