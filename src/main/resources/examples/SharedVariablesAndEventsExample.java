package examples;

import dev.mukulx.javaskript.api.event.EventHelper;
import dev.mukulx.javaskript.api.player.Players;
import dev.mukulx.javaskript.api.variable.VariableHelper;
import dev.mukulx.javaskript.api.variable.Variables;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Demonstrates shared inter-script variables and the custom Pub/Sub event bus.
 *
 * <p>Shared variables allow any script on the server to read, write, and atomically increment
 * shared state across isolated ClassLoaders.
 *
 * <p>Custom events allow scripts to broadcast and listen to custom events without having
 * compile-time class dependencies on each other.
 */
public class SharedVariablesAndEventsExample implements Listener {

  // Auto-injected helpers
  private VariableHelper variables;
  private EventHelper events;

  public void onEnable() {
    // 1. Listen for custom events broadcast by ANY script on the server
    events.onCustom(
        "quest_completed",
        ctx -> {
          Player player = ctx.getPlayer();
          String questName = ctx.get(1, String.class, "Unknown Quest");
          int reward = ctx.get(2, Integer.class, 0);

          // Update shared server stats
          variables.increment("total_quests_completed", 1);

          if (player != null) {
            Players.msg(
                player,
                "<green>Quest Finished: <gold>"
                    + questName
                    + " <gray>(+$"
                    + reward
                    + ")</gray></green>");
          }
        });

    // 2. Set an in-memory shared variable
    variables.set("motd_event", "Double EXP Weekend Active!");

    // 3. Set a persistent variable (saved to script-data/variables.json, survives restarts)
    if (!variables.hasPersistent("server_jackpot")) {
      variables.setPersistent("server_jackpot", 10000);
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // Increment shared player visit counter
    long visits = variables.increment("total_server_joins", 1);

    // Read shared variable via static facade
    String motd = Variables.getString("motd_event", "Welcome!");

    Players.msg(
        player, "<yellow>" + motd + "</yellow> <gray>(Total logins: " + visits + ")</gray>");

    // Fire a custom event that any other script can intercept
    events.fire("player_session_start", player, System.currentTimeMillis());
  }
}
