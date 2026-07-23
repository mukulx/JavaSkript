import dev.mukulx.javaskript.api.ScriptScheduler;
import dev.mukulx.javaskript.api.SoundHelper;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * NBS (Note Block Studio) file player - fully independent implementation Drop .nbs files into
 * plugins/JavaSkript/songs/ folder Supports NBS v0-5 format
 */
public class NBSPlayer implements CommandExecutor, TabCompleter {

  private SoundHelper sound;
  private ScriptScheduler scheduler;

  private final Path songsFolder = Paths.get("plugins/JavaSkript/songs");
  private final Map<String, NBSSong> loadedSongs = new HashMap<>();
  private final Map<UUID, SongPlayback> activePlayers = new ConcurrentHashMap<>();

  public void onEnable() {
    try {
      Files.createDirectories(songsFolder);
      loadAllSongs();
      log("§aNBS Player loaded! Drop .nbs files in: " + songsFolder);
    } catch (IOException e) {
      log("§cFailed to create songs folder: " + e.getMessage());
    }
  }

  private void loadAllSongs() {
    loadedSongs.clear();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(songsFolder, "*.nbs")) {
      for (Path file : stream) {
        try {
          NBSSong song = parseNBS(file);
          loadedSongs.put(song.name.toLowerCase(), song);
          log(
              "§7Loaded: §f"
                  + song.name
                  + " §7("
                  + song.length
                  + " ticks, "
                  + song.layers.size()
                  + " layers)");
        } catch (Exception e) {
          log("§cFailed to load " + file.getFileName() + ": " + e.getMessage());
        }
      }
      log("§aLoaded " + loadedSongs.size() + " songs");
    } catch (IOException e) {
      log("§cFailed to load songs: " + e.getMessage());
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("§cOnly players can use this command");
      return true;
    }

    if (args.length == 0) {
      player.sendMessage("§e§lNBS Player Commands:");
      player.sendMessage("§7/nbs play <song> §f- Play a song");
      player.sendMessage("§7/nbs stop §f- Stop current song");
      player.sendMessage("§7/nbs list §f- List available songs");
      player.sendMessage("§7/nbs reload §f- Reload songs folder");
      player.sendMessage("§7/nbs info <song> §f- Show song info");
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "play" -> {
        if (args.length < 2) {
          player.sendMessage("§cUsage: /nbs play <song>");
          return true;
        }
        String songName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        playSong(player, songName);
      }

      case "stop" -> {
        stopSong(player);
      }

      case "list" -> {
        if (loadedSongs.isEmpty()) {
          player.sendMessage("§cNo songs loaded. Drop .nbs files in: " + songsFolder);
        } else {
          player.sendMessage("§e§lAvailable Songs: §7(" + loadedSongs.size() + ")");
          loadedSongs
              .values()
              .forEach(
                  song -> {
                    player.sendMessage(
                        "§7• §f" + song.name + " §7- " + formatTime(song.length / song.tempo));
                  });
        }
      }

      case "reload" -> {
        loadAllSongs();
        player.sendMessage("§aReloaded " + loadedSongs.size() + " songs");
      }

      case "info" -> {
        if (args.length < 2) {
          player.sendMessage("§cUsage: /nbs info <song>");
          return true;
        }
        String songName = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
        NBSSong song = loadedSongs.get(songName);
        if (song == null) {
          player.sendMessage("§cSong not found: " + songName);
        } else {
          player.sendMessage("§e§lSong Info: §f" + song.name);
          player.sendMessage("§7Author: §f" + song.author);
          player.sendMessage("§7Original Author: §f" + song.originalAuthor);
          player.sendMessage("§7Description: §f" + song.description);
          player.sendMessage("§7Duration: §f" + formatTime(song.length / song.tempo));
          player.sendMessage("§7Tempo: §f" + String.format("%.2f", song.tempo) + " ticks/second");
          player.sendMessage("§7Layers: §f" + song.layers.size());
          player.sendMessage(
              "§7Total Notes: §f"
                  + song.layers.values().stream().mapToInt(l -> l.notes.size()).sum());
        }
      }

      default -> {
        player.sendMessage("§cUnknown subcommand. Use /nbs for help");
        return false;
      }
    }

