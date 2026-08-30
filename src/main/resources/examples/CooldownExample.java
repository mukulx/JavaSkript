import dev.mukulx.javaskript.api.CooldownHelper;
import dev.mukulx.javaskript.api.command.CommandArgs;
import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.time.Duration;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Example demonstrating JavaSkript's memory-safe, thread-safe Cooldown API.
 *
 * <p>Features: - Per-player ability cooldowns with millisecond precision - Automated Action Bar
 * progress bar ticker - Global server-wide rate limits - Auto-purging of expired entries (zero
 * memory leaks)
 */
@FoliaSupport
public class CooldownExample implements Listener {

  private CooldownHelper cooldowns;
  private CommandHelper commands;

  public void onEnable() {
    registerCommands();
  }

  /** Cast a Fireball ability with a 10-second cooldown and live Action Bar progress bar! */
  @EventHandler
  public void onBlazeRodClick(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR
        && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    Player player = event.getPlayer();
    if (player.getInventory().getItemInMainHand().getType() != Material.BLAZE_ROD) {
      return;
    }

    // Check if player is on cooldown
    if (cooldowns.isOnCooldown(player, "fireball")) {
      long remaining = cooldowns.getRemainingSeconds(player, "fireball");
      player.sendMessage(
          Component.text("§c✖ Fireball is on cooldown! §f(" + remaining + "s remaining)"));
      return;
    }

    // Start 10-second cooldown
    cooldowns.set(player, "fireball", Duration.ofSeconds(10));

    // Show live ticking progress bar on player's action bar:
    // [■■■■■■□□□□] 6.2s -> [■■■■■■■■■■] Ready to use!
    cooldowns.startActionBarTicker(player, "fireball", "Fireball");

    // Launch fireball
    Fireball fireball = player.launchProjectile(Fireball.class);
    fireball.setIsIncendiary(false);
    fireball.setYield(2.0f);

    player.sendMessage(Component.text("§6🔥 You launched a fiery blast!"));
  }

  private void registerCommands() {
    // 1. Ability trigger command
    commands
        .create("castfireball")
        .description("Test fireball ability cooldown")
        .executesPlayer(
            (player, ctx) -> {
              if (cooldowns.isOnCooldown(player, "fireball")) {
                ctx.replyError(
                    "Fireball is on cooldown! ("
                        + cooldowns.getRemainingSeconds(player, "fireball")
                        + "s remaining)");
                return;
              }

              cooldowns.set(player, "fireball", Duration.ofSeconds(8));
              cooldowns.startActionBarTicker(player, "fireball", "Fireball");
              ctx.replySuccess("Cast Fireball! Cooldown started for 8 seconds.");
            })
        .register();

    // 2. Global rate-limited announcement
    commands
        .create("globalalert")
        .description("Send a global announcement with a 30-second server-wide cooldown")
        .argument(CommandArgs.greedyString("message"))
        .executes(
            (sender, ctx) -> {
              if (cooldowns.isGlobalOnCooldown("alert")) {
                long rem = cooldowns.getGlobalRemainingSeconds("alert");
                ctx.replyError(
                    "An alert was recently sent! Wait " + rem + "s before sending another.");
                return;
              }

              cooldowns.setGlobal("alert", Duration.ofSeconds(30));
              ctx.replySuccess("Broadcast sent! Global alert rate-limited for 30s.");
            })
        .register();

    // 3. Admin reset cooldown command
    commands
        .create("cooldownreset")
        .permission("javaskript.admin")
        .argument(CommandArgs.player("target"))
        .argument(CommandArgs.string("ability"))
        .executes(
            (sender, ctx) -> {
              Player target = ctx.getPlayer("target");
              String ability = ctx.getString("ability");
              cooldowns.reset(target, ability);
              ctx.replySuccess("Reset cooldown '" + ability + "' for " + target.getName());
            })
        .register();
  }
}
