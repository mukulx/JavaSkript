package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Thread-safe, memory-leak-free cooldown and rate-limiting helper.
 *
 * <p>Supports: - Per-player and global/system cooldowns - Precise millisecond, second, and Duration
 * queries - Normalized progress calculation (0.0 to 1.0) - Animated visual Action Bar progress bars
 * with auto-expiration - Automatic background purging of expired keys to prevent memory leaks
 */
public class CooldownHelper {

  private final JavaSkriptPlugin plugin;
  private final String scriptKey;
  private final Map<UUID, Map<String, CooldownEntry>> playerCooldowns = new ConcurrentHashMap<>();
  private final Map<String, CooldownEntry> globalCooldowns = new ConcurrentHashMap<>();
  private final Map<String, Object> activeTickerTasks = new ConcurrentHashMap<>();

  public CooldownHelper(JavaSkriptPlugin plugin, String scriptKey) {
    this.plugin = plugin;
    this.scriptKey = scriptKey != null ? scriptKey : "unknown";
  }

  // ==========================================
  // Player Cooldowns
  // ==========================================

  public void set(Player player, String key, Duration duration) {
    if (player == null) return;
    set(player.getUniqueId(), key, duration.toMillis());
  }

  public void set(Player player, String key, long time, TimeUnit unit) {
    if (player == null) return;
    set(player.getUniqueId(), key, unit.toMillis(time));
  }

  public void set(Player player, String key, long millis) {
    if (player == null) return;
    set(player.getUniqueId(), key, millis);
  }

  public void set(UUID uuid, String key, Duration duration) {
    set(uuid, key, duration.toMillis());
  }

  public void set(UUID uuid, String key, long millis) {
    if (uuid == null || key == null || millis <= 0) return;
    long now = System.currentTimeMillis();
    String normalizedKey = key.toLowerCase();
    playerCooldowns
        .computeIfAbsent(uuid, u -> new ConcurrentHashMap<>())
        .put(normalizedKey, new CooldownEntry(now, now + millis));
  }

  public boolean isOnCooldown(Player player, String key) {
    if (player == null) return false;
    return isOnCooldown(player.getUniqueId(), key);
  }

  public boolean isOnCooldown(UUID uuid, String key) {
    if (uuid == null || key == null) return false;
    Map<String, CooldownEntry> map = playerCooldowns.get(uuid);
    if (map == null) return false;
    String normalizedKey = key.toLowerCase();
    CooldownEntry entry = map.get(normalizedKey);
    if (entry == null) return false;
    if (entry.isExpired()) {
      map.remove(normalizedKey);
      if (map.isEmpty()) {
        playerCooldowns.remove(uuid);
      }
      return false;
    }
    return true;
  }

  public long getRemainingMillis(Player player, String key) {
    if (player == null) return 0;
    return getRemainingMillis(player.getUniqueId(), key);
  }

  public long getRemainingMillis(UUID uuid, String key) {
    if (uuid == null || key == null) return 0;
    Map<String, CooldownEntry> map = playerCooldowns.get(uuid);
    if (map == null) return 0;
    String normalizedKey = key.toLowerCase();
    CooldownEntry entry = map.get(normalizedKey);
    if (entry == null || entry.isExpired()) {
      map.remove(normalizedKey);
      return 0;
    }
    return entry.remainingMillis();
  }

  public long getRemainingSeconds(Player player, String key) {
    return Math.max(0, (getRemainingMillis(player, key) + 999) / 1000);
  }

  public long getRemainingSeconds(UUID uuid, String key) {
    return Math.max(0, (getRemainingMillis(uuid, key) + 999) / 1000);
  }

  public Duration getRemaining(Player player, String key) {
    return Duration.ofMillis(getRemainingMillis(player, key));
  }

  public Duration getRemaining(UUID uuid, String key) {
    return Duration.ofMillis(getRemainingMillis(uuid, key));
  }

  /**
   * Progress of the cooldown from 0.0 (just started) to 1.0 (completed).
   *
   * @return progress value between 0.0 and 1.0
   */
  public double getProgress(Player player, String key) {
    if (player == null) return 1.0;
    return getProgress(player.getUniqueId(), key);
  }

  public double getProgress(UUID uuid, String key) {
    if (uuid == null || key == null) return 1.0;
    Map<String, CooldownEntry> map = playerCooldowns.get(uuid);
    if (map == null) return 1.0;
    CooldownEntry entry = map.get(key.toLowerCase());
    if (entry == null) return 1.0;
    return entry.progress();
  }

  public void reset(Player player, String key) {
    if (player == null) return;
    reset(player.getUniqueId(), key);
  }

  public void reset(UUID uuid, String key) {
    if (uuid == null || key == null) return;
    Map<String, CooldownEntry> map = playerCooldowns.get(uuid);
    if (map != null) {
      map.remove(key.toLowerCase());
      if (map.isEmpty()) {
        playerCooldowns.remove(uuid);
      }
    }
  }

  public void resetAll(Player player) {
    if (player == null) return;
    playerCooldowns.remove(player.getUniqueId());
  }

  public void resetAll(UUID uuid) {
    if (uuid == null) return;
    playerCooldowns.remove(uuid);
  }

  // ==========================================
  // Global / System Cooldowns
  // ==========================================

  public void setGlobal(String key, Duration duration) {
    setGlobal(key, duration.toMillis());
  }

  public void setGlobal(String key, long time, TimeUnit unit) {
    setGlobal(key, unit.toMillis(time));
  }

