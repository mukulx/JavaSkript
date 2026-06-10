package dev.mukulx.javaskript.api;

import java.time.Duration;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TitleHelper {

  private final MiniMessage miniMessage;

  public TitleHelper() {
    this.miniMessage = MiniMessage.miniMessage();
  }

  public void send(Player player, String title) {
    send(player, title, "");
  }

  public void send(Player player, String title, String subtitle) {
    Component titleComponent = Component.text(title);
    Component subtitleComponent = Component.text(subtitle);

    Title titleObj =
        Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500)));

    player.showTitle(titleObj);
  }

  public void send(Player player, Component title, Component subtitle) {
    Title titleObj =
        Title.title(
            title,
            subtitle,
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500)));

    player.showTitle(titleObj);
  }

  public void sendMini(Player player, String title) {
    sendMini(player, title, "");
  }

  public void sendMini(Player player, String title, String subtitle) {
    Component titleComponent = miniMessage.deserialize(title);
    Component subtitleComponent = miniMessage.deserialize(subtitle);

    Title titleObj =
        Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500)));

    player.showTitle(titleObj);
  }

  public void send(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    Component titleComponent = Component.text(title);
    Component subtitleComponent = Component.text(subtitle);

    Title titleObj =
        Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50),
                Duration.ofMillis(stay * 50),
                Duration.ofMillis(fadeOut * 50)));

    player.showTitle(titleObj);
  }

  public void sendMini(
      Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    Component titleComponent = miniMessage.deserialize(title);
    Component subtitleComponent = miniMessage.deserialize(subtitle);

    Title titleObj =
        Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50),
                Duration.ofMillis(stay * 50),
                Duration.ofMillis(fadeOut * 50)));

    player.showTitle(titleObj);
  }

  public void send(
      Player player,
      Component title,
      Component subtitle,
      int fadeIn,
      int stay,
      int fadeOut) {
    Title titleObj =
        Title.title(
            title,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(fadeIn * 50),
                Duration.ofMillis(stay * 50),
                Duration.ofMillis(fadeOut * 50)));

    player.showTitle(titleObj);
  }

  public void clear(Player player) {
    player.clearTitle();
  }

  public void reset(Player player) {
    player.resetTitle();
  }

  public void broadcast(String title) {
    broadcast(title, "");
  }

  public void broadcast(String title, String subtitle) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      send(player, title, subtitle);
    }
  }

  public void broadcastMini(String title) {
    broadcastMini(title, "");
  }

  public void broadcastMini(String title, String subtitle) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      sendMini(player, title, subtitle);
    }
  }

  public void broadcast(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      send(player, title, subtitle, fadeIn, stay, fadeOut);
    }
  }

  public void sendTo(Collection<? extends Player> players, String title) {
    sendTo(players, title, "");
  }

  public void sendTo(Collection<? extends Player> players, String title, String subtitle) {
    for (Player player : players) {
      send(player, title, subtitle);
    }
  }

  public void sendToMini(Collection<? extends Player> players, String title) {
    sendToMini(players, title, "");
  }

  public void sendToMini(Collection<? extends Player> players, String title, String subtitle) {
    for (Player player : players) {
      sendMini(player, title, subtitle);
    }
  }

  public TitleBuilder builder() {
    return new TitleBuilder();
  }

  public class TitleBuilder {
    private Component title = Component.empty();
    private Component subtitle = Component.empty();
    private int fadeIn = 10;
    private int stay = 60;
    private int fadeOut = 10;

    public TitleBuilder title(String title) {
      this.title = Component.text(title);
      return this;
    }

    public TitleBuilder title(Component title) {
      this.title = title;
      return this;
    }

    public TitleBuilder titleMini(String title) {
      this.title = miniMessage.deserialize(title);
      return this;
    }

    public TitleBuilder subtitle(String subtitle) {
      this.subtitle = Component.text(subtitle);
      return this;
    }

    public TitleBuilder subtitle(Component subtitle) {
      this.subtitle = subtitle;
      return this;
    }

    public TitleBuilder subtitleMini(String subtitle) {
      this.subtitle = miniMessage.deserialize(subtitle);
      return this;
    }

    public TitleBuilder fadeIn(int ticks) {
      this.fadeIn = ticks;
      return this;
    }

    public TitleBuilder stay(int ticks) {
      this.stay = ticks;
      return this;
    }

    public TitleBuilder fadeOut(int ticks) {
      this.fadeOut = ticks;
      return this;
    }

    public TitleBuilder times(int fadeIn, int stay, int fadeOut) {
      this.fadeIn = fadeIn;
      this.stay = stay;
      this.fadeOut = fadeOut;
      return this;
    }

    public Title build() {
      return Title.title(
          title,
          subtitle,
          Title.Times.times(
              Duration.ofMillis(fadeIn * 50),
              Duration.ofMillis(stay * 50),
              Duration.ofMillis(fadeOut * 50)));
    }

    public void send(Player player) {
      player.showTitle(build());
    }

    public void broadcast() {
      Title titleObj = build();
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.showTitle(titleObj);
      }
    }

    public void sendTo(Collection<? extends Player> players) {
      Title titleObj = build();
      for (Player player : players) {
        player.showTitle(titleObj);
      }
    }
  }

  public static class Quick {

    public static Component gradient(String text, TextColor start, TextColor end) {
      Component result = Component.empty();
      int length = text.length();

      for (int i = 0; i < length; i++) {
        float ratio = (float) i / Math.max(length - 1, 1);

        int r = (int) (start.red() + ratio * (end.red() - start.red()));
        int g = (int) (start.green() + ratio * (end.green() - start.green()));
        int b = (int) (start.blue() + ratio * (end.blue() - start.blue()));

        result = result.append(Component.text(text.charAt(i)).color(TextColor.color(r, g, b)));
      }

      return result;
    }

    public static Title success(String message) {
      return Title.title(
          Component.text("✓ Success").color(TextColor.color(85, 255, 85)),
          Component.text(message).color(TextColor.color(255, 255, 255)),
          Title.Times.times(
              Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500)));
    }

    public static Title error(String message) {
      return Title.title(
          Component.text("✗ Error").color(TextColor.color(255, 85, 85)),
          Component.text(message).color(TextColor.color(255, 255, 255)),
          Title.Times.times(
              Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500)));
    }

    public static Title warning(String message) {
      return Title.title(
          Component.text("⚠ Warning").color(TextColor.color(255, 215, 0)),
          Component.text(message).color(TextColor.color(255, 255, 255)),
          Title.Times.times(
              Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500)));
    }

    public static Title info(String message) {
      return Title.title(
          Component.text("ℹ Info").color(TextColor.color(85, 170, 255)),
          Component.text(message).color(TextColor.color(255, 255, 255)),
          Title.Times.times(
              Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500)));
    }
  }
}
