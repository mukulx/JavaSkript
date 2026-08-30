package examples;

import dev.mukulx.javaskript.api.event.EventHelper;
import dev.mukulx.javaskript.api.event.EventSubscription;
import dev.mukulx.javaskript.api.player.PlayerHelper;
import dev.mukulx.javaskript.api.player.Players;
import java.time.Duration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Modern Functional Events & Expressive Player Utilities Example.
 *
 * <p>Demonstrates:
 *
 * <ul>
 *   <li>1-Line Lambda Event Listeners (No Listener classes or @EventHandler needed)
 *   <li>Run-Once Events (auto-unsubscribes after 1 execution)
 *   <li>Predicate-filtered Events (e.g. only runs on DIAMOND_ORE)
 *   <li>Auto-expiring subscriptions with timeouts
 *   <li>1-Line Player titles, sounds, action bars, healing, and MiniMessage gradients
 *   <li>Static {@link Players} and injected {@link PlayerHelper} usage
 * </ul>
 */
public class EventAndPlayerExample {

  // Auto-injected by JavaSkript
  private EventHelper events;
  private PlayerHelper players;

  public void onEnable() {

    // 1. Single-line Welcome Event with Sound & Title
    events.on(
        PlayerJoinEvent.class,
        e -> {
          Player player = e.getPlayer();

          // 1-line audio-visual welcome using Players static facade:
          Players.title(
              player,
              "<gradient:#ff5555:#ffaa00><bold>WELCOME</bold></gradient>",
              "<gray>Enjoy your stay on the server!</gray>");
          Players.sound(player, Sound.ENTITY_PLAYER_LEVELUP);
          Players.msg(
              player,
              "<green>✦ Welcome back, <yellow>"
                  + player.getName()
                  + "</yellow>! (Lambda Event Handled)");
        });

    // 2. Filtered Event: Only triggers when DIAMOND_ORE or DEEPSLATE_DIAMOND_ORE is mined
    events.on(
        BlockBreakEvent.class,
        e ->
            e.getBlock().getType() == Material.DIAMOND_ORE
                || e.getBlock().getType() == Material.DEEPSLATE_DIAMOND_ORE,
        e -> {
          Player player = e.getPlayer();
          players.msg(
              player,
              "<aqua>💎 You unearthed a precious Diamond! <gray>(Auto-filtered event)</gray>");
          players.sound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
          players.actionBar(player, "<gold>+1 Diamond Mined!");
        });

    // 3. Run-Once Event: Auto-unregisters after the first click!
    // Great for tutorial steps, confirmation dialogues, and one-off quest triggers
    events.once(
        PlayerInteractEvent.class,
        e -> {
          Player player = e.getPlayer();
          Players.msg(
              player,
              "<green>✔ Tutorial Complete! <gray>You made your first world interaction.</gray>");
          Players.sound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
        });

    // 4. Advanced Subscription: Max 5 executions + auto-expires after 5 minutes
    EventSubscription<EntityDamageByEntityEvent> combatSub =
        events
            .on(
                EntityDamageByEntityEvent.class,
                e -> {
                  if (e.getDamager() instanceof Player damager
                      && e.getEntity() instanceof Player victim) {
                    Players.actionBar(
                        damager,
                        "<red>⚔ Dealt <yellow>"
                            + String.format("%.1f", e.getFinalDamage())
                            + " DMG <red>to <yellow>"
                            + victim.getName());
                  }
                })
            .maxExecutions(50)
            .expireAfter(Duration.ofMinutes(5))
            .onExpire(
                () -> {
                  Players.broadcast("<gray>[Combat] 5-minute combat tracker expired.");
                });
  }
}