  public void setGlobal(String key, long millis) {
    if (key == null || millis <= 0) return;
    long now = System.currentTimeMillis();
    globalCooldowns.put(key.toLowerCase(), new CooldownEntry(now, now + millis));
  }

  public boolean isGlobalOnCooldown(String key) {
    if (key == null) return false;
    String normalized = key.toLowerCase();
    CooldownEntry entry = globalCooldowns.get(normalized);
    if (entry == null) return false;
    if (entry.isExpired()) {
      globalCooldowns.remove(normalized);
      return false;
    }
    return true;
  }

  public long getGlobalRemainingMillis(String key) {
    if (key == null) return 0;
    String normalized = key.toLowerCase();
    CooldownEntry entry = globalCooldowns.get(normalized);
    if (entry == null || entry.isExpired()) {
      globalCooldowns.remove(normalized);
      return 0;
    }
    return entry.remainingMillis();
  }

  public long getGlobalRemainingSeconds(String key) {
    return Math.max(0, (getGlobalRemainingMillis(key) + 999) / 1000);
  }

  public void resetGlobal(String key) {
    if (key == null) return;
    globalCooldowns.remove(key.toLowerCase());
  }

  // ==========================================
  // Visual Action Bar Progress Ticker
  // ==========================================

  /**
   * Automatically display an animated action-bar progress bar on the player's screen until the
   * cooldown expires, then shows a brief ready confirmation!
   */
  public void startActionBarTicker(Player player, String key, String displayName) {
    if (player == null || !player.isOnline()) return;
    String tickerKey = player.getUniqueId() + ":" + key.toLowerCase();

    // Cancel existing ticker if running
    cancelTicker(tickerKey);

    Runnable ticker =
        new Runnable() {
          @Override
          public void run() {
            if (!player.isOnline()) {
              cancelTicker(tickerKey);
              return;
            }

            if (!isOnCooldown(player, key)) {
              player.sendActionBar(Component.text("§a✔ §l" + displayName + " §aReady to use!"));
              cancelTicker(tickerKey);
              return;
            }

            double progress = getProgress(player, key);
            double remainingSec = getRemainingMillis(player, key) / 1000.0;
            String bar = renderProgressBar(progress, 15);

            player.sendActionBar(
                Component.text(
                    "§e"
                        + displayName
                        + " §8["
                        + bar
                        + "§8] §f"
                        + String.format("%.1fs", remainingSec)));
          }
        };

    if (ServerUtil.isFolia()) {
      try {
        Object task = player.getScheduler().runAtFixedRate(plugin, t -> ticker.run(), null, 1L, 4L);
        activeTickerTasks.put(tickerKey, task);
      } catch (Exception e) {
        // Fallback
      }
    } else {
      BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, ticker, 1L, 4L);
      activeTickerTasks.put(tickerKey, task);
    }
  }

  private void cancelTicker(String tickerKey) {
    Object task = activeTickerTasks.remove(tickerKey);
    if (task instanceof BukkitTask bt) {
      bt.cancel();
    } else if (task != null) {
      try {
        task.getClass().getMethod("cancel").invoke(task);
      } catch (Exception ignored) {
      }
    }
  }

  private String renderProgressBar(double progress, int totalBars) {
    int filled = (int) Math.round(progress * totalBars);
    filled = Math.max(0, Math.min(totalBars, filled));
    int empty = totalBars - filled;
    return "§a" + "■".repeat(filled) + "§7" + "□".repeat(empty);
  }

  // ==========================================
  // Cleanup & Lifecycle
  // ==========================================

  /** Purges all expired entries to guarantee zero memory leaks over long server uptimes. */
  public void purgeExpired() {
    long now = System.currentTimeMillis();
    for (Map.Entry<UUID, Map<String, CooldownEntry>> pe : playerCooldowns.entrySet()) {
      pe.getValue().entrySet().removeIf(e -> now >= e.getValue().expireTimeMillis);
      if (pe.getValue().isEmpty()) {
        playerCooldowns.remove(pe.getKey());
      }
    }
    globalCooldowns.entrySet().removeIf(e -> now >= e.getValue().expireTimeMillis);
  }

  /** Clears all cooldowns and cancels all active ticker tasks. Called on script unload. */
  public void cleanup() {
    for (Object task : activeTickerTasks.values()) {
      if (task instanceof BukkitTask bt) {
        bt.cancel();
      } else if (task != null) {
        try {
          task.getClass().getMethod("cancel").invoke(task);
        } catch (Exception ignored) {
        }
      }
    }
    activeTickerTasks.clear();
    playerCooldowns.clear();
    globalCooldowns.clear();
  }

  // ==========================================
  // Internal Record
  // ==========================================

  private static class CooldownEntry {
    final long startTimeMillis;
    final long expireTimeMillis;

    CooldownEntry(long startTimeMillis, long expireTimeMillis) {
      this.startTimeMillis = startTimeMillis;
      this.expireTimeMillis = expireTimeMillis;
    }

    boolean isExpired() {
      return System.currentTimeMillis() >= expireTimeMillis;
    }

    long remainingMillis() {
      return Math.max(0, expireTimeMillis - System.currentTimeMillis());
    }

    double progress() {
      long total = expireTimeMillis - startTimeMillis;
      if (total <= 0) return 1.0;
      long elapsed = System.currentTimeMillis() - startTimeMillis;
      return Math.min(1.0, Math.max(0.0, (double) elapsed / total));
    }
  }
}
