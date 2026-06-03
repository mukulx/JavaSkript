import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

@FoliaSupport
public class FlyCommand implements CommandExecutor, TabCompleter {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length > 0) {
      return handleOtherPlayer(sender, args);
    }

    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          Component.text("Only players can use this command").color(NamedTextColor.RED));
      return true;
    }

    toggleFlight(player, player);
    return true;
  }

  private boolean handleOtherPlayer(CommandSender sender, String[] args) {
    if (!sender.hasPermission("fly.others")) {
      sender.sendMessage(
          Component.text("You don't have permission to toggle flight for others")
              .color(NamedTextColor.RED));
      return true;
    }

    Player target = Bukkit.getPlayer(args[0]);

    if (target == null) {
      sender.sendMessage(Component.text("Player not found: " + args[0]).color(NamedTextColor.RED));
      return true;
    }

    toggleFlight(target, sender);

    if (!sender.equals(target)) {
      sender.sendMessage(
          Component.text("Toggled flight for ")
              .color(NamedTextColor.GREEN)
              .append(Component.text(target.getName(), NamedTextColor.YELLOW))
              .append(Component.text(": ", NamedTextColor.GREEN))
              .append(
                  Component.text(
                      target.getAllowFlight() ? "ENABLED" : "DISABLED",
                      target.getAllowFlight() ? NamedTextColor.GREEN : NamedTextColor.RED,
                      TextDecoration.BOLD)));
    }

    return true;
  }

  private void toggleFlight(Player player, CommandSender executor) {
    if (player == null) return;

    boolean canFly = !player.getAllowFlight();
    player.setAllowFlight(canFly);

    if (!canFly) {
      player.setFlying(false);
    }

    if (canFly) {
      player.sendMessage(
          Component.text("✈ ", NamedTextColor.AQUA)
              .append(Component.text("Flight ", NamedTextColor.GREEN))
              .append(Component.text("ENABLED", NamedTextColor.GREEN, TextDecoration.BOLD)));
      player.sendMessage(Component.text("  Double-jump to start flying", NamedTextColor.GRAY));
    } else {
      player.sendMessage(
          Component.text("✈ ", NamedTextColor.GRAY)
              .append(Component.text("Flight ", NamedTextColor.RED))
              .append(Component.text("DISABLED", NamedTextColor.RED, TextDecoration.BOLD)));
    }
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1 && sender.hasPermission("fly.others")) {
      String input = args[0].toLowerCase();

      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase().startsWith(input))
          .collect(Collectors.toList());
    }

    return List.of();
  }
}
