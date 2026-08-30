package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.hologram.Hologram;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Modern Paper Display Entity Hologram Helper for JavaSkript scripts.
 *
 * <p>Enables ultra-fast, zero-lag creation of floating text, 3D items, and blocks without requiring
 * armor stands or external plugins like DecentHolograms.
 */
public class HologramHelper {

  private final JavaSkriptPlugin plugin;
  private final List<Hologram> activeHolograms = new ArrayList<>();

  public HologramHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Create a new fluent Hologram builder at the given location.
   *
   * @param location The spawn location
   * @return Hologram instance
   */
  public Hologram create(Location location) {
    Hologram hologram = new Hologram(plugin, location);
    activeHolograms.add(hologram);
    return hologram;
  }

  /**
   * Create a multi-line text hologram and return the builder.
   *
   * @param location The spawn location
   * @param lines Text lines
   * @return Hologram instance
   */
  public Hologram create(Location location, String... lines) {
    Hologram hologram = create(location);
    for (String line : lines) {
      hologram.line(line);
    }
    return hologram;
  }

  /**
   * Create a multi-line text hologram from components.
   *
   * @param location The spawn location
   * @param lines Component lines
   * @return Hologram instance
   */
  public Hologram create(Location location, Component... lines) {
    Hologram hologram = create(location);
    for (Component line : lines) {
      hologram.line(line);
    }
    return hologram;
  }

  /**
   * Create a single-line floating text hologram and spawn it immediately.
   *
   * @param location The spawn location
   * @param text The text
   * @return The spawned Hologram
   */
  public Hologram createText(Location location, String text) {
    return create(location).line(text).spawn();
  }

  /**
   * Create a single-line floating text hologram with Component.
   *
   * @param location The spawn location
   * @param text The Component
   * @return The spawned Hologram
   */
  public Hologram createText(Location location, Component text) {
    return create(location).line(text).spawn();
  }

  /**
   * Create a floating 3D item showcase and spawn it immediately.
   *
   * @param location The spawn location
   * @param item The ItemStack
   * @return The spawned Hologram
   */
  public Hologram createItem(Location location, ItemStack item) {
    return create(location).item(item).spawn();
  }

  /**
   * Create a floating 3D block showcase and spawn it immediately.
   *
   * @param location The spawn location
   * @param material The block Material
   * @return The spawned Hologram
   */
  public Hologram createBlock(Location location, Material material) {
    return create(location).block(material).spawn();
  }

  /**
   * Remove and despawn a specific hologram.
   *
   * @param hologram The hologram to remove
   */
  public void remove(Hologram hologram) {
    if (hologram != null) {
      hologram.remove();
      activeHolograms.remove(hologram);
    }
  }

  /**
   * Despawn and remove all holograms created by this helper. Called automatically when a script
   * unloads.
   */
  public void removeAll() {
    for (Hologram hologram : new ArrayList<>(activeHolograms)) {
      hologram.remove();
    }
    activeHolograms.clear();
    plugin.debug("Removed all holograms for script");
  }

  /**
   * Get all active holograms tracked by this helper.
   *
   * @return Unmodifiable collection of active holograms
   */
  public Collection<Hologram> getAll() {
    return Collections.unmodifiableList(activeHolograms);
  }

  /**
   * Find holograms near a given location.
   *
   * @param location Center location
   * @param radius Search radius in blocks
   * @return List of holograms within radius
   */
  public List<Hologram> findNearby(Location location, double radius) {
    if (location == null || location.getWorld() == null) {
      return Collections.emptyList();
    }
    double radiusSq = radius * radius;
    List<Hologram> found = new ArrayList<>();
    for (Hologram holo : activeHolograms) {
      Location hLoc = holo.getLocation();
      if (hLoc != null && hLoc.getWorld().equals(location.getWorld())) {
        if (hLoc.distanceSquared(location) <= radiusSq) {
          found.add(holo);
        }
      }
    }
    return found;
  }
}
