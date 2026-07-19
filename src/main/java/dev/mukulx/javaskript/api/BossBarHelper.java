package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BossBarHelper {

  private final JavaSkriptPlugin plugin;
  private final Map<UUID, BossBar> activeBossBars;

  public BossBarHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.activeBossBars = new HashMap<>();
  }

  public void show(Player player, String text) {
    show(player, text, 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
  }

  public void show(Player player, String text, float progress) {
    show(player, text, progress, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
  }

  public void show(Player player, String text, float progress, BossBar.Color color) {
    show(player, text, progress, color, BossBar.Overlay.PROGRESS);
  }

  public void show(
      Player player, String text, float progress, BossBar.Color color, BossBar.Overlay style) {
    Component message = Component.text(text);
    showComponent(player, message, progress, color, style);
  }

  public void showMini(Player player, String miniMessage) {
    showMini(player, miniMessage, 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
  }

  public void showMini(Player player, String miniMessage, float progress) {
    showMini(player, miniMessage, progress, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
  }

  public void showMini(Player player, String miniMessage, float progress, BossBar.Color color) {
    showMini(player, miniMessage, progress, color, BossBar.Overlay.PROGRESS);
  }

  public void showMini(
      Player player,
      String miniMessage,
      float progress,
      BossBar.Color color,
      BossBar.Overlay style) {
    Component message = MiniMessage.miniMessage().deserialize(miniMessage);
    showComponent(player, message, progress, color, style);
  }

  public void showComponent(Player player, Component text, float progress, BossBar.Color color) {
    showComponent(player, text, progress, color, BossBar.Overlay.PROGRESS);
  }

  public void showComponent(
      Player player, Component text, float progress, BossBar.Color color, BossBar.Overlay style) {
    hide(player);

    progress = Math.max(0.0f, Math.min(1.0f, progress));

    BossBar bossBar = BossBar.bossBar(text, progress, color, style);
    player.showBossBar(bossBar);
    activeBossBars.put(player.getUniqueId(), bossBar);
  }

  public void showTimed(Player player, String text, int durationTicks) {
    showTimed(player, text, 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, durationTicks);
  }

  public void showTimed(Player player, String text, float progress, int durationTicks) {
    showTimed(player, text, progress, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, durationTicks);
  }

  public void showTimed(
      Player player,
      String text,
      float progress,
      BossBar.Color color,
      BossBar.Overlay style,
      int durationTicks) {
    show(player, text, progress, color, style);

    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              if (player.isOnline()) {
                hide(player);
              }
            },
            durationTicks);
  }

  public void showCountdown(Player player, String text, int totalSeconds) {
    showCountdown(player, text, totalSeconds, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
  }

  public void showCountdown(
      Player player, String text, int totalSeconds, BossBar.Color color, BossBar.Overlay style) {
    hide(player);

    float initialProgress = 1.0f;
    Component message = Component.text(text + " (" + totalSeconds + "s)");
    BossBar bossBar = BossBar.bossBar(message, initialProgress, color, style);
    player.showBossBar(bossBar);
    activeBossBars.put(player.getUniqueId(), bossBar);

    final int[] remaining = {totalSeconds};

    Bukkit.getScheduler()
        .runTaskTimer(
            plugin,
            task -> {
              if (!player.isOnline() || remaining[0] <= 0) {
                task.cancel();
                hide(player);
                return;
              }

              remaining[0]--;
              float progress = (float) remaining[0] / totalSeconds;
              bossBar.progress(progress);
              bossBar.name(Component.text(text + " (" + remaining[0] + "s)"));

              if (remaining[0] <= 0) {
                task.cancel();
                Bukkit.getScheduler().runTaskLater(plugin, () -> hide(player), 20L);
              }
            },
            20L,
            20L);
  }

  public void updateText(Player player, String text) {
    BossBar bossBar = activeBossBars.get(player.getUniqueId());
    if (bossBar != null) {
      bossBar.name(Component.text(text));
    }
  }

  public void updateTextMini(Player player, String miniMessage) {
    BossBar bossBar = activeBossBars.get(player.getUniqueId());
    if (bossBar != null) {
      Component message = MiniMessage.miniMessage().deserialize(miniMessage);
      bossBar.name(message);
    }
  }

  public void updateProgress(Player player, float progress) {
    BossBar bossBar = activeBossBars.get(player.getUniqueId());
    if (bossBar != null) {
      progress = Math.max(0.0f, Math.min(1.0f, progress));
      bossBar.progress(progress);
    }
  }

  public void updateColor(Player player, BossBar.Color color) {
    BossBar bossBar = activeBossBars.get(player.getUniqueId());
    if (bossBar != null) {
      bossBar.color(color);
    }
  }

  public void updateStyle(Player player, BossBar.Overlay style) {
    BossBar bossBar = activeBossBars.get(player.getUniqueId());
    if (bossBar != null) {
      bossBar.overlay(style);
    }
  }

  public void hide(Player player) {
    BossBar bossBar = activeBossBars.remove(player.getUniqueId());
    if (bossBar != null) {
      player.hideBossBar(bossBar);
    }
  }

  public void hideAll() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      hide(player);
    }
  }

  public boolean isShowing(Player player) {
    return activeBossBars.containsKey(player.getUniqueId());
  }

  public void broadcast(String text) {
    broadcast(text, 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
  }

  public void broadcast(String text, float progress) {
    broadcast(text, progress, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
  }

  public void broadcast(String text, float progress, BossBar.Color color, BossBar.Overlay style) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      show(player, text, progress, color, style);
    }
  }

  public void broadcastTimed(String text, int durationTicks) {
    broadcastTimed(text, 1.0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS, durationTicks);
  }

  public void broadcastTimed(
      String text, float progress, BossBar.Color color, BossBar.Overlay style, int durationTicks) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      showTimed(player, text, progress, color, style, durationTicks);
    }
  }

  public BossBarBuilder builder() {
    return new BossBarBuilder(this);
  }

  public static class BossBarBuilder {
    private final BossBarHelper helper;
    private String text = "";
    private float progress = 1.0f;
    private BossBar.Color color = BossBar.Color.WHITE;
    private BossBar.Overlay style = BossBar.Overlay.PROGRESS;
    private Integer durationTicks = null;
    private boolean countdown = false;
    private Integer countdownSeconds = null;
    private boolean useMiniMessage = false;

    public BossBarBuilder(BossBarHelper helper) {
      this.helper = helper;
    }

    public BossBarBuilder text(String text) {
      this.text = text;
      this.useMiniMessage = false;
      return this;
    }

    public BossBarBuilder mini(String miniMessage) {
      this.text = miniMessage;
      this.useMiniMessage = true;
      return this;
    }

    public BossBarBuilder progress(float progress) {
      this.progress = progress;
      return this;
    }

    public BossBarBuilder color(BossBar.Color color) {
      this.color = color;
      return this;
    }

    public BossBarBuilder style(BossBar.Overlay style) {
      this.style = style;
      return this;
    }

    public BossBarBuilder duration(int ticks) {
      this.durationTicks = ticks;
      return this;
    }

    public BossBarBuilder countdown(int seconds) {
      this.countdown = true;
      this.countdownSeconds = seconds;
      return this;
    }

    public void show(Player player) {
      if (countdown && countdownSeconds != null) {
        helper.showCountdown(player, text, countdownSeconds, color, style);
      } else if (durationTicks != null) {
        if (useMiniMessage) {
          helper.showMini(player, text, progress, color, style);
          helper
              .plugin
              .getServer()
              .getScheduler()
              .runTaskLater(
                  helper.plugin,
                  () -> {
                    if (player.isOnline()) {
                      helper.hide(player);
                    }
                  },
                  durationTicks);
        } else {
          helper.showTimed(player, text, progress, color, style, durationTicks);
        }
      } else {
        if (useMiniMessage) {
          helper.showMini(player, text, progress, color, style);
        } else {
          helper.show(player, text, progress, color, style);
        }
      }
    }

    public void broadcast() {
      for (Player player : Bukkit.getOnlinePlayers()) {
        show(player);
      }
    }
  }
}
