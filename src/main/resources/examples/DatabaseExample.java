import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.DatabaseHelper;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@FoliaSupport
public class DatabaseExample implements Listener {

  private DatabaseHelper database;

  public DatabaseExample() {
    if (database == null) {
      database = new DatabaseHelper(JavaSkriptPlugin.getInstance(), "DatabaseExample.java");
    }

    database.createTable(
        "players",
        "uuid TEXT PRIMARY KEY",
        "name TEXT NOT NULL",
        "join_count INTEGER DEFAULT 0",
        "last_join INTEGER");
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (event == null || event.getPlayer() == null) return;

    var player = event.getPlayer();
    String uuid = player.getUniqueId().toString();

    var result = database.executeQuery("SELECT * FROM players WHERE uuid = ?", uuid);

    if (result.isEmpty()) {
      Map<String, Object> data = new HashMap<>();
      data.put("uuid", uuid);
      data.put("name", player.getName());
      data.put("join_count", 1);
      data.put("last_join", System.currentTimeMillis());

      database.insert("players", data);
      player.sendMessage("Welcome for the first time");
    } else {
      Map<String, Object> data = new HashMap<>();
      data.put("name", player.getName());
      data.put("join_count", ((Number) result.get(0).get("join_count")).intValue() + 1);
      data.put("last_join", System.currentTimeMillis());

      database.update("players", data, "uuid = ?", uuid);

      int joinCount = ((Number) result.get(0).get("join_count")).intValue() + 1;
      player.sendMessage("Welcome back! You've joined " + joinCount + " times");
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    // Database connection is managed automatically
  }
}
