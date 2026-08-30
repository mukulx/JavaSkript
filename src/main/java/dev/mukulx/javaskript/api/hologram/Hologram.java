package dev.mukulx.javaskript.api.hologram;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * Modern, high-performance Hologram built on native Minecraft Display Entities.
 *
 * <p>Supports zero-lag Text Displays, 3D Item Displays, and Block Displays with full customization
 * (billboarding, scaling, transparency, glowing, shadows, dynamic text).
 */
public class Hologram {

  private final JavaSkriptPlugin plugin;
  private Location location;
  private double lineSpacing = 0.28;
  private final List<HologramLine> lines = new ArrayList<>();
  private boolean spawned = false;

  // Defaults applied to new lines
  private Display.Billboard defaultBillboard = Display.Billboard.CENTER;
  private Color defaultBackgroundColor = Color.fromARGB(0, 0, 0, 0);
  private boolean defaultBackground = false;
  private boolean defaultShadow = true;
  private boolean defaultSeeThrough = false;
  private double defaultScale = 1.0;
  private boolean defaultGlowing = false;
  private Color defaultGlowColor = null;
  private float defaultViewRange = 1.0f;

  // Auto update task
  private BukkitTask bukkitTask = null;
  private Object foliaTask = null;

  public Hologram(JavaSkriptPlugin plugin, Location location) {
    this.plugin = plugin;
    this.location = location != null ? location.clone() : null;
  }

  // ==========================================
  // Line Adders & Setters
  // ==========================================

  public Hologram line(String text) {
    TextHologramLine line = new TextHologramLine(text);
    applyDefaults(line);
    lines.add(line);
    if (spawned) respawn();
    return this;
  }

  public Hologram line(Component text) {
    TextHologramLine line = new TextHologramLine(text);
    applyDefaults(line);
    lines.add(line);
    if (spawned) respawn();
    return this;
  }

  public Hologram line(Supplier<String> dynamicText) {
    TextHologramLine line = new TextHologramLine(dynamicText);
    applyDefaults(line);
    lines.add(line);
    if (spawned) respawn();
    return this;
  }

  public Hologram line(int index, String text) {
    return setLine(index, text);
  }

  public Hologram line(int index, Component text) {
    return setLine(index, text);
  }

  public Hologram item(ItemStack item) {
    ItemHologramLine line = new ItemHologramLine(item);
    line.billboard(defaultBillboard);
    line.scale(defaultScale * 0.6);
    line.glowing(defaultGlowing);
    line.glowColor(defaultGlowColor);
    line.viewRange(defaultViewRange);
    lines.add(line);
    if (spawned) respawn();
    return this;
  }

  public Hologram item(ItemStack item, double scale) {
    ItemHologramLine line = new ItemHologramLine(item, scale);
    line.billboard(defaultBillboard);
    line.glowing(defaultGlowing);
    line.glowColor(defaultGlowColor);
    line.viewRange(defaultViewRange);
    lines.add(line);
    if (spawned) respawn();
    return this;
  }

  public Hologram item(ItemStack item, ItemDisplay.ItemDisplayTransform transform) {
    ItemHologramLine line = new ItemHologramLine(item);
    line.transform(transform);
    line.billboard(defaultBillboard);
    line.scale(defaultScale * 0.6);
    line.glowing(defaultGlowing);
    line.glowColor(defaultGlowColor);
    line.viewRange(defaultViewRange);
    lines.add(line);
    if (spawned) respawn();
    return this;
  }

  public Hologram block(Material material) {
    BlockHologramLine line = new BlockHologramLine(material);
    line.billboard(defaultBillboard);
    line.scale(defaultScale * 0.5);
    line.glowing(defaultGlowing);
    line.glowColor(defaultGlowColor);
    line.viewRange(defaultViewRange);
    lines.add(line);
    if (spawned) respawn();
    return this;
  }

  public Hologram block(BlockData blockData) {
    BlockHologramLine line = new BlockHologramLine(blockData);
    line.billboard(defaultBillboard);
    line.scale(defaultScale * 0.5);
    line.glowing(defaultGlowing);
    line.glowColor(defaultGlowColor);
    line.viewRange(defaultViewRange);
    lines.add(line);
    if (spawned) respawn();
    return this;
  }

