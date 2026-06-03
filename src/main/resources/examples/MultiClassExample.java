import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

// Main public class - this is what gets registered
@FoliaSupport
public class MultiClassExample implements Listener, CommandExecutor {

  private PlayerManager playerManager = new PlayerManager();
  private MessageManager messageManager = new MessageManager();

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (event == null || event.getPlayer() == null) return;

    var player = event.getPlayer();
    playerManager.addPlayer(player);
    messageManager.sendWelcome(player);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players").color(NamedTextColor.RED));
      return true;
    }

    int count = playerManager.getPlayerCount();
    player.sendMessage(Component.text("Total players tracked: " + count, NamedTextColor.GOLD));

    return true;
  }
}

// Package-private class - manages players
class PlayerManager {
  private Map<UUID, PlayerData> players = new HashMap<>();

  void addPlayer(Player player) {
    players.put(player.getUniqueId(), new PlayerData(player.getName()));
  }

  int getPlayerCount() {
    return players.size();
  }

  PlayerData getPlayer(UUID uuid) {
    return players.get(uuid);
  }
}

// Package-private class - manages messages
class MessageManager {
  void sendWelcome(Player player) {
    player.sendMessage(Component.text("Welcome to the server", NamedTextColor.GOLD));
    player.sendMessage(Component.text("Type /multiclass for info", NamedTextColor.YELLOW));
  }

  void sendGoodbye(Player player) {
    player.sendMessage(Component.text("Goodbye", NamedTextColor.RED));
  }
}

// Package-private data class
class PlayerData {
  private String name;
  private long joinTime;

  PlayerData(String name) {
    this.name = name;
    this.joinTime = System.currentTimeMillis();
  }

  String getName() {
    return name;
  }

  long getJoinTime() {
    return joinTime;
  }
}
