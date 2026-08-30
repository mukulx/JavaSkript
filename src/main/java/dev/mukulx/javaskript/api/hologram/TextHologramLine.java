package dev.mukulx.javaskript.api.hologram;

import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/** A floating text line using modern Paper {@link TextDisplay}. */
public class TextHologramLine implements HologramLine {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  private Component text;
  private Supplier<Component> dynamicSupplier;
  private Display.Billboard billboard = Display.Billboard.CENTER;
  private Color backgroundColor = Color.fromARGB(0, 0, 0, 0); // Transparent by default
  private boolean defaultBackground = false;
  private boolean shadow = true;
  private boolean seeThrough = false;
  private TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.CENTER;
  private int lineWidth = 200;
  private byte textOpacity = (byte) -1;
  private Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
  private boolean glowing = false;
  private Color glowColor = null;
  private float viewRange = 1.0f;
  private TextDisplay entity;

  public TextHologramLine(Component text) {
    this.text = text != null ? text : Component.empty();
  }

  public TextHologramLine(String text) {
    this.text = parseText(text);
  }

  public TextHologramLine(Supplier<String> stringSupplier) {
    this.dynamicSupplier = () -> parseText(stringSupplier.get());
  }

  public static Component parseText(String text) {
    if (text == null || text.isEmpty()) {
      return Component.empty();
    }
    if (text.contains("<") && text.contains(">")) {
      try {
        return MINI_MESSAGE.deserialize(text);
      } catch (Exception ignored) {
      }
    }
    return Component.text(text.replace("&", "§"));
  }

  @Override
  public TextDisplay getEntity() {
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
                TextDisplay.class,
                display -> {
                  applyProperties(display);
                });
  }

  private void applyProperties(TextDisplay display) {
    Component current = (dynamicSupplier != null) ? dynamicSupplier.get() : text;
    display.text(current);
    display.setBillboard(billboard);
    display.setDefaultBackground(defaultBackground);
    if (!defaultBackground && backgroundColor != null) {
      display.setBackgroundColor(backgroundColor);
    }
    display.setShadowed(shadow);
    display.setSeeThrough(seeThrough);
    display.setAlignment(alignment);
    display.setLineWidth(lineWidth);
    if (textOpacity != (byte) -1) {
      display.setTextOpacity(textOpacity);
    }
    if (scale != null && (scale.x != 1.0f || scale.y != 1.0f || scale.z != 1.0f)) {
      display.setTransformation(
          new Transformation(
              new Vector3f(0, 0, 0),
              new AxisAngle4f(0, 0, 0, 1),
              scale,
              new AxisAngle4f(0, 0, 0, 1)));
    }
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
    if (entity != null && entity.isValid()) {
      Component current = (dynamicSupplier != null) ? dynamicSupplier.get() : text;
      entity.text(current);
    }
  }

  @Override
  public double getHeight() {
    return 0.28 * (scale != null ? scale.y : 1.0);
  }

  @Override
  public boolean isSpawned() {
    return entity != null && entity.isValid();
  }

  // Fluent Setters

  public TextHologramLine text(Component text) {
    this.text = text != null ? text : Component.empty();
    this.dynamicSupplier = null;
    update();
    return this;
  }

  public TextHologramLine text(String text) {
    return text(parseText(text));
  }

  public TextHologramLine text(Supplier<String> supplier) {
    this.dynamicSupplier = () -> parseText(supplier.get());
    update();
    return this;
  }

  public TextHologramLine billboard(Display.Billboard billboard) {
    this.billboard = billboard;
    if (entity != null && entity.isValid()) entity.setBillboard(billboard);
    return this;
  }

  public TextHologramLine backgroundColor(Color color) {
    this.backgroundColor = color;
    this.defaultBackground = false;
    if (entity != null && entity.isValid()) {
      entity.setDefaultBackground(false);
      entity.setBackgroundColor(color);
    }
    return this;
  }

  public TextHologramLine defaultBackground(boolean defaultBackground) {
    this.defaultBackground = defaultBackground;
    if (entity != null && entity.isValid()) entity.setDefaultBackground(defaultBackground);
    return this;
  }

  public TextHologramLine transparentBackground() {
    return backgroundColor(Color.fromARGB(0, 0, 0, 0));
  }

  public TextHologramLine shadow(boolean shadow) {
    this.shadow = shadow;
    if (entity != null && entity.isValid()) entity.setShadowed(shadow);
    return this;
  }

  public TextHologramLine seeThrough(boolean seeThrough) {
    this.seeThrough = seeThrough;
    if (entity != null && entity.isValid()) entity.setSeeThrough(seeThrough);
    return this;
  }

  public TextHologramLine alignment(TextDisplay.TextAlignment alignment) {
    this.alignment = alignment;
    if (entity != null && entity.isValid()) entity.setAlignment(alignment);
    return this;
  }

  public TextHologramLine lineWidth(int lineWidth) {
    this.lineWidth = lineWidth;
    if (entity != null && entity.isValid()) entity.setLineWidth(lineWidth);
    return this;
  }

  public TextHologramLine textOpacity(byte textOpacity) {
    this.textOpacity = textOpacity;
    if (entity != null && entity.isValid()) entity.setTextOpacity(textOpacity);
    return this;
  }

  public TextHologramLine scale(double scale) {
    float s = (float) scale;
    return scale(new Vector3f(s, s, s));
  }

  public TextHologramLine scale(Vector3f scale) {
    this.scale = scale;
    if (entity != null && entity.isValid()) {
      entity.setTransformation(
          new Transformation(
              new Vector3f(0, 0, 0),
              new AxisAngle4f(0, 0, 0, 1),
              scale,
              new AxisAngle4f(0, 0, 0, 1)));
    }
    return this;
  }

  public TextHologramLine glowing(boolean glowing) {
    this.glowing = glowing;
    if (entity != null && entity.isValid()) entity.setGlowing(glowing);
    return this;
  }

  public TextHologramLine glowColor(Color glowColor) {
    this.glowColor = glowColor;
    if (entity != null && entity.isValid()) entity.setGlowColorOverride(glowColor);
    return this;
  }

  public TextHologramLine viewRange(float viewRange) {
    this.viewRange = viewRange;
    if (entity != null && entity.isValid()) entity.setViewRange(viewRange);
    return this;
  }

  public Component getText() {
    return text;
  }
}