  public Hologram addLine(HologramLine line) {
    if (line != null) {
      lines.add(line);
      if (spawned) respawn();
    }
    return this;
  }

  public Hologram setLine(int index, String text) {
    if (index >= 0 && index < lines.size()) {
      HologramLine current = lines.get(index);
      if (current instanceof TextHologramLine textLine) {
        textLine.text(text);
      } else {
        TextHologramLine newLine = new TextHologramLine(text);
        applyDefaults(newLine);
        lines.set(index, newLine);
        if (spawned) respawn();
      }
    }
    return this;
  }

  public Hologram setLine(int index, Component text) {
    if (index >= 0 && index < lines.size()) {
      HologramLine current = lines.get(index);
      if (current instanceof TextHologramLine textLine) {
        textLine.text(text);
      } else {
        TextHologramLine newLine = new TextHologramLine(text);
        applyDefaults(newLine);
        lines.set(index, newLine);
        if (spawned) respawn();
      }
    }
    return this;
  }

  public Hologram setLine(int index, ItemStack item) {
    if (index >= 0 && index < lines.size()) {
      HologramLine current = lines.get(index);
      if (current instanceof ItemHologramLine itemLine) {
        itemLine.item(item);
      } else {
        ItemHologramLine newLine = new ItemHologramLine(item);
        lines.set(index, newLine);
        if (spawned) respawn();
      }
    }
    return this;
  }

  public Hologram removeLine(int index) {
    if (index >= 0 && index < lines.size()) {
      HologramLine removed = lines.remove(index);
      removed.despawn();
      if (spawned) reposition();
    }
    return this;
  }

  public Hologram clearLines() {
    despawn();
    lines.clear();
    return this;
  }

  // ==========================================
  // Customization & Properties
  // ==========================================

  public Hologram lineSpacing(double spacing) {
    this.lineSpacing = spacing;
    if (spawned) reposition();
    return this;
  }

