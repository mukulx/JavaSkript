import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

@FoliaSupport
public class HealCommand implements CommandExecutor, TabCompleter {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players can use this").color(NamedTextColor.RED));
      return true;
    }

    player.setHealth(player.getMaxHealth());
    player.setFoodLevel(20);
    player.sendMessage(Component.text("You have been healed").color(NamedTextColor.GREEN));
    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    return List.of();
  }
}
