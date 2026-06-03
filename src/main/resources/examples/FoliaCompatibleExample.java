import dev.mukulx.javaskript.script.FoliaSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Example script that works on both Paper and Folia.
 *
 * <p>Mark your script with @FoliaSupport to indicate it's Folia-compatible. This prevents warnings
 * on Folia servers.
 */
@FoliaSupport
public class FoliaCompatibleExample implements Listener {

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (event == null || event.getPlayer() == null) return;

    var player = event.getPlayer();

    // This code works on both Paper and Folia
    player.sendMessage(
        Component.text("Welcome! This script works on both Paper and Folia!")
            .color(NamedTextColor.GREEN));
  }
}
