package dev.mukulx.javaskript.api.player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Static shorthand facade for {@link PlayerHelper}.
 *
 * <p>Allows developers to write 1-line player operations anywhere without injecting any fields:
 *
 * <pre>{@code
 * Players.msg(player, "<gradient:#ff5555:#ffaa00>Level Up!</gradient>");
 * Players.sound(player, Sound.ENTITY_PLAYER_LEVELUP);
 * Players.title(player, "<gold>VICTORY", "<yellow>You won the battle!");
 * Players.heal(player);
 * Players.feed(player);
 * }</pre>
 */
public final class Players {

  private static PlayerHelper instance;

  private Players() {}

  /** Initialize or update the underlying static {@link PlayerHelper} instance. */
  public static void setInstance(PlayerHelper helper) {
    instance = helper;
  }

  private static PlayerHelper get() {
    if (instance == null) {
      instance = new PlayerHelper(dev.mukulx.javaskript.JavaSkriptPlugin.getInstance());
    }
    return instance;
  }

  // ==========================================
  // Messaging & Audio-Visual
  // ==========================================

  public static Component parse(String input) {
    return get().parse(input);
  }

  public static void msg(CommandSender sender, String message) {
    get().msg(sender, message);
  }

  public static void msg(CommandSender sender, String... messages) {
    get().msg(sender, messages);
  }

  public static void broadcast(String message) {
    get().broadcast(message);
  }

  public static void broadcast(String message, String permission) {
    get().broadcast(message, permission);
  }

  public static void sound(Player player, Sound sound) {
    get().sound(player, sound);
  }

  public static void sound(Player player, Sound sound, float volume, float pitch) {
    get().sound(player, sound, volume, pitch);
  }

  public static void sound(Location location, Sound sound, float volume, float pitch) {
    get().sound(location, sound, volume, pitch);
  }

  public static void title(Player player, String title, String subtitle) {
    get().title(player, title, subtitle);
  }

