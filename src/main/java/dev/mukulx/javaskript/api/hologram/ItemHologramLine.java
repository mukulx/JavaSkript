package dev.mukulx.javaskript.api.hologram;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/** A floating 3D item line using modern Paper {@link ItemDisplay}. */
public class ItemHologramLine implements HologramLine {

  private ItemStack item;
  private ItemDisplay.ItemDisplayTransform transform = ItemDisplay.ItemDisplayTransform.GROUND;
  private Display.Billboard billboard = Display.Billboard.CENTER;
  private Vector3f scale = new Vector3f(0.6f, 0.6f, 0.6f);
  private Vector3f translation = new Vector3f(0, 0, 0);
  private boolean glowing = false;
  private Color glowColor = null;
  private float viewRange = 1.0f;
  private ItemDisplay entity;

  public ItemHologramLine(ItemStack item) {
    this.item = item;
  }

  public ItemHologramLine(ItemStack item, double scale) {
    this.item = item;
    float s = (float) scale;
    this.scale = new Vector3f(s, s, s);
  }

  @Override
  public ItemDisplay getEntity() {
    return entity;
  }

  @Override
  public void spawn(Location location) {
    if (location == null || location.getWorld() == null) {
      return;
    }
    despawn();

    this.entity =
        location
            .getWorld()
            .spawn(
                location,
                ItemDisplay.class,
                display -> {
                  applyProperties(display);
                });
  }

  private void applyProperties(ItemDisplay display) {
    if (item != null) {
      display.setItemStack(item);
    }
    display.setItemDisplayTransform(transform);
    display.setBillboard(billboard);
    display.setTransformation(
        new Transformation(
            translation, new AxisAngle4f(0, 0, 0, 1), scale, new AxisAngle4f(0, 0, 0, 1)));
    display.setGlowing(glowing);
    if (glowColor != null) {
      display.setGlowColorOverride(glowColor);
    }
    display.setViewRange(viewRange);
    display.setPersistent(false);
  }

  @Override
  public void despawn() {
    if (entity != null && entity.isValid()) {
      entity.remove();
      entity = null;
    }
  }

  @Override
  public void teleport(Location location) {
    if (entity != null && entity.isValid()) {
      entity.teleport(location);
    }
  }

  @Override
  public void update() {
    if (entity != null && entity.isValid() && item != null) {
      entity.setItemStack(item);
    }
  }

  @Override
  public double getHeight() {
    return 0.5 * (scale != null ? scale.y : 1.0);
  }

  @Override
  public boolean isSpawned() {
    return entity != null && entity.isValid();
  }

  // Fluent Setters

  public ItemHologramLine item(ItemStack item) {
    this.item = item;
    update();
    return this;
  }

  public ItemHologramLine transform(ItemDisplay.ItemDisplayTransform transform) {
    this.transform = transform;
    if (entity != null && entity.isValid()) entity.setItemDisplayTransform(transform);
    return this;
  }

  public ItemHologramLine billboard(Display.Billboard billboard) {
    this.billboard = billboard;
    if (entity != null && entity.isValid()) entity.setBillboard(billboard);
    return this;
  }

  public ItemHologramLine scale(double scale) {
    float s = (float) scale;
    return scale(new Vector3f(s, s, s));
  }

  public ItemHologramLine scale(Vector3f scale) {
    this.scale = scale;
    if (entity != null && entity.isValid()) {
      entity.setTransformation(
          new Transformation(
              translation, new AxisAngle4f(0, 0, 0, 1), scale, new AxisAngle4f(0, 0, 0, 1)));
    }
    return this;
  }

  public ItemHologramLine glowing(boolean glowing) {
    this.glowing = glowing;
    if (entity != null && entity.isValid()) entity.setGlowing(glowing);
    return this;
  }

  public ItemHologramLine glowColor(Color glowColor) {
    this.glowColor = glowColor;
    if (entity != null && entity.isValid()) entity.setGlowColorOverride(glowColor);
    return this;
  }

  public ItemHologramLine viewRange(float viewRange) {
    this.viewRange = viewRange;
    if (entity != null && entity.isValid()) entity.setViewRange(viewRange);
    return this;
  }

  public ItemStack getItem() {
    return item;
  }
}
