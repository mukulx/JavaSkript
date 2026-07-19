import dev.mukulx.javaskript.api.BossBarHelper;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class BossBarExample implements CommandExecutor, TabCompleter {

  private BossBarHelper bossBar;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Players only!");
      return true;
    }

    if (args.length == 0) {
      player.sendMessage("§6BossBar Examples:");
      player.sendMessage("§e/bossbar simple §7- Simple white bar");
      player.sendMessage("§e/bossbar progress §7- Progress bar");
      player.sendMessage("§e/bossbar colored §7- Colored bars");
      player.sendMessage("§e/bossbar gradient §7- MiniMessage gradient");
      player.sendMessage("§e/bossbar timed §7- Auto-hide after 5s");
      player.sendMessage("§e/bossbar countdown §7- 10 second countdown");
      player.sendMessage("§e/bossbar update §7- Update existing bar");
      player.sendMessage("§e/bossbar styles §7- Different styles");
      player.sendMessage("§e/bossbar builder §7- Builder pattern");
      player.sendMessage("§e/bossbar broadcast §7- Show to all players");
      player.sendMessage("§e/bossbar hide §7- Hide your boss bar");
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "simple" -> {
        bossBar.show(player, "Simple Boss Bar");
        player.sendMessage("§aShowing simple boss bar");
      }

      case "progress" -> {
        bossBar.show(player, "Loading...", 0.5f);
        player.sendMessage("§aShowing 50% progress");
      }

      case "colored" -> {
        bossBar.show(player, "Green Bar", 1.0f, BossBar.Color.GREEN);
        player.sendMessage("§aShowing green bar, cycling colors...");

        scheduler.runTaskLater(
            () -> {
              bossBar.updateColor(player, BossBar.Color.YELLOW);
              bossBar.updateText(player, "Yellow Bar");
            },
            40);

        scheduler.runTaskLater(
            () -> {
              bossBar.updateColor(player, BossBar.Color.RED);
              bossBar.updateText(player, "Red Bar");
            },
            80);

        scheduler.runTaskLater(() -> bossBar.hide(player), 120);
      }

      case "gradient" -> {
        bossBar.showMini(player, "<gradient:green:blue>Beautiful Gradient Boss Bar</gradient>");
        player.sendMessage("§aShowing gradient boss bar");
      }

      case "timed" -> {
        bossBar.showTimed(player, "This will hide in 5 seconds", 1.0f, 100);
        player.sendMessage("§aShowing timed boss bar (5s)");
      }

      case "countdown" -> {
        bossBar.showCountdown(player, "Countdown", 10);
        player.sendMessage("§aStarting 10 second countdown");
      }

      case "update" -> {
        bossBar.show(player, "Watch me change", 0.0f, BossBar.Color.RED);
        player.sendMessage("§aUpdating boss bar every second...");

        for (int i = 1; i <= 10; i++) {
          final int step = i;
          scheduler.runTaskLater(
              () -> {
                bossBar.updateProgress(player, step / 10.0f);
                bossBar.updateText(player, "Progress: " + (step * 10) + "%");

                if (step >= 5) {
                  bossBar.updateColor(player, BossBar.Color.YELLOW);
                }
                if (step >= 8) {
                  bossBar.updateColor(player, BossBar.Color.GREEN);
                }
                if (step == 10) {
                  bossBar.updateText(player, "Complete!");
                  scheduler.runTaskLater(() -> bossBar.hide(player), 40);
                }
              },
              i * 20L);
        }
      }

      case "styles" -> {
        bossBar.show(
            player, "PROGRESS Style", 0.8f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        player.sendMessage("§aShowing different styles...");

        scheduler.runTaskLater(
            () -> {
              bossBar.updateStyle(player, BossBar.Overlay.NOTCHED_6);
              bossBar.updateText(player, "NOTCHED_6 Style");
            },
            40);

        scheduler.runTaskLater(
            () -> {
              bossBar.updateStyle(player, BossBar.Overlay.NOTCHED_10);
              bossBar.updateText(player, "NOTCHED_10 Style");
            },
            80);

        scheduler.runTaskLater(
            () -> {
              bossBar.updateStyle(player, BossBar.Overlay.NOTCHED_12);
              bossBar.updateText(player, "NOTCHED_12 Style");
            },
            120);

        scheduler.runTaskLater(
            () -> {
              bossBar.updateStyle(player, BossBar.Overlay.NOTCHED_20);
              bossBar.updateText(player, "NOTCHED_20 Style");
            },
            160);

        scheduler.runTaskLater(() -> bossBar.hide(player), 200);
      }

      case "builder" -> {
        bossBar
            .builder()
            .mini("<gradient:gold:yellow>Custom Boss Bar</gradient>")
            .progress(0.75f)
            .color(BossBar.Color.YELLOW)
            .style(BossBar.Overlay.NOTCHED_10)
            .duration(100)
            .show(player);

        player.sendMessage("§aShowing boss bar built with builder pattern");
      }

      case "broadcast" -> {
        bossBar.broadcast(
            "Message for Everyone!", 1.0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
        player.sendMessage("§aBroadcast boss bar to all players");
      }

      case "hide" -> {
        bossBar.hide(player);
        player.sendMessage("§aHidden your boss bar");
      }

      default -> {
        player.sendMessage("§cUnknown example: " + args[0]);
        return false;
      }
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return Arrays.asList(
          "simple",
          "progress",
          "colored",
          "gradient",
          "timed",
          "countdown",
          "update",
          "styles",
          "builder",
          "broadcast",
          "hide");
    }
    return List.of();
  }
}
