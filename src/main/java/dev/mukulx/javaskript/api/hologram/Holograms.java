package dev.mukulx.javaskript.api.hologram;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.HologramHelper;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Static shorthand facade for Paper Display Entity Holograms.
 *
 * <p>Enables 1-line creation of 3D floating text, items, and blocks anywhere:
 *
 * <pre>{@code
 * Holograms.text(loc, "<gradient:#ff5555:#ffaa00><bold>SPAWN</bold></gradient>");
 * Holograms.item(loc, new ItemStack(Material.DIAMOND_SWORD));
 * Holograms.create(loc, "<gold>Line 1", "<yellow>Line 2").spawn();
 * }</pre>
 */
public final class Holograms {

  private static HologramHelper helper;

  private Holograms() {}

  private static HologramHelper get() {
    if (helper == null) {
      helper = new HologramHelper(JavaSkriptPlugin.getInstance());
    }
    return helper;
  }

  public static void setInstance(HologramHelper h) {
    helper = h;
  }

  /** Create a fluent Hologram builder at the location. */
  public static Hologram create(Location location) {
    return get().create(location);
  }

  /** Create a multi-line text hologram builder at the location. */
  public static Hologram create(Location location, String... lines) {
    return get().create(location, lines);
  }

  /** Create a multi-line text hologram builder at the location with components. */
  public static Hologram create(Location location, Component... lines) {
    return get().create(location, lines);
  }

  /** Create a single-line floating text hologram and spawn it immediately. */
  public static Hologram text(Location location, String text) {
    return get().createText(location, text);
  }

  /** Create a single-line floating text hologram with Component and spawn it immediately. */
  public static Hologram text(Location location, Component text) {
    return get().createText(location, text);
  }

  /** Create a floating 3D item showcase and spawn it immediately. */
  public static Hologram item(Location location, ItemStack item) {
    return get().createItem(location, item);
  }

  /** Create a floating 3D item showcase from Material and spawn it immediately. */
  public static Hologram item(Location location, Material material) {
    return get().createItem(location, new ItemStack(material));
  }

  /** Create a floating 3D block showcase and spawn it immediately. */
  public static Hologram block(Location location, Material material) {
    return get().createBlock(location, material);
  }

  /** Remove all active holograms spawned via this facade. */
  public static void removeAll() {
    get().removeAll();
  }

  /** Get all active holograms spawned via this facade. */
  public static Collection<Hologram> getAll() {
    return get().getAll();
  }
}
