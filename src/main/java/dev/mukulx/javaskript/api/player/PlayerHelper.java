package dev.mukulx.javaskript.api.player;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Universal, expressive Player utility for JavaSkript.
 *
 * <p>Streamlines player communication, audio-visual feedback, stats manipulation, inventory
 * handing, and spatial queries into clean 1-line operations.
 *
 * <p>Usable both via injection ({@code private PlayerHelper players;}) and statically ({@link
 * Players#msg(Player, String)}).
 */
public class PlayerHelper {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.builder().character('&').hexColors().build();

  private static final Set<String> NEGATIVE_POTION_EFFECT_NAMES =
      Set.of(
          "POISON",
          "WITHER",
          "SLOWNESS",
          "SLOW",
          "BLINDNESS",
          "WEAKNESS",
          "NAUSEA",
          "CONFUSION",
          "DARKNESS",
          "MINING_FATIGUE",
          "SLOW_DIGGING",
          "HUNGER",
          "LEVITATION",
          "UNLUCK",
          "BAD_OMEN");

  private static final Random RANDOM = new Random();
  private final JavaSkriptPlugin plugin;

  public PlayerHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  // ==========================================
  // Messaging & Formatting
  // ==========================================

  /**
   * Parse formatted text with support for MiniMessage tags and legacy ampersand codes. Default
   * italics are disabled.
   */
  public Component parse(String input) {
    if (input == null || input.isEmpty()) return Component.empty();
    Component comp;
    if (input.contains("<") && input.contains(">")) {
      try {
        comp = MINI_MESSAGE.deserialize(input);
      } catch (Exception ignored) {
        comp = LEGACY_AMPERSAND.deserialize(input.replace('§', '&'));
      }
    } else {
      comp = LEGACY_AMPERSAND.deserialize(input.replace('§', '&'));
    }

    if (!comp.hasDecoration(TextDecoration.ITALIC)) {
      comp = comp.decoration(TextDecoration.ITALIC, false);
    }
    return comp;
  }

  /** Send formatted message to a player or command sender. */
  public void msg(CommandSender sender, String message) {
    if (sender != null && message != null) {
      sender.sendMessage(parse(message));
    }
  }

  /** Send multiple formatted messages to a sender. */
  public void msg(CommandSender sender, String... messages) {
    if (sender != null && messages != null) {
      for (String m : messages) {
        msg(sender, m);
      }
    }
  }

  /** Broadcast a formatted message to all online players and console. */
  public void broadcast(String message) {
    if (message == null) return;
    Component comp = parse(message);
    Bukkit.broadcast(comp);
  }

  /** Broadcast a formatted message to all online players with a specific permission. */
  public void broadcast(String message, String permission) {
    if (message == null || permission == null) return;
    Component comp = parse(message);
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (p.hasPermission(permission)) {
        p.sendMessage(comp);
      }
    }
    Bukkit.getConsoleSender().sendMessage(comp);
  }

  // ==========================================
  // Audio & Sounds
  // ==========================================

  /** Play a sound to a player with default volume (1.0f) and pitch (1.0f). */
  public void sound(Player player, Sound sound) {
    sound(player, sound, 1.0f, 1.0f);
  }

  /** Play a sound to a player with specified volume and pitch. */
  public void sound(Player player, Sound sound, float volume, float pitch) {
    if (player != null && player.isOnline() && sound != null) {
      player.playSound(player.getLocation(), sound, volume, pitch);
    }
  }

  /** Play a sound at a world location to all nearby players. */
  public void sound(Location location, Sound sound, float volume, float pitch) {
    if (location != null && location.getWorld() != null && sound != null) {
      location.getWorld().playSound(location, sound, volume, pitch);
    }
  }

  // ==========================================
  // Titles & ActionBars
  // ==========================================

  /** Send title and subtitle with standard duration (0.5s fade in, 2.5s stay, 0.5s fade out). */
  public void title(Player player, String title, String subtitle) {
    title(player, title, subtitle, 10, 50, 10);
  }

  /** Send title and subtitle with precise tick timings. */
  public void title(
      Player player,
      String title,
      String subtitle,
      int fadeInTicks,
      int stayTicks,
      int fadeOutTicks) {
    if (player == null || !player.isOnline()) return;

    Component titleComp = parse(title);
    Component subComp = parse(subtitle);

    Title.Times times =
        Title.Times.times(
            Duration.ofMillis(fadeInTicks * 50L),
            Duration.ofMillis(stayTicks * 50L),
            Duration.ofMillis(fadeOutTicks * 50L));

    player.showTitle(Title.title(titleComp, subComp, times));
  }

  /** Send an action bar message to a player. */
  public void actionBar(Player player, String message) {
    if (player != null && player.isOnline() && message != null) {
      player.sendActionBar(parse(message));
    }
  }

  // ==========================================
  // Stats, Healing & Feeding
  // ==========================================

  /** Fully restore player health, extinguish fire, and clear negative potion effects. */
  public void heal(Player player) {
    if (player == null || !player.isOnline()) return;

    double maxHealth = 20.0;
    var attr = player.getAttribute(Attribute.MAX_HEALTH);
    if (attr != null) {
      maxHealth = attr.getValue();
    }
    player.setHealth(maxHealth);
    player.setFireTicks(0);

    // Remove negative potion effects
    for (var effect : player.getActivePotionEffects()) {
      if (effect != null && effect.getType() != null) {
        String typeName = effect.getType().getName();
        if (typeName != null && NEGATIVE_POTION_EFFECT_NAMES.contains(typeName)) {
          player.removePotionEffect(effect.getType());
        }
      }
    }
  }

  /** Fully restore player hunger and saturation. */
  public void feed(Player player) {
    if (player == null || !player.isOnline()) return;
    player.setFoodLevel(20);
    player.setSaturation(20.0f);
  }

  /** Clear a player's inventory, armor, and offhand slots. */
  public void clearInventory(Player player) {
    if (player != null && player.isOnline()) {
      player.getInventory().clear();
      player.getInventory().setArmorContents(null);
      player.getInventory().setItemInOffHand(null);
    }
  }

  /**
   * Safely give items to a player, dropping any items naturally on the ground if inventory is full.
   */
  public void give(Player player, ItemStack... items) {
    if (player == null || !player.isOnline() || items == null) return;
    for (ItemStack item : items) {
      if (item == null || item.getType().isAir()) continue;
      Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
      for (ItemStack drop : leftover.values()) {
        player.getWorld().dropItemNaturally(player.getLocation(), drop);
      }
    }
  }

  // ==========================================
  // World Interactions & Lightning
  // ==========================================

  /** Strike real lightning at player's location. */
  public void lightning(Player player) {
    if (player != null && player.isOnline()) {
      lightning(player.getLocation());
    }
  }

  /** Strike real lightning at a location. */
  public void lightning(Location location) {
    if (location != null && location.getWorld() != null) {
      location.getWorld().strikeLightning(location);
    }
  }

  /** Strike cosmetic visual lightning (no damage, no fire) at a player's location. */
  public void lightningEffect(Player player) {
    if (player != null && player.isOnline()) {
      lightningEffect(player.getLocation());
    }
  }

  /** Strike cosmetic visual lightning (no damage, no fire) at a location. */
  public void lightningEffect(Location location) {
    if (location != null && location.getWorld() != null) {
      location.getWorld().strikeLightningEffect(location);
    }
  }

  /** Teleport player safely (Folia-compatible async teleport). */
  public void teleport(Player player, Location location) {
    if (player == null || !player.isOnline() || location == null) return;
    try {
      player.teleportAsync(location);
    } catch (Throwable t) {
      player.teleport(location);
    }
  }

  /** Teleport player to another entity's location. */
  public void teleport(Player player, Entity target) {
    if (target != null) {
      teleport(player, target.getLocation());
    }
  }

  /** Teleport player to their world spawn. */
  public void teleportSpawn(Player player) {
    if (player != null && player.isOnline()) {
      teleport(player, player.getWorld().getSpawnLocation());
    }
  }

  // ==========================================
  // Queries & Lookups
  // ==========================================

  /** Check if a player has permission, or is OP. */
  public boolean hasPermission(Player player, String permission) {
    return player != null && (player.isOp() || player.hasPermission(permission));
  }

  /** Get all online players. */
  public Collection<? extends Player> all() {
    return Bukkit.getOnlinePlayers();
  }

  /** Pick a random online player. */
  public Optional<Player> random() {
    List<? extends Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
    if (online.isEmpty()) return Optional.empty();
    return Optional.of(online.get(RANDOM.nextInt(online.size())));
  }

  /** Find an online player by name or UUID string. */
  public Optional<Player> find(String nameOrUuid) {
    if (nameOrUuid == null || nameOrUuid.isBlank()) return Optional.empty();

    // 1. Try UUID
    try {
      UUID uuid = UUID.fromString(nameOrUuid.trim());
      Player p = Bukkit.getPlayer(uuid);
      if (p != null && p.isOnline()) return Optional.of(p);
    } catch (IllegalArgumentException ignored) {
    }

    // 2. Exact Name
    Player p = Bukkit.getPlayerExact(nameOrUuid.trim());
    if (p != null && p.isOnline()) return Optional.of(p);

    // 3. Match Name
    p = Bukkit.getPlayer(nameOrUuid.trim());
    if (p != null && p.isOnline()) return Optional.of(p);

    return Optional.empty();
  }

  /** Get all online players within a radius of a center location. */
  public List<Player> nearby(Location center, double radius) {
    if (center == null || center.getWorld() == null || radius <= 0) {
      return Collections.emptyList();
    }
    double radiusSq = radius * radius;
    List<Player> result = new ArrayList<>();
    for (Player p : center.getWorld().getPlayers()) {
      if (p != null && p.isOnline() && p.getWorld().equals(center.getWorld())) {
        if (p.getLocation().distanceSquared(center) <= radiusSq) {
          result.add(p);
        }
      }
    }
    return result;
  }
}
