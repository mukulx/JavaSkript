import dev.mukulx.javaskript.script.FoliaSupport;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

@FoliaSupport
public class AnnounceCommand implements CommandExecutor, TabCompleter {

  private static final TextColor ANNOUNCE_START = TextColor.color(255, 100, 100);
  private static final TextColor ANNOUNCE_END = TextColor.color(255, 200, 100);

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("announce.use")) {
      sender.sendMessage(
          Component.text("You don't have permission to use this command!")
              .color(TextColor.color(255, 85, 85)));
      return true;
    }

    if (args.length == 0) {
      sender.sendMessage(
          Component.text("Usage: /announce <message>").color(TextColor.color(255, 170, 0)));
      return true;
    }

    String message = String.join(" ", args);
    boolean centered = label.equalsIgnoreCase("announcecentered");

    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(Component.empty());

      if (centered) {
        player.sendMessage(centerMessage(createSeparator()));
        player.sendMessage(Component.empty());
        player.sendMessage(centerMessage(createGradient("ANNOUNCEMENT")));
        player.sendMessage(Component.empty());
        player.sendMessage(
            centerMessage(Component.text(message).color(TextColor.color(255, 255, 255))));
        player.sendMessage(Component.empty());
        player.sendMessage(centerMessage(createSeparator()));
      } else {
        player.sendMessage(createSeparator());
        player.sendMessage(Component.empty());
        player.sendMessage(
            Component.text("  ")
                .append(createGradient("Announcement: "))
                .append(Component.text(message).color(TextColor.color(255, 255, 255))));
        player.sendMessage(Component.empty());
        player.sendMessage(createSeparator());
      }

      player.sendMessage(Component.empty());

      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.5f);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
      player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);

      Component titleComponent = createGradient("ANNOUNCEMENT");
      Component subtitleComponent = Component.text(message).color(TextColor.color(255, 255, 255));

      Title title =
          Title.title(
              titleComponent,
              subtitleComponent,
              Title.Times.times(
                  Duration.ofMillis(500), Duration.ofMillis(4000), Duration.ofMillis(500)));

      player.showTitle(title);
    }

    sender.sendMessage(
        Component.text("Announcement sent to all players!").color(TextColor.color(85, 255, 85)));
    return true;
  }

  private Component createGradient(String text) {
    Component result = Component.empty();
    int length = text.length();

    for (int i = 0; i < length; i++) {
      float ratio = (float) i / Math.max(length - 1, 1);

      int r = (int) (ANNOUNCE_START.red() + ratio * (ANNOUNCE_END.red() - ANNOUNCE_START.red()));
      int g =
          (int) (ANNOUNCE_START.green() + ratio * (ANNOUNCE_END.green() - ANNOUNCE_START.green()));
      int b = (int) (ANNOUNCE_START.blue() + ratio * (ANNOUNCE_END.blue() - ANNOUNCE_START.blue()));

      result = result.append(Component.text(text.charAt(i)).color(TextColor.color(r, g, b)));
    }

    return result;
  }

  private Component createSeparator() {
    return createGradient("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
  }

  private Component centerMessage(Component component) {
    return Component.text("                    ").append(component);
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return Arrays.asList("Type your announcement here");
    }
    return List.of();
  }
}
