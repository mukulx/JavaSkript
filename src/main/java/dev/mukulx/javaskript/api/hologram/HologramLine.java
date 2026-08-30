package dev.mukulx.javaskript.api.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Display;

/** Represents a single line (Text, Item, or Block) within a {@link Hologram}. */
public interface HologramLine {

  /** Get the spawned Bukkit Display entity (or null if not spawned). */
  Display getEntity();

  /** Spawn the display entity at the specified location. */
  void spawn(Location location);

  /** Despawn and remove the entity from the world. */
  void despawn();

  /** Move / teleport the line to a new location. */
  void teleport(Location location);

  /** Re-render and update line contents / placeholders. */
  void update();

  /** Get the vertical height consumed by this line. */
  double getHeight();

  /** Check if the line entity is alive and valid. */
  boolean isSpawned();
}
