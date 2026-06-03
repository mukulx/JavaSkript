// Placeholder example - Shows how to register custom placeholders
// Requires PlaceholderAPI to be installed
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.PlaceholderHelper;
import dev.mukulx.javaskript.script.FoliaSupport;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

@FoliaSupport
public class PlaceholderExample implements Listener {

  // PlaceholderHelper will be automatically injected!
  private PlaceholderHelper placeholders;

  @EventHandler
  public void onPluginEnable(PluginEnableEvent event) {
    if (event == null || event.getPlugin() == null) return;

    // Only register when JavaSkript loads
    if (!event.getPlugin().getName().equals("JavaSkript")) return;

    // Get PlaceholderHelper if not injected
    if (placeholders == null) {
      placeholders =
          new PlaceholderHelper(JavaSkriptPlugin.getInstance(), "PlaceholderExample.java");
    }

    // Check if PlaceholderAPI is available
    if (!placeholders.isPlaceholderAPIAvailable()) {
      System.out.println("[PlaceholderExample] PlaceholderAPI not found!");
      return;
    }

    // Register simple placeholder
    // Usage: %placeholderexample_server%
    placeholders.registerPlaceholder("server", "My Awesome Server");

    // Register dynamic placeholder with player context
    // Usage: %placeholderexample_health%
    placeholders.registerPlaceholder(
        "health",
        (player, params) -> {
          if (player == null || !player.isOnline()) {
            return "N/A";
          }
          var onlinePlayer = player.getPlayer();
          if (onlinePlayer == null) {
            return "N/A";
          }
          return String.format("%.1f", onlinePlayer.getHealth());
        });

    // Register placeholder with parameters
    // Usage: %placeholderexample_online%
    placeholders.registerPlaceholder(
        "online",
        (player, params) -> {
          return String.valueOf(Bukkit.getOnlinePlayers().size());
        });

    // Register placeholder that uses other placeholders
    // Usage: %placeholderexample_status%
    placeholders.registerPlaceholder(
        "status",
        (player, params) -> {
          if (player == null || !player.isOnline()) {
            return "Offline";
          }

          var onlinePlayer = player.getPlayer();
          if (onlinePlayer == null) {
            return "Offline";
          }

          // You can parse other placeholders
          String parsed =
              placeholders.parsePlaceholders(
                  player,
                  "Health: %placeholderexample_health% | Online: %placeholderexample_online%");
          return parsed;
        });

    System.out.println("[PlaceholderExample] Registered placeholders!");
    System.out.println("  - %placeholderexample_server%");
    System.out.println("  - %placeholderexample_health%");
    System.out.println("  - %placeholderexample_online%");
    System.out.println("  - %placeholderexample_status%");
  }
}
