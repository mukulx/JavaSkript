package dev.mukulx.javaskript.util;

import org.bukkit.Bukkit;

/** Utility class for server detection and compatibility checks. */
public class ServerUtil {

  // Memoized state flag to avoid reflection lookups on every single tick
  private static Boolean isFolia = null;

  /**
   * Check if the server is running Folia.
   *
   * @return true if running on Folia, false otherwise
   */
  public static boolean isFolia() {
    if (isFolia == null) {
      try {
        // Look up class signature for Folia's multi-threaded region manager
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
        isFolia = true;
      } catch (ClassNotFoundException e) {
        isFolia = false;
      }
    }
    return isFolia;
  }

  /**
   * Check if the server is running Paper (not Folia).
   *
   * @return true if running on Paper, false if Folia
   */
  public static boolean isPaper() {
    return !isFolia();
  }

  /**
   * Get the server type as a string.
   *
   * @return "Folia" or "Paper"
   */
  public static String getServerType() {
    return isFolia() ? "Folia" : "Paper";
  }

  /**
   * Get the server version.
   *
   * @return server version string
   */
  public static String getServerVersion() {
    return Bukkit.getVersion(); // Grabs implementation metadata from the server instance
  }

  /** Run a task on the main/global region thread (Folia-safe). */
  public static void runSync(org.bukkit.plugin.Plugin plugin, Runnable action) {
    if (isFolia()) {
      Bukkit.getGlobalRegionScheduler().run(plugin, task -> action.run());
    } else {
      Bukkit.getScheduler().runTask(plugin, action);
    }
  }

  /** Run a task later on the main/global region thread (Folia-safe, min 1 tick). */
  public static Object runLaterSync(
      org.bukkit.plugin.Plugin plugin, Runnable action, long delayTicks) {
    long delay = Math.max(1L, delayTicks);
    if (isFolia()) {
      return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> action.run(), delay);
    } else {
      return Bukkit.getScheduler().runTaskLater(plugin, action, delay);
    }
  }

  /** Run a repeating task on the main/global region thread (Folia-safe, min 1 tick period). */
  public static Object runTimerSync(
      org.bukkit.plugin.Plugin plugin, Runnable action, long delayTicks, long periodTicks) {
    long delay = Math.max(1L, delayTicks);
    long period = Math.max(1L, periodTicks);
    if (isFolia()) {
      return Bukkit.getGlobalRegionScheduler()
          .runAtFixedRate(plugin, task -> action.run(), delay, period);
    } else {
      return Bukkit.getScheduler().runTaskTimer(plugin, action, delay, period);
    }
  }

  /** Run a task off the game thread (Folia-safe, never touches world state). */
  public static void runAsync(org.bukkit.plugin.Plugin plugin, Runnable action) {
    if (isFolia()) {
      Bukkit.getAsyncScheduler().runNow(plugin, task -> action.run());
    } else {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, action);
    }
  }

  /** Run a task for a specific player on their region thread (Folia-safe). */
  public static void runForPlayer(
      org.bukkit.plugin.Plugin plugin, org.bukkit.entity.Player player, Runnable action) {
    if (player == null || action == null) return;
    if (isFolia()) {
      try {
        player.getScheduler().run(plugin, task -> action.run(), null);
        return;
      } catch (Throwable ignored) {
        // Fall through to global scheduler
      }
      Bukkit.getGlobalRegionScheduler().run(plugin, task -> action.run());
    } else {
      Bukkit.getScheduler().runTask(plugin, action);
    }
  }
}