  public Hologram billboard(Display.Billboard billboard) {
    this.defaultBillboard = billboard;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.billboard(billboard);
      else if (line instanceof ItemHologramLine il) il.billboard(billboard);
      else if (line instanceof BlockHologramLine bl) bl.billboard(billboard);
    }
    return this;
  }

  public Hologram backgroundColor(Color color) {
    this.defaultBackgroundColor = color;
    this.defaultBackground = false;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.backgroundColor(color);
    }
    return this;
  }

  public Hologram transparentBackground() {
    return backgroundColor(Color.fromARGB(0, 0, 0, 0));
  }

  public Hologram defaultBackground(boolean defaultBackground) {
    this.defaultBackground = defaultBackground;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.defaultBackground(defaultBackground);
    }
    return this;
  }

  public Hologram shadow(boolean shadow) {
    this.defaultShadow = shadow;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.shadow(shadow);
    }
    return this;
  }

  public Hologram seeThrough(boolean seeThrough) {
    this.defaultSeeThrough = seeThrough;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.seeThrough(seeThrough);
    }
    return this;
  }

  public Hologram scale(double scale) {
    this.defaultScale = scale;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.scale(scale);
      else if (line instanceof ItemHologramLine il) il.scale(scale * 0.6);
      else if (line instanceof BlockHologramLine bl) bl.scale(scale * 0.5);
    }
    if (spawned) reposition();
    return this;
  }

  public Hologram glowing(boolean glowing) {
    this.defaultGlowing = glowing;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.glowing(glowing);
      else if (line instanceof ItemHologramLine il) il.glowing(glowing);
      else if (line instanceof BlockHologramLine bl) bl.glowing(glowing);
    }
    return this;
  }

  public Hologram glowColor(Color glowColor) {
    this.defaultGlowColor = glowColor;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.glowColor(glowColor);
      else if (line instanceof ItemHologramLine il) il.glowColor(glowColor);
      else if (line instanceof BlockHologramLine bl) bl.glowColor(glowColor);
    }
    return this;
  }

  public Hologram viewRange(float viewRange) {
    this.defaultViewRange = viewRange;
    for (HologramLine line : lines) {
      if (line instanceof TextHologramLine tl) tl.viewRange(viewRange);
      else if (line instanceof ItemHologramLine il) il.viewRange(viewRange);
      else if (line instanceof BlockHologramLine bl) bl.viewRange(viewRange);
    }
    return this;
  }

  /** Auto-update dynamic text lines every {@code ticks}. */
  public Hologram updateInterval(long ticks) {
    stopUpdateTask();
    if (ticks > 0) {
      if (ServerUtil.isFolia()) {
        try {
          this.foliaTask =
              Bukkit.getGlobalRegionScheduler()
                  .runAtFixedRate(
                      plugin,
                      task -> {
                        update();
                      },
                      ticks,
                      ticks);
        } catch (Exception e) {
          plugin.debug("Could not schedule Folia hologram update task: " + e.getMessage());
        }
      } else {
        this.bukkitTask =
            Bukkit.getScheduler()
                .runTaskTimer(
                    plugin,
                    () -> {
                      update();
                    },
                    ticks,
                    ticks);
      }
    }
    return this;
  }

  // ==========================================
  // Lifecycle & Spawning
  // ==========================================

  public Hologram spawn() {
    if (location == null || location.getWorld() == null) {
      return this;
    }
    despawn();
    this.spawned = true;

    double currentY = location.getY();
    for (HologramLine line : lines) {
      Location lineLoc = location.clone();
      lineLoc.setY(currentY);
      line.spawn(lineLoc);
      currentY -= (line.getHeight() + lineSpacing);
    }
    return this;
  }

  public void respawn() {
    if (spawned) {
      spawn();
    }
  }

  public void despawn() {
    this.spawned = false;
    for (HologramLine line : lines) {
      line.despawn();
    }
  }

  public void remove() {
    stopUpdateTask();
    despawn();
    lines.clear();
  }

  public void delete() {
    remove();
  }

  public void teleport(Location newLocation) {
    if (newLocation == null) return;
    this.location = newLocation.clone();
    reposition();
  }

  public void update() {
    for (HologramLine line : lines) {
      line.update();
    }
  }

  private void reposition() {
    if (!spawned || location == null) return;
    double currentY = location.getY();
    for (HologramLine line : lines) {
      Location lineLoc = location.clone();
      lineLoc.setY(currentY);
      if (line.isSpawned()) {
        line.teleport(lineLoc);
      } else {
        line.spawn(lineLoc);
      }
      currentY -= (line.getHeight() + lineSpacing);
    }
  }

  private void applyDefaults(TextHologramLine line) {
    line.billboard(defaultBillboard);
    line.defaultBackground(defaultBackground);
    if (!defaultBackground && defaultBackgroundColor != null) {
      line.backgroundColor(defaultBackgroundColor);
    }
    line.shadow(defaultShadow);
    line.seeThrough(defaultSeeThrough);
    line.scale(defaultScale);
    line.glowing(defaultGlowing);
    line.glowColor(defaultGlowColor);
    line.viewRange(defaultViewRange);
  }

  private void stopUpdateTask() {
    if (bukkitTask != null) {
      bukkitTask.cancel();
      bukkitTask = null;
    }
    if (foliaTask != null) {
      try {
        ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) foliaTask).cancel();
      } catch (Exception ignored) {
      }
      foliaTask = null;
    }
  }

  // ==========================================
  // Getters
  // ==========================================

  public boolean isSpawned() {
    return spawned;
  }

  public Location getLocation() {
    return location != null ? location.clone() : null;
  }

  public int getLinesCount() {
    return lines.size();
  }

  public List<HologramLine> getLines() {
    return Collections.unmodifiableList(lines);
  }

  public List<Display> getEntities() {
    List<Display> entities = new ArrayList<>();
    for (HologramLine line : lines) {
      if (line.getEntity() != null && line.getEntity().isValid()) {
        entities.add(line.getEntity());
      }
    }
    return entities;
  }
}