  public static void title(
      Player player,
      String title,
      String subtitle,
      int fadeInTicks,
      int stayTicks,
      int fadeOutTicks) {
    get().title(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
  }

  public static void actionBar(Player player, String message) {
    get().actionBar(player, message);
  }

  // ==========================================
  // Stats, Inventory & Movement
  // ==========================================

  public static void heal(Player player) {
    get().heal(player);
  }

  public static void feed(Player player) {
    get().feed(player);
  }

  // ==========================================
  // Potion Effects & Status (Paper 1.21 & 26.2)
  // ==========================================

  public static PotionEffectType parseEffect(String name) {
    return get().parseEffect(name);
  }

  public static boolean isHarmful(PotionEffectType type) {
    return get().isHarmful(type);
  }

  public static void addEffect(
      Player player,
      PotionEffectType type,
      int durationSeconds,
      int amplifier,
      boolean ambient,
      boolean particles,
      boolean icon) {
    get().addEffect(player, type, durationSeconds, amplifier, ambient, particles, icon);
  }

  public static void addEffect(
      Player player, PotionEffectType type, int durationSeconds, int amplifier) {
    get().addEffect(player, type, durationSeconds, amplifier);
  }

  public static void addEffect(Player player, PotionEffectType type, int durationSeconds) {
    get().addEffect(player, type, durationSeconds);
  }

  public static void addEffect(
      Player player, String effectName, int durationSeconds, int amplifier) {
    get().addEffect(player, effectName, durationSeconds, amplifier);
  }

  public static void addEffect(Player player, String effectName, int durationSeconds) {
    get().addEffect(player, effectName, durationSeconds);
  }

  public static void addEffect(Player player, PotionEffect effect) {
    get().addEffect(player, effect);
  }

  public static void removeEffect(Player player, PotionEffectType type) {
    get().removeEffect(player, type);
  }

  public static void removeEffect(Player player, String effectName) {
    get().removeEffect(player, effectName);
  }

  public static void clearEffects(Player player) {
    get().clearEffects(player);
  }

  public static void clearNegativeEffects(Player player) {
    get().clearNegativeEffects(player);
  }

  public static boolean hasEffect(Player player, PotionEffectType type) {
    return get().hasEffect(player, type);
  }

  public static boolean hasEffect(Player player, String effectName) {
    return get().hasEffect(player, effectName);
  }

  public static Optional<PotionEffect> getEffect(Player player, PotionEffectType type) {
    return get().getEffect(player, type);
  }

  public static Collection<PotionEffect> getActiveEffects(Player player) {
    return get().getActiveEffects(player);
  }

  // --- Dedicated 1-Line Effect Helpers ---

  public static void speed(Player player, int seconds, int amplifier) {
    get().speed(player, seconds, amplifier);
  }

  public static void speed(Player player, int seconds) {
    get().speed(player, seconds);
  }

  public static void slowness(Player player, int seconds, int amplifier) {
    get().slowness(player, seconds, amplifier);
  }

  public static void slowness(Player player, int seconds) {
    get().slowness(player, seconds);
  }

  public static void haste(Player player, int seconds, int amplifier) {
    get().haste(player, seconds, amplifier);
  }

  public static void haste(Player player, int seconds) {
    get().haste(player, seconds);
  }

  public static void miningFatigue(Player player, int seconds, int amplifier) {
    get().miningFatigue(player, seconds, amplifier);
  }

  public static void miningFatigue(Player player, int seconds) {
    get().miningFatigue(player, seconds);
  }

  public static void strength(Player player, int seconds, int amplifier) {
    get().strength(player, seconds, amplifier);
  }

  public static void strength(Player player, int seconds) {
    get().strength(player, seconds);
  }

  public static void instantHealth(Player player, int amplifier) {
    get().instantHealth(player, amplifier);
  }

  public static void instantHealth(Player player) {
    get().instantHealth(player);
  }

  public static void instantDamage(Player player, int amplifier) {
    get().instantDamage(player, amplifier);
  }

  public static void instantDamage(Player player) {
    get().instantDamage(player);
  }

  public static void jumpBoost(Player player, int seconds, int amplifier) {
    get().jumpBoost(player, seconds, amplifier);
  }

  public static void jumpBoost(Player player, int seconds) {
    get().jumpBoost(player, seconds);
  }

  public static void nausea(Player player, int seconds, int amplifier) {
    get().nausea(player, seconds, amplifier);
  }

  public static void nausea(Player player, int seconds) {
    get().nausea(player, seconds);
  }

  public static void regeneration(Player player, int seconds, int amplifier) {
    get().regeneration(player, seconds, amplifier);
  }

  public static void regeneration(Player player, int seconds) {
    get().regeneration(player, seconds);
  }

  public static void resistance(Player player, int seconds, int amplifier) {
    get().resistance(player, seconds, amplifier);
  }

  public static void resistance(Player player, int seconds) {
    get().resistance(player, seconds);
  }

  public static void fireResistance(Player player, int seconds) {
    get().fireResistance(player, seconds);
  }

  public static void waterBreathing(Player player, int seconds) {
    get().waterBreathing(player, seconds);
  }

  public static void invisibility(Player player, int seconds) {
    get().invisibility(player, seconds);
  }

  public static void blindness(Player player, int seconds) {
    get().blindness(player, seconds);
  }

  public static void nightVision(Player player, int seconds) {
    get().nightVision(player, seconds);
  }

  public static void hunger(Player player, int seconds, int amplifier) {
    get().hunger(player, seconds, amplifier);
  }

  public static void hunger(Player player, int seconds) {
    get().hunger(player, seconds);
  }

  public static void weakness(Player player, int seconds, int amplifier) {
    get().weakness(player, seconds, amplifier);
  }

  public static void weakness(Player player, int seconds) {
    get().weakness(player, seconds);
  }

  public static void poison(Player player, int seconds, int amplifier) {
    get().poison(player, seconds, amplifier);
  }

  public static void poison(Player player, int seconds) {
    get().poison(player, seconds);
  }

  public static void wither(Player player, int seconds, int amplifier) {
    get().wither(player, seconds, amplifier);
  }

  public static void wither(Player player, int seconds) {
    get().wither(player, seconds);
  }

  public static void healthBoost(Player player, int seconds, int amplifier) {
    get().healthBoost(player, seconds, amplifier);
  }

  public static void healthBoost(Player player, int seconds) {
    get().healthBoost(player, seconds);
  }

  public static void absorption(Player player, int seconds, int amplifier) {
    get().absorption(player, seconds, amplifier);
  }

  public static void absorption(Player player, int seconds) {
    get().absorption(player, seconds);
  }

  public static void saturation(Player player, int seconds, int amplifier) {
    get().saturation(player, seconds, amplifier);
  }

  public static void saturation(Player player, int seconds) {
    get().saturation(player, seconds);
  }

  public static void glowing(Player player, int seconds) {
    get().glowing(player, seconds);
  }

  public static void levitation(Player player, int seconds, int amplifier) {
    get().levitation(player, seconds, amplifier);
  }

  public static void levitation(Player player, int seconds) {
    get().levitation(player, seconds);
  }

  public static void luck(Player player, int seconds, int amplifier) {
    get().luck(player, seconds, amplifier);
  }

  public static void luck(Player player, int seconds) {
    get().luck(player, seconds);
  }

  public static void unluck(Player player, int seconds, int amplifier) {
    get().unluck(player, seconds, amplifier);
  }

  public static void unluck(Player player, int seconds) {
    get().unluck(player, seconds);
  }

  public static void slowFalling(Player player, int seconds) {
    get().slowFalling(player, seconds);
  }

  public static void conduitPower(Player player, int seconds) {
    get().conduitPower(player, seconds);
  }

  public static void dolphinsGrace(Player player, int seconds) {
    get().dolphinsGrace(player, seconds);
  }

  public static void badOmen(Player player, int seconds, int amplifier) {
    get().badOmen(player, seconds, amplifier);
  }

  public static void badOmen(Player player, int seconds) {
    get().badOmen(player, seconds);
  }

  public static void heroOfTheVillage(Player player, int seconds, int amplifier) {
    get().heroOfTheVillage(player, seconds, amplifier);
  }

  public static void heroOfTheVillage(Player player, int seconds) {
    get().heroOfTheVillage(player, seconds);
  }

  public static void darkness(Player player, int seconds) {
    get().darkness(player, seconds);
  }

  // --- 1.21 Trial Chambers Effects ---

  public static void trialOmen(Player player, int seconds, int amplifier) {
    get().trialOmen(player, seconds, amplifier);
  }

  public static void trialOmen(Player player, int seconds) {
    get().trialOmen(player, seconds);
  }

  public static void raidOmen(Player player, int seconds, int amplifier) {
    get().raidOmen(player, seconds, amplifier);
  }

  public static void raidOmen(Player player, int seconds) {
    get().raidOmen(player, seconds);
  }

  public static void windCharged(Player player, int seconds) {
    get().windCharged(player, seconds);
  }

  public static void weaving(Player player, int seconds) {
    get().weaving(player, seconds);
  }

  public static void oozing(Player player, int seconds) {
    get().oozing(player, seconds);
  }

  public static void infested(Player player, int seconds) {
    get().infested(player, seconds);
  }

  // --- Paper 26.2 Effect ---

  public static void breathOfTheNautilus(Player player, int seconds, int amplifier) {
    get().breathOfTheNautilus(player, seconds, amplifier);
  }

  public static void breathOfTheNautilus(Player player, int seconds) {
    get().breathOfTheNautilus(player, seconds);
  }

  public static void clearInventory(Player player) {
    get().clearInventory(player);
  }

  public static void give(Player player, ItemStack... items) {
    get().give(player, items);
  }

  public static void lightning(Player player) {
    get().lightning(player);
  }

  public static void lightning(Location location) {
    get().lightning(location);
  }

  public static void lightningEffect(Player player) {
    get().lightningEffect(player);
  }

  public static void lightningEffect(Location location) {
    get().lightningEffect(location);
  }

  public static void teleport(Player player, Location location) {
    get().teleport(player, location);
  }

  public static void teleport(Player player, Entity target) {
    get().teleport(player, target);
  }

  public static void teleportSpawn(Player player) {
    get().teleportSpawn(player);
  }

  public static boolean hasPermission(Player player, String permission) {
    return get().hasPermission(player, permission);
  }

  // ==========================================
  // Queries
  // ==========================================

  public static Collection<? extends Player> all() {
    return get().all();
  }

  public static Collection<? extends Player> online() {
    return get().online();
  }

  public static Optional<Player> random() {
    return get().random();
  }

  public static Optional<Player> find(String nameOrUuid) {
    return get().find(nameOrUuid);
  }

  public static List<Player> nearby(Location center, double radius) {
    return get().nearby(center, radius);
  }
}
