package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;

/**
 * Static shorthand facade for managing player and global cooldowns.
 *
 * <p>Enables 1-line cooldown operations anywhere without injecting {@link CooldownHelper}:
 *
 * <pre>{@code
 * if (Cooldowns.has(player, "dash")) {
 *     Players.msg(player, "<red>Please wait " + Cooldowns.remainingSeconds(player, "dash") + "s!");
 *     return;
 * }
 * Cooldowns.set(player, "dash", Duration.ofSeconds(5));
 * }</pre>
 */
public final class Cooldowns {

  private static CooldownHelper helper;

  private Cooldowns() {}

  private static CooldownHelper get() {
    if (helper == null) {
      helper = new CooldownHelper(JavaSkriptPlugin.getInstance(), "static_global");
    }
    return helper;
  }

  public static void setInstance(CooldownHelper h) {
    helper = h;
  }

  /** Set a player cooldown using Duration. */
  public static void set(Player player, String key, Duration duration) {
    get().set(player, key, duration);
  }

  /** Set a player cooldown using time and TimeUnit. */
  public static void set(Player player, String key, long time, TimeUnit unit) {
    get().set(player, key, time, unit);
  }

  /** Set a player cooldown in milliseconds. */
  public static void set(Player player, String key, long millis) {
    get().set(player, key, millis);
  }

  /** Set a UUID cooldown using Duration. */
  public static void set(UUID uuid, String key, Duration duration) {
    get().set(uuid, key, duration);
  }

  /** Set a UUID cooldown in milliseconds. */
  public static void set(UUID uuid, String key, long millis) {
    get().set(uuid, key, millis);
  }

  /** Check if a player is currently on cooldown. */
  public static boolean has(Player player, String key) {
    return get().isOnCooldown(player, key);
  }

  /** Check if a UUID is currently on cooldown. */
  public static boolean has(UUID uuid, String key) {
    return get().isOnCooldown(uuid, key);
  }

  /** Alias for {@link #has(Player, String)}. */
  public static boolean isOnCooldown(Player player, String key) {
    return get().isOnCooldown(player, key);
  }

  /** Alias for {@link #has(UUID, String)}. */
  public static boolean isOnCooldown(UUID uuid, String key) {
    return get().isOnCooldown(uuid, key);
  }

  /** Get remaining cooldown in milliseconds. */
  public static long remainingMillis(Player player, String key) {
    return get().getRemainingMillis(player, key);
  }

  /** Get remaining cooldown in milliseconds. */
  public static long remainingMillis(UUID uuid, String key) {
    return get().getRemainingMillis(uuid, key);
  }

  /** Get remaining cooldown in decimal seconds (e.g. 4.2s). */
  public static double remainingSeconds(Player player, String key) {
    return get().getRemainingSeconds(player, key);
  }

  /** Get remaining cooldown in decimal seconds. */
  public static double remainingSeconds(UUID uuid, String key) {
    return get().getRemainingSeconds(uuid, key);
  }

  /** Get normalized cooldown progress (0.0 to 1.0). */
  public static double progress(Player player, String key) {
    return get().getProgress(player, key);
  }

  /** Clear/reset a player's cooldown. */
  public static void clear(Player player, String key) {
    get().reset(player, key);
  }

  /** Clear/reset a UUID's cooldown. */
  public static void clear(UUID uuid, String key) {
    get().reset(uuid, key);
  }

  /** Reset a player's cooldown. */
  public static void reset(Player player, String key) {
    get().reset(player, key);
  }

  /** Reset a UUID's cooldown. */
  public static void reset(UUID uuid, String key) {
    get().reset(uuid, key);
  }

  /** Clear/reset all cooldowns for a player. */
  public static void clearAll(Player player) {
    get().resetAll(player);
  }

  /** Clear/reset all cooldowns for a UUID. */
  public static void clearAll(UUID uuid) {
    get().resetAll(uuid);
  }

  /** Reset all cooldowns for a player. */
  public static void resetAll(Player player) {
    get().resetAll(player);
  }

  /** Reset all cooldowns for a UUID. */
  public static void resetAll(UUID uuid) {
    get().resetAll(uuid);
  }

  /** Show an animated visual Action Bar progress bar ticker for this cooldown. */
  public static void showTicker(Player player, String key, String displayName) {
    get().startActionBarTicker(player, key, displayName);
  }

  /** Show an animated visual Action Bar progress bar ticker for this cooldown. */
  public static void startActionBarTicker(Player player, String key, String displayName) {
    get().startActionBarTicker(player, key, displayName);
  }
}
