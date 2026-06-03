import dev.mukulx.javaskript.script.FoliaSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@FoliaSupport
public class WelcomeScript implements Listener {

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (event == null || event.getPlayer() == null) return;

    var player = event.getPlayer();
    var message = Component.text("Welcome, " + player.getName()).color(NamedTextColor.GOLD);

    player.sendMessage(message);
  }
}