    return true;
  }

  private void playSong(Player player, String songName) {
    // Stop current song if playing
    stopSong(player);

    NBSSong song = loadedSongs.get(songName.toLowerCase());
    if (song == null) {
      player.sendMessage("§cSong not found: " + songName);
      return;
    }

    player.sendMessage("§a♪ Now playing: §f" + song.name + " §7by " + song.author);
    SongPlayback playback = new SongPlayback(player, song);
    activePlayers.put(player.getUniqueId(), playback);
    playback.start();
  }

  private void stopSong(Player player) {
    SongPlayback playback = activePlayers.remove(player.getUniqueId());
    if (playback != null) {
      playback.stop();
      player.sendMessage("§7Stopped playback");
    } else {
      player.sendMessage("§cNo song is playing");
    }
  }

  public void onDisable() {
    activePlayers.values().forEach(SongPlayback::stop);
    activePlayers.clear();
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return Arrays.asList("play", "stop", "list", "reload", "info");
    }
    if (args.length == 2
        && (args[0].equalsIgnoreCase("play") || args[0].equalsIgnoreCase("info"))) {
      return new ArrayList<>(loadedSongs.keySet());
    }
    return List.of();
  }

  // ========== NBS PARSER ==========

  private NBSSong parseNBS(Path file) throws IOException {
    try (DataInputStream in =
        new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
      NBSSong song = new NBSSong();
      song.fileName = file.getFileName().toString();

      // Read header
      short length = 0;
      byte format = readFormat(in);

      if (format >= 1) {
        in.readByte(); // vanillaInstrumentCount
      }

      length = Short.reverseBytes(in.readShort());
      song.length = length;

      short layerCount = Short.reverseBytes(in.readShort());
      song.name = readString(in);
      song.author = readString(in);
      song.originalAuthor = readString(in);
      song.description = readString(in);

      float tempo = Short.reverseBytes(in.readShort()) / 100.0f;
      song.tempo = tempo;

      in.readByte(); // auto-save
      in.readByte(); // auto-save duration
      in.readByte(); // time signature
      in.readInt(); // minutes spent
      in.readInt(); // left clicks
      in.readInt(); // right clicks
      in.readInt(); // blocks added
      in.readInt(); // blocks removed
      readString(in); // MIDI/schematic file name

      if (format >= 4) {
        in.readByte(); // loop
        in.readByte(); // max loop count
        Short.reverseBytes(in.readShort()); // loop start tick
      }

      // Read notes
      short tick = -1;
      while (true) {
        short jumps = Short.reverseBytes(in.readShort());
        if (jumps == 0) break;
        tick += jumps;

        short layer = -1;
        while (true) {
          short layerJumps = Short.reverseBytes(in.readShort());
          if (layerJumps == 0) break;
          layer += layerJumps;

          byte instrument = in.readByte();
          byte key = in.readByte();

          if (format >= 4) {
            in.readByte(); // velocity
            in.readByte(); // panning
            Short.reverseBytes(in.readShort()); // pitch
          }

          song.layers
              .computeIfAbsent(layer, k -> new Layer())
              .notes
              .add(new Note(tick, instrument, key));
        }
      }

      // Read layer info
      for (short i = 0; i < layerCount; i++) {
        Layer layer = song.layers.computeIfAbsent(i, k -> new Layer());
        layer.name = readString(in);
        if (format >= 4) {
          in.readByte(); // lock
        }
        layer.volume = in.readByte();
        if (format >= 2) {
          in.readByte(); // panning
        }
      }

      // Read custom instruments (if any) - we'll map to vanilla
      if (format >= 3) {
        byte customInstruments = in.readByte();
        for (byte i = 0; i < customInstruments; i++) {
          readString(in); // name
          readString(in); // file
          in.readByte(); // pitch
          in.readByte(); // press key
        }
      }

      if (song.name.isEmpty()) {
        song.name = file.getFileName().toString().replace(".nbs", "");
      }

      return song;
    }
  }

  private byte readFormat(DataInputStream in) throws IOException {
    // ponytail: peek first 2 bytes to detect format without consuming stream
    in.mark(2);
    short firstShort = Short.reverseBytes(in.readShort());
    in.reset();

    if (firstShort == 0) {
      in.readShort(); // consume the 0
      return in.readByte(); // read format byte
    }
    return 0; // classic format
  }

  private String readString(DataInputStream in) throws IOException {
    int length = Integer.reverseBytes(in.readInt());
    if (length == 0) return "";
    byte[] bytes = new byte[length];
    in.readFully(bytes);
    return new String(bytes, "UTF-8");
  }

  private String formatTime(float seconds) {
    int min = (int) seconds / 60;
    int sec = (int) seconds % 60;
    return String.format("%d:%02d", min, sec);
  }

  // ========== SONG PLAYBACK ==========

  private class SongPlayback {
    private final Player player;
    private final NBSSong song;
    private boolean stopped = false;

    public SongPlayback(Player player, NBSSong song) {
      this.player = player;
      this.song = song;
    }

    public void start() {
      // Group notes by tick for efficient playback
      Map<Integer, List<Note>> notesByTick = new HashMap<>();
      for (Layer layer : song.layers.values()) {
        for (Note note : layer.notes) {
          notesByTick.computeIfAbsent(note.tick, k -> new ArrayList<>()).add(note);
        }
      }

      // Schedule each tick's notes
      for (Map.Entry<Integer, List<Note>> entry : notesByTick.entrySet()) {
        int tick = entry.getKey();
        List<Note> notes = entry.getValue();

        long delayTicks = (long) (tick / song.tempo * 20); // Convert to Minecraft ticks

        scheduler.runLater(
            () -> {
              if (!stopped && player.isOnline()) {
                for (Note note : notes) {
                  playNote(note);
                }
              }
            },
            delayTicks);
      }

      // Auto-cleanup after song ends
      long songDuration = (long) (song.length / song.tempo * 20) + 20;
      scheduler.runLater(
          () -> {
            if (!stopped) {
              activePlayers.remove(player.getUniqueId());
              player.sendMessage("§7♪ Song finished");
            }
          },
          songDuration);
    }

    private void playNote(Note note) {
      // Convert NBS instrument ID to Minecraft sound
      Sound minecraftSound = getInstrumentSound(note.instrument);

      // Convert NBS key (0-87) to pitch (0.5-2.0)
      // NBS: key 45 = F#3 (pitch 1.0)
      float pitch = (float) Math.pow(2.0, (note.key - 45) / 12.0);
      pitch = Math.max(0.5f, Math.min(2.0f, pitch)); // Clamp to valid range

      sound.play(player, minecraftSound, SoundHelper.Volume.NORMAL, pitch);
    }

    public void stop() {
      stopped = true;
      // Task cancellation handled by scheduler
    }
  }

  // ========== INSTRUMENT MAPPING ==========

  private Sound getInstrumentSound(byte instrument) {
    // ponytail: direct mapping without abstraction - NBS instrument IDs to Minecraft sounds
    return switch (instrument) {
      case 0 -> Sound.BLOCK_NOTE_BLOCK_HARP; // Piano
      case 1 -> Sound.BLOCK_NOTE_BLOCK_BASS; // Double Bass
      case 2 -> Sound.BLOCK_NOTE_BLOCK_BASEDRUM; // Bass Drum
      case 3 -> Sound.BLOCK_NOTE_BLOCK_SNARE; // Snare Drum
      case 4 -> Sound.BLOCK_NOTE_BLOCK_HAT; // Click
      case 5 -> Sound.BLOCK_NOTE_BLOCK_GUITAR; // Guitar
      case 6 -> Sound.BLOCK_NOTE_BLOCK_FLUTE; // Flute
      case 7 -> Sound.BLOCK_NOTE_BLOCK_BELL; // Bell
      case 8 -> Sound.BLOCK_NOTE_BLOCK_CHIME; // Chime
      case 9 -> Sound.BLOCK_NOTE_BLOCK_XYLOPHONE; // Xylophone
      case 10 -> Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE; // Iron Xylophone
      case 11 -> Sound.BLOCK_NOTE_BLOCK_COW_BELL; // Cow Bell
      case 12 -> Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO; // Didgeridoo
      case 13 -> Sound.BLOCK_NOTE_BLOCK_BIT; // Bit
      case 14 -> Sound.BLOCK_NOTE_BLOCK_BANJO; // Banjo
      case 15 -> Sound.BLOCK_NOTE_BLOCK_PLING; // Pling
      default -> Sound.BLOCK_NOTE_BLOCK_HARP; // Fallback for custom instruments
    };
  }

  // ========== DATA STRUCTURES ==========

  private static class NBSSong {
    String fileName;
    String name = "";
    String author = "";
    String originalAuthor = "";
    String description = "";
    float tempo = 10.0f; // ticks per second
    int length = 0; // in ticks
    Map<Short, Layer> layers = new HashMap<>();
  }

  private static class Layer {
    String name = "";
    byte volume = 100;
    List<Note> notes = new ArrayList<>();
  }

  private static class Note {
    int tick;
    byte instrument;
    byte key;

    Note(int tick, byte instrument, byte key) {
      this.tick = tick;
      this.instrument = instrument;
      this.key = key;
    }
  }

  private void log(String message) {
    Bukkit.getLogger().info("[NBSPlayer] " + message);
  }
}
