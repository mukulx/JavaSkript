import dev.mukulx.javaskript.api.command.CommandArgs;
import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Example demonstrating JavaSkript's modern Fluent Command API.
 *
 * <p>Features: - Subcommands with recursive branching - Built-in typed argument parsing (Player,
 * Integer, Greedy Strings, Choices) - Automatic tab completion - Player-only and Console-only
 * execution guards - Clean automatic unregistration on reload
 */
@FoliaSupport
public class CommandAPIExample {

  private CommandHelper commands;
  private final Map<String, Location> warps = new HashMap<>();

  public void onEnable() {
    registerWarpCommand();
    registerEcoCommand();
    registerBroadcastCommand();
  }

  /**
   * Complex subcommand example with dynamic choices and auto tab-completion: /mywarp set <name>
   * /mywarp delete <name> /mywarp list /mywarp <name>
   */
  private void registerWarpCommand() {
    commands
        .create("mywarp")
        .description("Server warp command powered by JavaSkript Command API")
        .permission("javaskript.warp")
        .aliases("warppoint")

        // Subcommand: /mywarp set <name>
        .subcommand(
            "set",
            sub ->
                sub.permission("javaskript.warp.admin")
                    .argument(CommandArgs.string("name"))
                    .executesPlayer(
                        (player, ctx) -> {
                          String name = ctx.getString("name").toLowerCase();
                          warps.put(name, player.getLocation());
                          ctx.replySuccess("Warp '§e" + name + "§a' set to your current location!");
                        }))

        // Subcommand: /mywarp delete <name>
        .subcommand(
            "delete",
            sub ->
                sub.permission("javaskript.warp.admin")
                    .argument(CommandArgs.choice("name", () -> warps.keySet()))
                    .executes(
                        (sender, ctx) -> {
                          String name = ctx.getString("name").toLowerCase();
                          if (warps.remove(name) != null) {
                            ctx.replySuccess("Warp '§e" + name + "§a' deleted.");
                          } else {
                            ctx.replyError("Warp '§e" + name + "§c' does not exist.");
                          }
                        }))

        // Subcommand: /mywarp list
        .subcommand(
            "list",
            sub ->
                sub.executes(
                    (sender, ctx) -> {
                      if (warps.isEmpty()) {
                        ctx.reply("§eNo warps have been set yet.");
                      } else {
                        ctx.reply("§6Available warps: §f" + String.join(", ", warps.keySet()));
                      }
                    }))

        // Default root action: /mywarp <name> -> teleport
        .argument(CommandArgs.choice("name", () -> warps.keySet()))
        .executesPlayer(
            (player, ctx) -> {
              String name = ctx.getString("name").toLowerCase();
              Location target = warps.get(name);
              if (target != null) {
                player.teleportAsync(target);
                ctx.replySuccess("Teleported to warp '§e" + name + "§a'!");
              } else {
                ctx.replyError("Unknown warp: " + name + ". Use /mywarp list to view all warps.");
              }
            })
        .register();
  }

  /** Number range validation and Player argument example: /myeco give <player> <amount> */
  private void registerEcoCommand() {
    commands
        .create("myeco")
        .permission("javaskript.eco.admin")
        .subcommand(
            "give",
            sub ->
                sub.argument(CommandArgs.player("target"))
                    .argument(CommandArgs.integer("amount", 1, 1_000_000))
                    .executes(
                        (sender, ctx) -> {
                          Player target = ctx.getPlayer("target");
                          int amount = ctx.getInt("amount");
                          ctx.replySuccess("Gave §e$" + amount + " §ato §b" + target.getName());
                          target.sendMessage(
                              Component.text(
                                  "§aYou received §e$" + amount + " §afrom §b" + sender.getName()));
                        }))
        .register();
  }

  /** Greedy string argument consuming all remaining words: /myannouncement <message...> */
  private void registerBroadcastCommand() {
    commands
        .create("myannouncement")
        .permission("javaskript.broadcast")
        .argument(CommandArgs.greedyString("message"))
        .executes(
            (sender, ctx) -> {
              String message = ctx.getString("message");
              Bukkit.broadcast(Component.text("§c[ANNOUNCEMENT] §f" + message.replace('&', '§')));
            })
        .register();
  }
}
