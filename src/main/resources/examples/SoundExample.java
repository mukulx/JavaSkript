import dev.mukulx.javaskript.api.SoundHelper;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

@FoliaSupport
public class SoundExample implements CommandExecutor, TabCompleter {

  private SoundHelper sound;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("§cOnly players can use this command");
      return true;
    }

    if (args.length == 0) {
      player.sendMessage("§e§lSound Examples:");
      player.sendMessage("§7/sound simple §f- Play simple sound");
      player.sendMessage("§7/sound volume §f- Play with custom volume");
      player.sendMessage("§7/sound pitch §f- Play with custom pitch");
      player.sendMessage("§7/sound location §f- Play at location");
      player.sendMessage("§7/sound broadcast §f- Broadcast to all players");
      player.sendMessage("§7/sound radius §f- Play in 10 block radius");
      player.sendMessage("§7/sound builder §f- Use builder pattern");
      player.sendMessage("§7/sound success §f- Quick success sound");
      player.sendMessage("§7/sound error §f- Quick error sound");
      player.sendMessage("§7/sound notify §f- Quick notification");
      player.sendMessage("§7/sound music §f- Musical notes");
      player.sendMessage("§7/sound categories §f- Sound categories");
      player.sendMessage("§7/sound stop §f- Stop all sounds");
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "simple" -> {
        sound.play(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        player.sendMessage("§aPlayed simple sound");
      }

      case "volume" -> {
        sound.play(player, Sound.BLOCK_NOTE_BLOCK_PLING, SoundHelper.Volume.VERY_LOUD);
        player.sendMessage("§aPlayed with VERY_LOUD volume");
      }

      case "pitch" -> {
        sound.play(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, SoundHelper.Pitch.C_HIGH);
        player.sendMessage("§aPlayed high-pitched C note");
      }

      case "location" -> {
        Location loc = player.getLocation().add(0, 2, 5);
        sound.playAt(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
        player.sendMessage("§aPlayed thunder sound 5 blocks ahead");
      }

      case "broadcast" -> {
        sound.broadcast(Sound.ENTITY_ENDER_DRAGON_GROWL);
        player.sendMessage("§aBroadcasted dragon sound to all players");
      }

      case "radius" -> {
        sound.playInRadius(player.getLocation(), 10, Sound.BLOCK_BELL_USE);
        player.sendMessage("§aPlayed bell sound in 10 block radius");
      }

      case "builder" -> {
        sound
            .builder()
            .sound(Sound.BLOCK_NOTE_BLOCK_CHIME)
            .volume(SoundHelper.Volume.LOUD)
            .pitch(SoundHelper.Pitch.E_HIGH)
            .play(player);
        player.sendMessage("§aPlayed sound using builder pattern");
      }

      case "success" -> {
        SoundHelper.Quick.success(player);
        player.sendMessage("§a✓ Success sound played!");
      }

      case "error" -> {
        SoundHelper.Quick.error(player);
        player.sendMessage("§c✗ Error sound played!");
      }

      case "notify" -> {
        SoundHelper.Quick.notify(player);
        player.sendMessage("§eℹ Notification sound played!");
      }

      case "music" -> {
        // Play a melody
        playMelody(player);
        player.sendMessage("§aPlaying musical melody...");
      }

      case "categories" -> {
        player.sendMessage("§e§lSound Categories:");
        player.sendMessage("");

        player.sendMessage("§6UI Sounds:");
        sound.play(player, SoundHelper.Category.UI.CLICK, 0.5f, 1.0f);
        player.sendMessage("  §7- Click, Toast In/Out");

        try {
          Thread.sleep(500);
        } catch (Exception ignored) {
        }

        player.sendMessage("§6Entity Sounds:");
        sound.play(player, SoundHelper.Category.Entity.LEVELUP, 1.0f, 1.0f);
        player.sendMessage("  §7- Level up, Villager, Teleport");

        try {
          Thread.sleep(500);
        } catch (Exception ignored) {
        }

        player.sendMessage("§6Block Sounds:");
        sound.play(player, SoundHelper.Category.Block.CHEST_OPEN, 1.0f, 1.0f);
        player.sendMessage("  §7- Chest, Door, Glass Break");

        try {
          Thread.sleep(500);
        } catch (Exception ignored) {
        }

        player.sendMessage("§6Music:");
        sound.play(player, SoundHelper.Category.Music.BELL, 1.0f, 1.0f);
        player.sendMessage("  §7- Pling, Bass, Guitar, Bell");

        try {
          Thread.sleep(500);
        } catch (Exception ignored) {
        }

        player.sendMessage("§6Combat:");
        sound.play(player, SoundHelper.Category.Combat.ARROW_SHOOT, 1.0f, 1.0f);
        player.sendMessage("  §7- Arrow, Sword, Explosion");
      }

      case "stop" -> {
        sound.stopAll(player);
        player.sendMessage("§7Stopped all sounds");
      }

      default -> {
        player.sendMessage("§cUnknown example. Use /sound for help");
        return false;
      }
    }

    return true;
  }

  private void playMelody(Player player) {
    // Play a simple melody: C - D - E - F - G
    playNote(player, SoundHelper.Pitch.C, 0);
    playNote(player, SoundHelper.Pitch.D, 5);
    playNote(player, SoundHelper.Pitch.E, 10);
    playNote(player, SoundHelper.Pitch.F, 15);
    playNote(player, SoundHelper.Pitch.G_HIGH, 20);

    // Final chord
    playNote(player, SoundHelper.Pitch.C_HIGH, 25);
  }

  private void playNote(Player player, float pitch, long delayTicks) {
    if (delayTicks == 0) {
      sound.play(player, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, pitch);
    } else {
      // This would need scheduler - simplified for example
      sound.play(player, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, pitch);
    }
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return Arrays.asList(
          "simple",
          "volume",
          "pitch",
          "location",
          "broadcast",
          "radius",
          "builder",
          "success",
          "error",
          "notify",
          "music",
          "categories",
          "stop");
    }
    return List.of();
  }
}
