package dev.mukulx.javaskript.api;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Static shorthand facade for playing sounds, audio fanfares, and feedback chords.
 *
 * <p>Enables 1-line sounds anywhere without injecting {@link SoundHelper}:
 *
 * <pre>{@code
 * Sounds.success(player);
 * Sounds.fail(player);
 * Sounds.play(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP);
 * }</pre>
 */
public final class Sounds {

  private static final SoundHelper HELPER = new SoundHelper();

  private Sounds() {}

  /** Play a Bukkit sound to a player. */
  public static void play(Player player, org.bukkit.Sound sound) {
    HELPER.play(player, sound);
  }

  /** Play a Bukkit sound with custom volume and pitch. */
  public static void play(Player player, org.bukkit.Sound sound, float volume, float pitch) {
    HELPER.play(player, sound, volume, pitch);
  }

  /** Play an Adventure sound to a player. */
  public static void play(Player player, Sound sound) {
    HELPER.play(player, sound);
  }

  /** Play a Bukkit sound at a location. */
  public static void play(Location location, org.bukkit.Sound sound) {
    HELPER.playAt(location, sound);
  }

  /** Play a Bukkit sound at a location with volume and pitch. */
  public static void play(Location location, org.bukkit.Sound sound, float volume, float pitch) {
    HELPER.playAt(location, sound, volume, pitch);
  }

  /** Play a sound by string key (e.g. "entity.player.levelup"). */
  public static void play(Player player, String soundKey) {
    HELPER.play(player, soundKey);
  }

  /** Play a sound by string key with volume and pitch. */
  public static void play(Player player, String soundKey, float volume, float pitch) {
    HELPER.play(player, soundKey, volume, pitch);
  }

  /** High-pitch cheerful chime for successful actions. */
  public static void success(Player player) {
    if (player != null) {
      player.playSound(
          player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
    }
  }

  /** Low-pitch bass note for errors, failed actions, or denied requests. */
  public static void fail(Player player) {
    if (player != null) {
      player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }
  }

  /** Subtle UI click for buttons and menus. */
  public static void click(Player player) {
    if (player != null) {
      player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }
  }

  /** Grand fanfare for level ups, quest completions, or achievements. */
  public static void levelup(Player player) {
    if (player != null) {
      player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
  }

  /** Bell chime for alerts and notifications. */
  public static void chime(Player player) {
    if (player != null) {
      player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
    }
  }
}
