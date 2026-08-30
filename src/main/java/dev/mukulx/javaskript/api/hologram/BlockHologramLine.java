package dev.mukulx.javaskript.api.hologram;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/** A floating 3D block line using modern Paper {@link BlockDisplay}. */
public class BlockHologramLine implements HologramLine {

  private BlockData blockData;
  private Display.Billboard billboard = Display.Billboard.FIXED;
  private Vector3f scale = new Vector3f(0.5f, 0.5f, 0.5f);
  private Vector3f translation = new Vector3f(-0.25f, 0, -0.25f);
  private boolean glowing = false;
  private Color glowColor = null;
  private float viewRange = 1.0f;
  private BlockDisplay entity;

  public BlockHologramLine(Material material) {
    this.blockData = material.createBlockData();
  }

  public BlockHologramLine(BlockData blockData) {
    this.blockData = blockData;
  }

  @Override
  public BlockDisplay getEntity() {
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
                BlockDisplay.class,
                display -> {
                  applyProperties(display);
                });
  }

  private void applyProperties(BlockDisplay display) {
    if (blockData != null) {
      display.setBlock(blockData);
    }
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
    if (entity != null && entity.isValid() && blockData != null) {
      entity.setBlock(blockData);
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

  public BlockHologramLine block(BlockData blockData) {
    this.blockData = blockData;
    update();
    return this;
  }

  public BlockHologramLine block(Material material) {
    return block(material.createBlockData());
  }

  public BlockHologramLine billboard(Display.Billboard billboard) {
    this.billboard = billboard;
    if (entity != null && entity.isValid()) entity.setBillboard(billboard);
    return this;
  }

  public BlockHologramLine scale(double scale) {
    float s = (float) scale;
    this.scale = new Vector3f(s, s, s);
    this.translation = new Vector3f(-s / 2f, 0, -s / 2f);
    if (entity != null && entity.isValid()) {
      entity.setTransformation(
          new Transformation(
              translation, new AxisAngle4f(0, 0, 0, 1), this.scale, new AxisAngle4f(0, 0, 0, 1)));
    }
    return this;
  }

  public BlockHologramLine glowing(boolean glowing) {
    this.glowing = glowing;
    if (entity != null && entity.isValid()) entity.setGlowing(glowing);
    return this;
  }

  public BlockHologramLine glowColor(Color glowColor) {
    this.glowColor = glowColor;
    if (entity != null && entity.isValid()) entity.setGlowColorOverride(glowColor);
    return this;
  }

  public BlockHologramLine viewRange(float viewRange) {
    this.viewRange = viewRange;
    if (entity != null && entity.isValid()) entity.setViewRange(viewRange);
    return this;
  }

  public BlockData getBlockData() {
    return blockData;
  }
}
