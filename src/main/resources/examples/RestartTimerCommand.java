import dev.mukulx.javaskript.api.BossBarHelper;
import dev.mukulx.javaskript.api.ScriptScheduler;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class RestartTimerCommand implements CommandExecutor, TabCompleter {

  private BossBarHelper bossBar;
  private ScriptScheduler scheduler;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("restarttimer.use")) {
      sender.sendMessage("§cNo permission!");
      return true;
    }

    if (args.length == 0) {
      sender.sendMessage("§cUsage: /restarttimer <seconds>");
      return true;
    }

    int seconds;
    try {
      seconds = Integer.parseInt(args[0]);
      if (seconds <= 0 || seconds > 3600) {
        sender.sendMessage("§cTime must be between 1 and 3600 seconds!");
        return true;
      }
    } catch (NumberFormatException e) {
      sender.sendMessage("§cInvalid number!");
      return true;
    }

    sender.sendMessage("§aStarting " + seconds + " second restart timer!");

    for (Player player : Bukkit.getOnlinePlayers()) {
      bossBar.showCountdown(
          player, "§c§lѕᴇʀᴠᴇʀ ʀᴇѕᴛᴀʀᴛ", seconds, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
    }

    scheduler.runLater(
        () -> {
          for (Player player : Bukkit.getOnlinePlayers()) {
            player.kick(
                net.kyori.adventure.text.Component.text(
                    "§c§lѕᴇʀᴠᴇʀ ʀᴇѕᴛᴀʀᴛɪɴɢ\n\n§7ᴄᴏᴍᴇ ʙᴀᴄᴋ ɪɴ ᴀ ғᴇᴡ ꜱᴇᴄᴏɴᴅꜱ!"));
          }
        },
        seconds * 20L);

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return Arrays.asList("30", "60", "120", "300");
    }
    return List.of();
  }
}
