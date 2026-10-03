package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.mannequin.CustomMannequin;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Pose;

/**
 * Mannequin subsystem helper for Paper 1.21.11+ servers.
 *
 * <p>Enables ultra-fast creation, customization, and interaction management for native Minecraft
 * Mannequins without external plugins or Citizens. Automatically tracks and despawns created
 * mannequins when a script reloads or unloads.
 */
public class MannequinHelper {

  private final JavaSkriptPlugin plugin;
  private final String scriptKey;
  private final List<CustomMannequin> activeMannequins = new CopyOnWriteArrayList<>();

  public MannequinHelper(JavaSkriptPlugin plugin) {
    this(plugin, "api");
  }

  public MannequinHelper(JavaSkriptPlugin plugin, String scriptKey) {
    this.plugin = plugin;
    this.scriptKey = scriptKey != null ? scriptKey : "api";
  }

  /**
   * Create a new fluent CustomMannequin builder at the specified location. Call {@link
   * CustomMannequin#spawn()} to spawn it.
   */
  public CustomMannequin create(Location location) {
    return new CustomMannequin(plugin, this, location);
  }

  /**
   * Create a new fluent CustomMannequin builder with a custom name.
   */
  public CustomMannequin create(Location location, String name) {
    return create(location).name(name);
  }

  /**
   * Create a new fluent CustomMannequin builder with an Adventure Component name.
   */
  public CustomMannequin create(Location location, Component name) {
    return create(location).name(name);
  }

  /**
   * Create and immediately spawn a mannequin with a name and player skin.
   */
  public CustomMannequin spawn(Location location, String name, String skinPlayerName) {
    return create(location).name(name).skin(skinPlayerName).standing().spawn();
  }

  /**
   * Create and immediately spawn a mannequin with a Component name and player skin.
   */
  public CustomMannequin spawn(Location location, Component name, String skinPlayerName) {
    return create(location).name(name).skin(skinPlayerName).standing().spawn();
  }

  /**
   * Wrap an existing Bukkit/Paper Mannequin entity into a CustomMannequin controller.
   */
  public CustomMannequin wrap(Mannequin entity) {
    if (entity == null) {
      throw new IllegalArgumentException("Mannequin entity cannot be null");
    }
    CustomMannequin custom = new CustomMannequin(plugin, this, entity);
    track(custom);
    return custom;
  }

  /**
   * Track an active mannequin for automatic script cleanup.
   */
  public void track(CustomMannequin mannequin) {
    if (mannequin != null && !activeMannequins.contains(mannequin)) {
      activeMannequins.add(mannequin);
    }
  }

  /**
   * Stop tracking an active mannequin.
   */
  public void untrack(CustomMannequin mannequin) {
    if (mannequin != null) {
      activeMannequins.remove(mannequin);
    }
  }

  /**
   * Remove and despawn a specific mannequin.
   */
  public void remove(CustomMannequin mannequin) {
    if (mannequin != null) {
      mannequin.remove();
      activeMannequins.remove(mannequin);
    }
  }

  /**
   * Despawn and remove all mannequins tracked by this script. Automatically invoked on script
   * unload or reload.
   */
  public void removeAll() {
    for (CustomMannequin mannequin : new ArrayList<>(activeMannequins)) {
      try {
        mannequin.remove();
      } catch (Throwable t) {
        plugin.getLogger().warning("Error despawning mannequin: " + t.getMessage());
      }
    }
    activeMannequins.clear();
    plugin.debug("Removed all mannequins for script: " + scriptKey);
  }

  /**
   * Get all active mannequins tracked by this helper.
   */
  public Collection<CustomMannequin> getAll() {
    return Collections.unmodifiableList(activeMannequins);
  }

  /**
   * Find mannequins near a given location.
   */
  public List<CustomMannequin> getNearby(Location location, double radius) {
    if (location == null || location.getWorld() == null || radius <= 0) {
      return Collections.emptyList();
    }
    double rSq = radius * radius;
    return activeMannequins.stream()
        .filter(m -> m.isSpawned() && m.location() != null)
        .filter(m -> m.location().getWorld().equals(location.getWorld()))
        .filter(m -> m.location().distanceSquared(location) <= rSq)
        .collect(Collectors.toList());
  }

  /**
   * Find tracked mannequins with a specific scoreboard tag.
   */
  public List<CustomMannequin> findByTag(String tag) {
    if (tag == null || tag.isBlank()) {
      return Collections.emptyList();
    }
    return activeMannequins.stream()
        .filter(m -> m.hasTag(tag))
        .collect(Collectors.toList());
  }

  /**
   * Find a tracked mannequin by its entity UUID.
   */
  public Optional<CustomMannequin> findById(UUID uuid) {
    if (uuid == null) {
      return Optional.empty();
    }
    return activeMannequins.stream()
        .filter(m -> uuid.equals(m.getUniqueId()))
        .findFirst();
  }

  /**
   * Valid poses supported by Mannequins in Paper 1.21.11+.
   */
  public static Set<Pose> validPoses() {
    return CustomMannequin.validPoses();
  }

  /**
   * Default mannequin profile.
   */
  public static io.papermc.paper.datacomponent.item.ResolvableProfile defaultProfile() {
    try {
      return Mannequin.defaultProfile();
    } catch (Throwable t) {
      return null;
    }
  }

  /**
   * Default mannequin description component.
   */
  public static Component defaultDescription() {
    try {
      return Mannequin.defaultDescription();
    } catch (Throwable t) {
      return Component.empty();
    }
  }
}
