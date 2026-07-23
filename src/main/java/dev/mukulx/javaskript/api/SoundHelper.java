package dev.mukulx.javaskript.api;

import java.util.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Easy sound API with full Sound support and convenient builders */
public class SoundHelper {

  /** Play a sound to a player (default volume and pitch) */
  public void play(Player player, org.bukkit.Sound sound) {
    play(player, sound, 1.0f, 1.0f);
  }

  /** Play a sound to a player with custom volume */
  public void play(Player player, org.bukkit.Sound sound, float volume) {
    play(player, sound, volume, 1.0f);
  }

  /** Play a sound to a player with custom volume and pitch */
  public void play(Player player, org.bukkit.Sound sound, float volume, float pitch) {
    player.playSound(player.getLocation(), sound, volume, pitch);
  }

  /** Play a sound by string key */
  public void play(Player player, String soundKey) {
    play(player, soundKey, 1.0f, 1.0f);
  }

  /** Play a sound by string key with volume and pitch */
  public void play(Player player, String soundKey, float volume, float pitch) {
    Sound sound = Sound.sound(Key.key(soundKey), Sound.Source.MASTER, volume, pitch);
    player.playSound(sound);
  }

  /** Play Adventure sound */
  public void play(Player player, Sound sound) {
    player.playSound(sound);
  }

  /** Play sound at a specific location */
  public void playAt(Location location, org.bukkit.Sound sound) {
    playAt(location, sound, 1.0f, 1.0f);
  }

  /** Play sound at location with volume and pitch */
  public void playAt(Location location, org.bukkit.Sound sound, float volume, float pitch) {
    if (location.getWorld() != null) {
      location.getWorld().playSound(location, sound, volume, pitch);
    }
  }

  /** Play sound by key at location */
  public void playAt(Location location, String soundKey, float volume, float pitch) {
    if (location.getWorld() != null) {
      Sound sound = Sound.sound(Key.key(soundKey), Sound.Source.MASTER, volume, pitch);
      location.getWorld().playSound(sound, location.x(), location.y(), location.z());
    }
  }

  /** Broadcast sound to all online players */
  public void broadcast(org.bukkit.Sound sound) {
    broadcast(sound, 1.0f, 1.0f);
  }

