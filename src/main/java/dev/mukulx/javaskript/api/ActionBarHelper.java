package dev.mukulx.javaskript.api;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Comprehensive ActionBar API with full Adventure API support. Supports gradients, animations,
 * MiniMessage, and all text formatting.
 */
public class ActionBarHelper {

  private final Plugin plugin;
  private final MiniMessage miniMessage;
  private final Map<UUID, BukkitTask> activeTasks;

  public ActionBarHelper(Plugin plugin) {
    this.plugin = plugin;
    this.miniMessage = MiniMessage.miniMessage();
    this.activeTasks = new ConcurrentHashMap<>();
  }

  // Simple text
  public void send(Player player, String text) {
    player.sendActionBar(Component.text(text));
  }

  // Colored text
  public void send(Player player, String text, TextColor color) {
    player.sendActionBar(Component.text(text, color));
  }

  // MiniMessage format
  public void sendMini(Player player, String miniMessageText) {
    player.sendActionBar(miniMessage.deserialize(miniMessageText));
  }

  // Component
  public void send(Player player, Component component) {
    player.sendActionBar(component);
  }

  // Component with duration
  public void send(Player player, Component component, Duration duration) {
    player.sendActionBar(component);
    scheduleTask(player, () -> clear(player), duration.toMillis() / 50);
  }

  // Duration-based (auto-clear after duration)
  public void send(Player player, String text, Duration duration) {
    sendMini(player, text);
    scheduleTask(player, () -> clear(player), duration.toMillis() / 50); // Convert to ticks
  }

  public void sendMini(Player player, String miniMessageText, Duration duration) {
    sendMini(player, miniMessageText);
    scheduleTask(player, () -> clear(player), duration.toMillis() / 50);
  }

