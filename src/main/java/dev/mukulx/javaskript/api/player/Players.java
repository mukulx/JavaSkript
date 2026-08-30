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