  /** Broadcast sound with volume and pitch */
  public void broadcast(org.bukkit.Sound sound, float volume, float pitch) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      play(player, sound, volume, pitch);
    }
  }

  /** Play to multiple players */
  public void playTo(Collection<? extends Player> players, org.bukkit.Sound sound) {
    playTo(players, sound, 1.0f, 1.0f);
  }

  /** Play to multiple players with volume and pitch */
  public void playTo(
      Collection<? extends Player> players, org.bukkit.Sound sound, float volume, float pitch) {
    for (Player player : players) {
      play(player, sound, volume, pitch);
    }
  }

  /** Play to players within radius */
  public void playInRadius(Location center, double radius, org.bukkit.Sound sound) {
    playInRadius(center, radius, sound, 1.0f, 1.0f);
  }

  /** Play to players within radius with volume and pitch */
  public void playInRadius(
      Location center, double radius, org.bukkit.Sound sound, float volume, float pitch) {
    if (center.getWorld() == null) return;

    double radiusSquared = radius * radius;
    for (Player player : center.getWorld().getPlayers()) {
      if (player.getLocation().distanceSquared(center) <= radiusSquared) {
        play(player, sound, volume, pitch);
      }
    }
  }

  /** Stop all sounds for a player */
  public void stopAll(Player player) {
    player.stopAllSounds();
  }

  /** Stop specific sound for a player */
  public void stop(Player player, org.bukkit.Sound sound) {
    player.stopSound(sound);
  }

  /** Stop sound by key */
  public void stop(Player player, String soundKey) {
    player.stopSound(soundKey);
  }

  /** Stop sounds by source */
  public void stopSource(Player player, Sound.Source source) {
    player.stopSound(net.kyori.adventure.sound.SoundStop.source(source));
  }

  /** Get a sound builder */
  public SoundBuilder builder() {
    return new SoundBuilder();
  }

  /** Sound builder for advanced usage */
  public class SoundBuilder {
    private org.bukkit.Sound sound;
    private String soundKey;
    private float volume = 1.0f;
    private float pitch = 1.0f;
    private Sound.Source source = Sound.Source.MASTER;
    private boolean useAdventure = false;

    public SoundBuilder sound(org.bukkit.Sound sound) {
      this.sound = sound;
      this.useAdventure = false;
      return this;
    }

    public SoundBuilder sound(String soundKey) {
      this.soundKey = soundKey;
      this.useAdventure = true;
      return this;
    }

    public SoundBuilder volume(float volume) {
      this.volume = volume;
      return this;
    }

    public SoundBuilder pitch(float pitch) {
      this.pitch = pitch;
      return this;
    }

    public SoundBuilder source(Sound.Source source) {
      this.source = source;
      return this;
    }

    public SoundBuilder loud() {
      this.volume = 2.0f;
      return this;
    }

    public SoundBuilder quiet() {
      this.volume = 0.3f;
      return this;
    }

    public SoundBuilder high() {
      this.pitch = 2.0f;
      return this;
    }

    public SoundBuilder low() {
      this.pitch = 0.5f;
      return this;
    }

    public void play(Player player) {
      if (useAdventure && soundKey != null) {
        Sound adventureSound = Sound.sound(Key.key(soundKey), source, volume, pitch);
        player.playSound(adventureSound);
      } else if (sound != null) {
        player.playSound(player.getLocation(), sound, volume, pitch);
      }
    }

    public void playAt(Location location) {
      if (useAdventure && soundKey != null) {
        Sound adventureSound = Sound.sound(Key.key(soundKey), source, volume, pitch);
        if (location.getWorld() != null) {
          location.getWorld().playSound(adventureSound, location.x(), location.y(), location.z());
        }
      } else if (sound != null && location.getWorld() != null) {
        location.getWorld().playSound(location, sound, volume, pitch);
      }
    }

    public void broadcast() {
      for (Player player : Bukkit.getOnlinePlayers()) {
        play(player);
      }
    }
  }

  /** Quick preset sounds */
  public static class Quick {

    public static void success(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }

    public static void error(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    public static void click(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    public static void notify(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    public static void warning(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    public static void teleport(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    public static void explosion(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
    }

    public static void pickup(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
    }

    public static void anvil(Player player) {
      player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
    }

    public static void firework(Player player) {
      player.playSound(
          player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
    }
  }

  /** Common sound categories */
  public static class Category {

    /** UI sounds */
    public static class UI {
      public static final org.bukkit.Sound CLICK = org.bukkit.Sound.UI_BUTTON_CLICK;
      public static final org.bukkit.Sound TOAST_IN = org.bukkit.Sound.UI_TOAST_IN;
      public static final org.bukkit.Sound TOAST_OUT = org.bukkit.Sound.UI_TOAST_OUT;
    }

    /** Entity sounds */
    public static class Entity {
      public static final org.bukkit.Sound PLAYER_HURT = org.bukkit.Sound.ENTITY_PLAYER_HURT;
      public static final org.bukkit.Sound PLAYER_DEATH = org.bukkit.Sound.ENTITY_PLAYER_DEATH;
      public static final org.bukkit.Sound LEVELUP = org.bukkit.Sound.ENTITY_PLAYER_LEVELUP;
      public static final org.bukkit.Sound VILLAGER_YES = org.bukkit.Sound.ENTITY_VILLAGER_YES;
      public static final org.bukkit.Sound VILLAGER_NO = org.bukkit.Sound.ENTITY_VILLAGER_NO;
      public static final org.bukkit.Sound ENDERMAN_TELEPORT =
          org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT;
      public static final org.bukkit.Sound EXPERIENCE_ORB =
          org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
      public static final org.bukkit.Sound ITEM_PICKUP = org.bukkit.Sound.ENTITY_ITEM_PICKUP;
    }

    /** Block sounds */
    public static class Block {
      public static final org.bukkit.Sound ANVIL_USE = org.bukkit.Sound.BLOCK_ANVIL_USE;
      public static final org.bukkit.Sound ANVIL_LAND = org.bukkit.Sound.BLOCK_ANVIL_LAND;
      public static final org.bukkit.Sound CHEST_OPEN = org.bukkit.Sound.BLOCK_CHEST_OPEN;
      public static final org.bukkit.Sound CHEST_CLOSE = org.bukkit.Sound.BLOCK_CHEST_CLOSE;
      public static final org.bukkit.Sound DOOR_OPEN = org.bukkit.Sound.BLOCK_WOODEN_DOOR_OPEN;
      public static final org.bukkit.Sound DOOR_CLOSE = org.bukkit.Sound.BLOCK_WOODEN_DOOR_CLOSE;
      public static final org.bukkit.Sound GLASS_BREAK = org.bukkit.Sound.BLOCK_GLASS_BREAK;
    }

    /** Note block sounds */
    public static class Music {
      public static final org.bukkit.Sound PLING = org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING;
      public static final org.bukkit.Sound BASS = org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS;
      public static final org.bukkit.Sound GUITAR = org.bukkit.Sound.BLOCK_NOTE_BLOCK_GUITAR;
      public static final org.bukkit.Sound BELL = org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL;
      public static final org.bukkit.Sound CHIME = org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME;
    }

    /** Ambient sounds */
    public static class Ambient {
      public static final org.bukkit.Sound THUNDER = org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER;
      public static final org.bukkit.Sound RAIN = org.bukkit.Sound.WEATHER_RAIN;
      public static final org.bukkit.Sound FIRE = org.bukkit.Sound.BLOCK_FIRE_AMBIENT;
      public static final org.bukkit.Sound WATER = org.bukkit.Sound.BLOCK_WATER_AMBIENT;
    }

    /** Combat sounds */
    public static class Combat {
      public static final org.bukkit.Sound ARROW_SHOOT = org.bukkit.Sound.ENTITY_ARROW_SHOOT;
      public static final org.bukkit.Sound ARROW_HIT = org.bukkit.Sound.ENTITY_ARROW_HIT;
      public static final org.bukkit.Sound SWORD_HIT = org.bukkit.Sound.ENTITY_PLAYER_ATTACK_STRONG;
      public static final org.bukkit.Sound EXPLOSION = org.bukkit.Sound.ENTITY_GENERIC_EXPLODE;
      public static final org.bukkit.Sound FIREWORK = org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST;
    }
  }

  /** Pitch presets for musical notes */
  public static class Pitch {
    public static final float F_SHARP_LOW = 0.5f;
    public static final float G = 0.53f;
    public static final float G_SHARP = 0.56f;
    public static final float A = 0.6f;
    public static final float A_SHARP = 0.63f;
    public static final float B = 0.67f;
    public static final float C = 0.7f;
    public static final float C_SHARP = 0.76f;
    public static final float D = 0.8f;
    public static final float D_SHARP = 0.84f;
    public static final float E = 0.9f;
    public static final float F = 0.94f;
    public static final float F_SHARP = 1.0f;
    public static final float G_HIGH = 1.06f;
    public static final float G_SHARP_HIGH = 1.12f;
    public static final float A_HIGH = 1.2f;
    public static final float A_SHARP_HIGH = 1.26f;
    public static final float B_HIGH = 1.34f;
    public static final float C_HIGH = 1.4f;
    public static final float C_SHARP_HIGH = 1.5f;
    public static final float D_HIGH = 1.6f;
    public static final float D_SHARP_HIGH = 1.68f;
    public static final float E_HIGH = 1.8f;
    public static final float F_HIGH = 1.88f;
    public static final float F_SHARP_HIGH = 2.0f;
  }

  /** Volume presets */
  public static class Volume {
    public static final float SILENT = 0.0f;
    public static final float VERY_QUIET = 0.2f;
    public static final float QUIET = 0.5f;
    public static final float NORMAL = 1.0f;
    public static final float LOUD = 1.5f;
    public static final float VERY_LOUD = 2.0f;
  }
}