  // Persistent (stays until manually cleared, refreshes every second)
  public void sendPersistent(Player player, String text) {
    cancelTask(player);
    BukkitTask task =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  if (!player.isOnline()) {
                    cancelTask(player);
                    return;
                  }
                  send(player, text);
                },
                0L,
                20L);
    activeTasks.put(player.getUniqueId(), task);
  }

  public void sendPersistentMini(Player player, String miniMessageText) {
    cancelTask(player);
    BukkitTask task =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  if (!player.isOnline()) {
                    cancelTask(player);
                    return;
                  }
                  sendMini(player, miniMessageText);
                },
                0L,
                20L);
    activeTasks.put(player.getUniqueId(), task);
  }

  // Animated action bars (cycles through messages)
  public void sendAnimated(Player player, List<String> frames, long interval) {
    cancelTask(player);
    if (frames.isEmpty()) return;

    final int[] index = {0};
    BukkitTask task =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  if (!player.isOnline()) {
                    cancelTask(player);
                    return;
                  }
                  sendMini(player, frames.get(index[0]));
                  index[0] = (index[0] + 1) % frames.size();
                },
                0L,
                interval);
    activeTasks.put(player.getUniqueId(), task);
  }

  // Animated with auto-stop after duration
  public void sendAnimated(Player player, List<String> frames, long interval, Duration duration) {
    sendAnimated(player, frames, interval);
    scheduleTask(player, () -> cancelTask(player), duration.toMillis() / 50);
  }

  // Progress bar builder
  public ProgressBar progressBar() {
    return new ProgressBar();
  }

  // Gradient builder
  public GradientBuilder gradient(String text) {
    return new GradientBuilder(text);
  }

  // Clear action bar
  public void clear(Player player) {
    cancelTask(player);
    player.sendActionBar(Component.empty());
  }

  // Broadcast to all players
  public void broadcast(String text) {
    Bukkit.getOnlinePlayers().forEach(p -> send(p, text));
  }

  public void broadcastMini(String miniMessageText) {
    Bukkit.getOnlinePlayers().forEach(p -> sendMini(p, miniMessageText));
  }

  // Cleanup
  public void shutdown() {
    activeTasks.values().forEach(BukkitTask::cancel);
    activeTasks.clear();
  }

  private void scheduleTask(Player player, Runnable task, long delay) {
    Bukkit.getScheduler().runTaskLater(plugin, task, delay);
  }

  private void cancelTask(Player player) {
    BukkitTask task = activeTasks.remove(player.getUniqueId());
    if (task != null) {
      task.cancel();
    }
  }

  // Progress bar builder
  public class ProgressBar {
    private double current = 0;
    private double max = 100;
    private int length = 20;
    private String symbol = "█";
    private TextColor filledColor = NamedTextColor.GREEN;
    private TextColor emptyColor = NamedTextColor.GRAY;
    private String prefix = "";
    private String suffix = "";
    private boolean showPercentage = false;

    public ProgressBar current(double current) {
      this.current = current;
      return this;
    }

    public ProgressBar max(double max) {
      this.max = max;
      return this;
    }

    public ProgressBar length(int length) {
      this.length = length;
      return this;
    }

    public ProgressBar symbol(String symbol) {
      this.symbol = symbol;
      return this;
    }

    public ProgressBar filledColor(TextColor color) {
      this.filledColor = color;
      return this;
    }

    public ProgressBar emptyColor(TextColor color) {
      this.emptyColor = color;
      return this;
    }

    public ProgressBar prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    public ProgressBar suffix(String suffix) {
      this.suffix = suffix;
      return this;
    }

    public ProgressBar showPercentage(boolean show) {
      this.showPercentage = show;
      return this;
    }

    public Component build() {
      double percentage = Math.min(1.0, Math.max(0.0, current / max));
      int filled = (int) (length * percentage);
      int empty = length - filled;

      Component bar = Component.empty();

      if (!prefix.isEmpty()) {
        bar = bar.append(miniMessage.deserialize(prefix));
      }

      bar = bar.append(Component.text(symbol.repeat(filled), filledColor));
      bar = bar.append(Component.text(symbol.repeat(empty), emptyColor));

      if (showPercentage) {
        bar =
            bar.append(Component.text(" " + (int) (percentage * 100) + "%", NamedTextColor.WHITE));
      }

      if (!suffix.isEmpty()) {
        bar = bar.append(miniMessage.deserialize(suffix));
      }

      return bar;
    }

    public void send(Player player) {
      ActionBarHelper.this.send(player, build());
    }

    public void send(Player player, Duration duration) {
      send(player);
      scheduleTask(player, () -> clear(player), duration.toMillis() / 50);
    }
  }

  // Gradient builder
  public class GradientBuilder {
    private final String text;
    private TextColor startColor = NamedTextColor.AQUA;
    private TextColor endColor = NamedTextColor.BLUE;
    private boolean bold = false;
    private boolean italic = false;
    private boolean underlined = false;
    private boolean strikethrough = false;
    private boolean obfuscated = false;

    public GradientBuilder(String text) {
      this.text = text;
    }

    public GradientBuilder colors(TextColor start, TextColor end) {
      this.startColor = start;
      this.endColor = end;
      return this;
    }

    public GradientBuilder bold() {
      this.bold = true;
      return this;
    }

    public GradientBuilder italic() {
      this.italic = true;
      return this;
    }

    public GradientBuilder underlined() {
      this.underlined = true;
      return this;
    }

    public GradientBuilder strikethrough() {
      this.strikethrough = true;
      return this;
    }

    public GradientBuilder obfuscated() {
      this.obfuscated = true;
      return this;
    }

    public Component build() {
      String gradient =
          String.format("<gradient:%s:%s>%s</gradient>", toHex(startColor), toHex(endColor), text);

      Component component = miniMessage.deserialize(gradient);

      if (bold) component = component.decorate(TextDecoration.BOLD);
      if (italic) component = component.decorate(TextDecoration.ITALIC);
      if (underlined) component = component.decorate(TextDecoration.UNDERLINED);
      if (strikethrough) component = component.decorate(TextDecoration.STRIKETHROUGH);
      if (obfuscated) component = component.decorate(TextDecoration.OBFUSCATED);

      return component;
    }

    public void send(Player player) {
      ActionBarHelper.this.send(player, build());
    }

    public void send(Player player, Duration duration) {
      send(player);
      scheduleTask(player, () -> clear(player), duration.toMillis() / 50);
    }

    private String toHex(TextColor color) {
      return String.format("#%06x", color.value());
    }
  }

  // Quick utility methods for common patterns
  public static class Quick {

    // Health bar
    public static Component healthBar(double health, double maxHealth) {
      return healthBar(health, maxHealth, 10);
    }

    public static Component healthBar(double health, double maxHealth, int length) {
      double percentage = health / maxHealth;
      int filled = (int) (length * percentage);
      int empty = length - filled;

      TextColor color =
          percentage > 0.6
              ? NamedTextColor.GREEN
              : percentage > 0.3 ? NamedTextColor.YELLOW : NamedTextColor.RED;

      return Component.text("❤ ", NamedTextColor.RED)
          .append(Component.text("█".repeat(filled), color))
          .append(Component.text("█".repeat(empty), NamedTextColor.DARK_GRAY))
          .append(Component.text(" " + (int) health + "/" + (int) maxHealth, NamedTextColor.WHITE));
    }

    // XP/Level bar
    public static Component xpBar(int current, int required) {
      return xpBar(current, required, 20);
    }

    public static Component xpBar(int current, int required, int length) {
      double percentage = (double) current / required;
      int filled = (int) (length * percentage);
      int empty = length - filled;

      return Component.text("✦ ", NamedTextColor.GOLD)
          .append(Component.text("▰".repeat(filled), NamedTextColor.YELLOW))
          .append(Component.text("▱".repeat(empty), NamedTextColor.GRAY))
          .append(Component.text(" " + current + "/" + required + " XP", NamedTextColor.GOLD));
    }

    // Cooldown bar
    public static Component cooldownBar(long remainingMs, long totalMs) {
      return cooldownBar(remainingMs, totalMs, 15);
    }

    public static Component cooldownBar(long remainingMs, long totalMs, int length) {
      double percentage = 1.0 - ((double) remainingMs / totalMs);
      int filled = (int) (length * percentage);
      int empty = length - filled;

      return Component.text("⏳ ", NamedTextColor.AQUA)
          .append(Component.text("▌".repeat(filled), NamedTextColor.AQUA))
          .append(Component.text("▌".repeat(empty), NamedTextColor.DARK_GRAY))
          .append(Component.text(" " + (remainingMs / 1000) + "s", NamedTextColor.WHITE));
    }

    // Loading animation frames
    public static List<String> loadingAnimation() {
      return Arrays.asList(
          "<gray>Loading<white>.",
          "<gray>Loading<white>..",
          "<gray>Loading<white>...",
          "<gray>Loading<white>....");
    }

    public static List<String> spinnerAnimation() {
      return Arrays.asList(
          "<aqua>⠋</aqua> Loading...",
          "<aqua>⠙</aqua> Loading...",
          "<aqua>⠹</aqua> Loading...",
          "<aqua>⠸</aqua> Loading...",
          "<aqua>⠼</aqua> Loading...",
          "<aqua>⠴</aqua> Loading...",
          "<aqua>⠦</aqua> Loading...",
          "<aqua>⠧</aqua> Loading...",
          "<aqua>⠇</aqua> Loading...",
          "<aqua>⠏</aqua> Loading...");
    }

    public static List<String> rainbowAnimation(String text) {
      return Arrays.asList(
          "<gradient:red:yellow>" + text + "</gradient>",
          "<gradient:yellow:green>" + text + "</gradient>",
          "<gradient:green:aqua>" + text + "</gradient>",
          "<gradient:aqua:blue>" + text + "</gradient>",
          "<gradient:blue:light_purple>" + text + "</gradient>",
          "<gradient:light_purple:red>" + text + "</gradient>");
    }
  }
}
