import dev.mukulx.javaskript.api.ActionBarHelper;
import dev.mukulx.javaskript.api.JavaSkriptAPI;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

@FoliaSupport
public class ActionBarExample implements CommandExecutor, TabCompleter {

  private JavaSkriptAPI api;
  private ActionBarHelper actionBar;

  private static final List<String> EXAMPLES =
      Arrays.asList(
          "simple",
          "mini",
          "gradient",
          "progress",
          "health",
          "xp",
          "cooldown",
          "loading",
          "spinner",
          "rainbow",
          "persistent",
          "clear");

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can use this command");
      return true;
    }

    if (args.length == 0) {
      player.sendMessage("§eActionBar Examples:");
      player.sendMessage("§7/actionbar simple §f- Simple text");
      player.sendMessage("§7/actionbar mini §f- MiniMessage format");
      player.sendMessage("§7/actionbar gradient §f- Gradient text");
      player.sendMessage("§7/actionbar progress §f- Progress bar");
      player.sendMessage("§7/actionbar health §f- Health bar");
      player.sendMessage("§7/actionbar xp §f- XP bar");
      player.sendMessage("§7/actionbar cooldown §f- Cooldown timer");
      player.sendMessage("§7/actionbar loading §f- Loading animation");
      player.sendMessage("§7/actionbar spinner §f- Spinner animation");
      player.sendMessage("§7/actionbar rainbow §f- Rainbow animation");
      player.sendMessage("§7/actionbar persistent §f- Persistent message");
      player.sendMessage("§7/actionbar clear §f- Clear action bar");
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "simple":
        actionBar.send(player, "Simple action bar message!");
        break;

      case "mini":
        actionBar.sendMini(
            player,
            "<gradient:gold:yellow><bold>Fancy</bold></gradient> <aqua>MiniMessage</aqua> <red>❤</red>");
        break;

      case "gradient":
        actionBar
            .gradient("Beautiful Gradient Text")
            .colors(NamedTextColor.AQUA, NamedTextColor.LIGHT_PURPLE)
            .bold()
            .send(player, Duration.ofSeconds(5));
        break;

      case "progress":
        actionBar
            .progressBar()
            .current(75)
            .max(100)
            .length(20)
            .prefix("<gold>⚡ Power: </gold>")
            .showPercentage(true)
            .filledColor(NamedTextColor.YELLOW)
            .emptyColor(NamedTextColor.DARK_GRAY)
            .send(player, Duration.ofSeconds(5));
        break;

      case "health":
        actionBar.send(
            player,
            ActionBarHelper.Quick.healthBar(player.getHealth(), player.getMaxHealth()),
            Duration.ofSeconds(3));
        break;

      case "xp":
        actionBar.send(player, ActionBarHelper.Quick.xpBar(450, 1000), Duration.ofSeconds(3));
        break;

      case "cooldown":
        actionBar.send(
            player, ActionBarHelper.Quick.cooldownBar(5000, 10000), Duration.ofSeconds(3));
        break;

      case "loading":
        actionBar.sendAnimated(
            player, ActionBarHelper.Quick.loadingAnimation(), 10L, Duration.ofSeconds(5));
        break;

      case "spinner":
        actionBar.sendAnimated(
            player, ActionBarHelper.Quick.spinnerAnimation(), 2L, Duration.ofSeconds(5));
        break;

      case "rainbow":
        actionBar.sendAnimated(
            player,
            ActionBarHelper.Quick.rainbowAnimation("Rainbow Text!"),
            5L,
            Duration.ofSeconds(10));
        break;

      case "persistent":
        actionBar.sendPersistentMini(
            player, "<gradient:green:aqua>Persistent Message (stays until cleared)</gradient>");
        player.sendMessage("§aSet persistent action bar. Use /actionbar clear to remove.");
        break;

      case "clear":
        actionBar.clear(player);
        player.sendMessage("§7Action bar cleared.");
        break;

      default:
        player.sendMessage("§cUnknown example. Use /actionbar for help.");
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return EXAMPLES.stream()
          .filter(example -> example.toLowerCase().startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }
    return List.of();
  }
}
