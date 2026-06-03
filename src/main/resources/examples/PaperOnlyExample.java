import dev.mukulx.javaskript.script.PaperOnly;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Example script that only works on Paper (not Folia).
 *
 * <p>Mark your script with @PaperOnly if it uses Paper-specific APIs that don't work on Folia. This
 * prevents the script from loading on Folia servers.
 *
 * <p>Common reasons for Paper-only scripts: - Uses Bukkit.getScheduler() directly (use
 * ScriptScheduler instead) - Uses global world access without entity context - Uses synchronous
 * chunk loading - Uses deprecated Bukkit APIs
 */
@PaperOnly
public class PaperOnlyExample implements Listener {

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (event == null || event.getPlayer() == null) return;

    var player = event.getPlayer();

    // This uses Bukkit.getScheduler() which doesn't work on Folia
    Bukkit.getScheduler()
        .runTaskLater(
            Bukkit.getPluginManager().getPlugin("JavaSkript"),
            () -> {
              player.sendMessage(
                  Component.text("This only works on Paper!").color(NamedTextColor.RED));
            },
            100L);
  }
}
