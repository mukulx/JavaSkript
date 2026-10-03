package dev.mukulx.javaskript.api.mannequin;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.MannequinHelper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Pose;

/**
 * Static shorthand facade for Paper Mannequins.
 *
 * <p>Enables 1-line creation of player-model mannequins, NPCs, and statues anywhere:
 *
 * <pre>{@code
 * Mannequins.spawn(location, "<gold>Welcome NPC</gold>", "Mukulx");
 *
 * Mannequins.create(location)
 *     .name("Guard")
 *     .skin("Steve")
 *     .helmet(Material.IRON_HELMET)
 *     .mainHandItem(Material.IRON_SWORD)
 *     .onClick((player, m) -> player.sendMessage("Halt!"))
 *     .spawn();
 * }</pre>
 */
public final class Mannequins {

  private static MannequinHelper helper;

  private Mannequins() {}

  private static MannequinHelper get() {
    if (helper == null) {
      helper = new MannequinHelper(JavaSkriptPlugin.getInstance(), "global");
    }
    return helper;
  }

  public static void setInstance(MannequinHelper h) {
    helper = h;
  }

  /**
   * Create a new CustomMannequin builder at the specified location.
   */
  public static CustomMannequin create(Location location) {
    return get().create(location);
  }

  /**
   * Create a new CustomMannequin builder with a custom name.
   */
  public static CustomMannequin create(Location location, String name) {
    return get().create(location, name);
  }

  /**
   * Create a new CustomMannequin builder with a Component name.
   */
  public static CustomMannequin create(Location location, Component name) {
    return get().create(location, name);
  }

  /**
   * Create and immediately spawn a mannequin with a name and player skin.
   */
  public static CustomMannequin spawn(Location location, String name, String skinPlayerName) {
    return get().spawn(location, name, skinPlayerName);
  }

  /**
   * Create and immediately spawn a mannequin with a Component name and player skin.
   */
  public static CustomMannequin spawn(Location location, Component name, String skinPlayerName) {
    return get().spawn(location, name, skinPlayerName);
  }

  /**
   * Wrap an existing Bukkit/Paper Mannequin entity into a CustomMannequin controller.
   */
  public static CustomMannequin wrap(Mannequin entity) {
    return get().wrap(entity);
  }

  /**
   * Remove and despawn a specific mannequin.
   */
  public static void remove(CustomMannequin mannequin) {
    get().remove(mannequin);
  }

  /**
   * Despawn all mannequins tracked by the global facade.
   */
  public static void removeAll() {
    get().removeAll();
  }

  /**
   * Get all active mannequins.
   */
  public static Collection<CustomMannequin> getAll() {
    return get().getAll();
  }

  /**
   * Find mannequins near a given location.
   */
  public static List<CustomMannequin> getNearby(Location location, double radius) {
    return get().getNearby(location, radius);
  }

  /**
   * Find tracked mannequins with a specific scoreboard tag.
   */
  public static List<CustomMannequin> findByTag(String tag) {
    return get().findByTag(tag);
  }

  /**
   * Find a tracked mannequin by its entity UUID.
   */
  public static Optional<CustomMannequin> findById(UUID uuid) {
    return get().findById(uuid);
  }

  /**
   * Valid poses supported by Mannequins in Paper 1.21.11+.
   */
  public static Set<Pose> validPoses() {
    return MannequinHelper.validPoses();
  }

  /**
   * Default mannequin profile.
   */
  public static io.papermc.paper.datacomponent.item.ResolvableProfile defaultProfile() {
    return MannequinHelper.defaultProfile();
  }

  /**
   * Default mannequin description component.
   */
  public static Component defaultDescription() {
    return MannequinHelper.defaultDescription();
  }
}
