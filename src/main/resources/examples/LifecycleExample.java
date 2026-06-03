// Lifecycle Example - Demonstrates onEnable() and onDisable() methods
//
// This script shows proper resource management and lifecycle handling.
// It demonstrates:
// - onEnable(): Initialize resources when script loads
// - onDisable(): Clean up resources when script unloads
// - Proper state management
// - Graceful reload handling
//
// Lifecycle methods are OPTIONAL but recommended for scripts that:
// - Manage custom resources (threads, connections, etc.)
// - Need to save data on unload
// - Want to initialize state on load

import dev.mukulx.javaskript.api.ScriptScheduler;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@FoliaSupport
public class LifecycleExample implements Listener {

  // Injected by JavaSkript
  private ScriptScheduler scheduler;

  // Script state
  private boolean initialized = false;
  private AtomicInteger totalJoins = new AtomicInteger(0);
  private Map<UUID, Integer> playerJoinCount = new ConcurrentHashMap<>();

  // Constructor - runs FIRST
  // APIs are NOT available yet!
  public LifecycleExample() {
    // ✓ GOOD: Initialize simple fields
    this.initialized = false;
    this.totalJoins = new AtomicInteger(0);
    this.playerJoinCount = new ConcurrentHashMap<>();

    // ✗ BAD: Don't use APIs here (they're null!)
    // scheduler.runLater(() -> {}, 20L); // NullPointerException!

    Bukkit.getLogger().info("[LifecycleExample] Constructor called");
  }

  // onEnable() - runs AFTER APIs are injected
  // This is where you should initialize resources
  public void onEnable() {
    // Prevent double initialization on reload
    if (initialized) {
      Bukkit.getLogger().warning("[LifecycleExample] Already initialized, skipping...");
      return;
    }

    // ✓ GOOD: Now APIs are available
    Bukkit.getLogger().info("[LifecycleExample] Enabling script...");

    // Start a repeating task to broadcast stats
    scheduler.everyMinute(
        () -> {
          int total = totalJoins.get();
          int online = playerJoinCount.size();

          Bukkit.broadcast(
              Component.text("Stats: ", NamedTextColor.GOLD)
                  .append(Component.text(total + " total joins, ", NamedTextColor.YELLOW))
                  .append(Component.text(online + " players tracked", NamedTextColor.YELLOW)));
        });

    // Load saved data (in a real script, you'd load from database/file)
    loadSavedData();

    initialized = true;
    Bukkit.getLogger().info("[LifecycleExample] Script enabled successfully!");
  }

  // onDisable() - runs BEFORE script is unloaded
  // This is where you should clean up resources
  public void onDisable() {
    Bukkit.getLogger().info("[LifecycleExample] Disabling script...");

    // Save data before unload
    saveFinalData();

    // Clear state
    playerJoinCount.clear();
    initialized = false;

    // Note: You don't need to manually:
    // - Unregister event listeners (automatic)
    // - Cancel scheduler tasks (automatic)
    // - Unregister commands (automatic)

    Bukkit.getLogger().info("[LifecycleExample] Script disabled! Total joins: " + totalJoins.get());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!initialized) {
      Bukkit.getLogger().warning("[LifecycleExample] Not initialized yet!");
      return;
    }

    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    // Increment counters
    int total = totalJoins.incrementAndGet();
    int playerCount = playerJoinCount.compute(uuid, (k, v) -> v == null ? 1 : v + 1);

    // Send welcome message
    player.sendMessage(
        Component.text("Welcome! ", NamedTextColor.GREEN)
            .append(Component.text("You've joined ", NamedTextColor.GRAY))
            .append(Component.text(playerCount + " times", NamedTextColor.GOLD)));

    player.sendMessage(
        Component.text("Total server joins: ", NamedTextColor.GRAY)
            .append(Component.text(total, NamedTextColor.YELLOW)));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    if (!initialized) {
      return;
    }

    // Keep tracking even after quit (for demonstration)
    // In a real script, you might save to database here
  }

  private void loadSavedData() {
    // In a real script, you would load from database or file
    // For this example, we'll just log
    Bukkit.getLogger().info("[LifecycleExample] Loading saved data...");

    // Example: Load from database
    // var results = database.executeQuery("SELECT * FROM stats");
    // if (!results.isEmpty()) {
    //     totalJoins.set((Integer) results.get(0).get("total_joins"));
    // }

    Bukkit.getLogger().info("[LifecycleExample] Data loaded successfully");
  }

  private void saveFinalData() {
    // In a real script, you would save to database or file
    Bukkit.getLogger().info("[LifecycleExample] Saving final data...");

    int total = totalJoins.get();
    int tracked = playerJoinCount.size();

    // Example: Save to database
    // database.execute(
    //     "INSERT OR REPLACE INTO stats (id, total_joins, tracked_players) VALUES (1, ?, ?)",
    //     total, tracked
    // );

    Bukkit.getLogger()
        .info(
            "[LifecycleExample] Saved: " + total + " total joins, " + tracked + " players tracked");
  }
}
