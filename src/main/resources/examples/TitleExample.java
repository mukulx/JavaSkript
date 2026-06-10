import dev.mukulx.javaskript.api.TitleHelper;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

@FoliaSupport
public class TitleExample implements CommandExecutor, TabCompleter {

  private TitleHelper title;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can use this command");
      return true;
    }

    if (args.length == 0) {
      player.sendMessage("§eTitleHelper Examples:");
      player.sendMessage("§7/title simple §f- Simple title");
      player.sendMessage("§7/title subtitle §f- Title with subtitle");
      player.sendMessage("§7/title mini §f- MiniMessage format");
      player.sendMessage("§7/title gradient §f- Gradient title");
      player.sendMessage("§7/title builder §f- Using builder");
      player.sendMessage("§7/title success §f- Success message");
      player.sendMessage("§7/title error §f- Error message");
      player.sendMessage("§7/title warning §f- Warning message");
      player.sendMessage("§7/title info §f- Info message");
      player.sendMessage("§7/title custom §f- Custom timings");
      player.sendMessage("§7/title broadcast §f- Broadcast to all");
      player.sendMessage("§7/title clear §f- Clear title");
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "simple":
        title.send(player, "Simple Title");
        break;

      case "subtitle":
        title.send(player, "Main Title", "This is the subtitle");
        break;

      case "mini":
        title.sendMini(
            player,
            "<gradient:gold:yellow><bold>Fancy Title</bold></gradient>",
            "<aqua>MiniMessage Subtitle</aqua>");
        break;

      case "gradient":
        title.send(
            player,
            TitleHelper.Quick.gradient("Rainbow Title", NamedTextColor.RED, NamedTextColor.BLUE),
            TitleHelper.Quick.gradient(
                "Gradient Subtitle", NamedTextColor.AQUA, NamedTextColor.GREEN));
        break;

      case "builder":
        title
            .builder()
            .titleMini("<gradient:green:aqua>Builder Pattern</gradient>")
            .subtitleMini("<gray>Very flexible</gray>")
            .fadeIn(20)
            .stay(100)
            .fadeOut(20)
            .send(player);
        break;

      case "success":
        player.showTitle(TitleHelper.Quick.success("Operation completed!"));
        break;

      case "error":
        player.showTitle(TitleHelper.Quick.error("Something went wrong!"));
        break;

      case "warning":
        player.showTitle(TitleHelper.Quick.warning("Be careful!"));
        break;

      case "info":
        player.showTitle(TitleHelper.Quick.info("Did you know?"));
        break;

      case "custom":
        title.send(player, "Custom Timings", "Fade: 1s, Stay: 5s, Fade: 1s", 20, 100, 20);
        break;

      case "broadcast":
        title.broadcastMini(
            "<gradient:red:yellow>Server Announcement</gradient>",
            "<white>Everyone sees this!</white>");
        player.sendMessage("§aBroadcasted title to all players");
        break;

      case "clear":
        title.clear(player);
        player.sendMessage("§7Title cleared");
        break;

      default:
        player.sendMessage("§cUnknown example. Use /title for help.");
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return Arrays.asList(
          "simple",
          "subtitle",
          "mini",
          "gradient",
          "builder",
          "success",
          "error",
          "warning",
          "info",
          "custom",
          "broadcast",
          "clear");
    }
    return List.of();
  }
}
