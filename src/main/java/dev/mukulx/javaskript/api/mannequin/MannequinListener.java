package dev.mukulx.javaskript.api.mannequin;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Event listener managing player interactions and attacks on custom JavaSkript mannequins.
 */
public class MannequinListener implements Listener {

  private final JavaSkriptPlugin plugin;
  private static final Map<UUID, CustomMannequin> REGISTRY = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> LAST_CLICK = new ConcurrentHashMap<>();

  public MannequinListener(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  public static void register(CustomMannequin mannequin) {
    if (mannequin != null && mannequin.getUniqueId() != null) {
      REGISTRY.put(mannequin.getUniqueId(), mannequin);
    }
  }

  public static void unregister(CustomMannequin mannequin) {
    if (mannequin != null && mannequin.getUniqueId() != null) {
      REGISTRY.remove(mannequin.getUniqueId());
    }
  }

  public static CustomMannequin get(UUID uuid) {
    return uuid != null ? REGISTRY.get(uuid) : null;
  }

  public static void clearRegistry() {
    REGISTRY.clear();
    LAST_CLICK.clear();
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
  public void onPlayerInteract(PlayerInteractEntityEvent event) {
    Entity clicked = event.getRightClicked();
    if (!(clicked instanceof Mannequin)) {
      return;
    }

    CustomMannequin custom = REGISTRY.get(clicked.getUniqueId());
    if (custom == null) {
      return;
    }

    // Cancel default equipment manipulation if locked
    if (custom.isEquipmentLocked()) {
      event.setCancelled(true);
    }

    Player player = event.getPlayer();
    EquipmentSlot hand = event.getHand();

    // Debounce duplicate interaction packets (client sends main-hand and off-hand sequentially)
    long now = System.currentTimeMillis();
    Long last = LAST_CLICK.put(player.getUniqueId(), now);
    boolean debounced = (last != null && (now - last) < 200);

    // Call hand-specific interact callback
    custom.handleInteract(player, hand);

    // Trigger main click action once per click
    if (hand == EquipmentSlot.HAND && !debounced) {
      custom.handleClick(player);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
  public void onPlayerAttack(PrePlayerAttackEntityEvent event) {
    Entity entity = event.getAttacked();
    if (!(entity instanceof Mannequin)) {
      return;
    }

    CustomMannequin custom = REGISTRY.get(entity.getUniqueId());
    if (custom == null) {
      return;
    }

    if (custom.isInvulnerable()) {
      event.setCancelled(true);
    }

    custom.handleAttack(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    Entity entity = event.getEntity();
    if (!(entity instanceof Mannequin)) {
      return;
    }

    CustomMannequin custom = REGISTRY.get(entity.getUniqueId());
    if (custom == null) {
      return;
    }

    if (custom.isInvulnerable()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEntityRemove(EntityRemoveFromWorldEvent event) {
    UUID uuid = event.getEntity().getUniqueId();
    REGISTRY.remove(uuid);
  }
}
