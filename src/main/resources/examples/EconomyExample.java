package examples;

import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.api.economy.Economy;
import dev.mukulx.javaskript.api.economy.EconomyHelper;
import dev.mukulx.javaskript.api.economy.EconomyResult;
import dev.mukulx.javaskript.api.player.Players;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Universal Economy Example for JavaSkript.
 *
 * <p>Demonstrates:
 *
 * <ul>
 *   <li>Seamless Vault (EssentialsX, CMI, UltraEconomy) & Built-in SQLite support
 *   <li>Checking balances & formatted strings ($1,500.00 vs $1.5k)
 *   <li>Safe atomic player-to-player payments (/pay)
 *   <li>Admin balance adjustments (/eco add, take, set)
 *   <li>Server richest leaderboard (/baltop)
 *   <li>Custom secondary currency registration (Gems/Tokens)
 * </ul>
 */
public class EconomyExample {

  // Auto-injected by JavaSkript
  private EconomyHelper economy;
  private CommandHelper commands;

  public void onEnable() {
    Bukkit.getLogger()
        .info("[EconomyExample] Active Economy Provider: " + Economy.getProviderName());

    // 1. /balance command: View own or another player's balance
    // Usage: /bal or /balance <player>
    commands
        .create("balance")
        .aliases("bal", "money")
        .description("Check player balance")
        .playerOnly()
        .executesPlayer(
            (player, ctx) -> {
              OfflinePlayer target = player;
              if (ctx.args().length > 0) {
                target = Bukkit.getOfflinePlayer(ctx.arg(0));
              }

              double balance = Economy.getBalance(target);
              String formatted = Economy.format(balance);
              String compact = Economy.formatShort(balance);

              if (target.equals(player)) {
                Players.msg(
                    player,
                    "<gradient:#ffaa00:#ffd700><bold>✦ BALANCE:</bold></gradient> <green>"
                        + formatted
                        + " <gray>("
                        + compact
                        + ")</gray>");
              } else {
                Players.msg(
                    player,
                    "<gold>"
                        + target.getName()
                        + "'s Balance:</gold> <green>"
                        + formatted
                        + " <gray>("
                        + compact
                        + ")</gray>");
              }
              Players.sound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            })
        .register();

    // 2. /pay command: Safe atomic player-to-player transfer
    // Usage: /pay <player> <amount>
    commands
        .create("pay")
        .description("Send money to another player")
        .playerOnly()
        .executesPlayer(
            (sender, ctx) -> {
              if (ctx.args().length < 2) {
                Players.msg(sender, "<red>Usage: /pay <player> <amount>");
                return;
              }

              Player target = Bukkit.getPlayer(ctx.arg(0));
              if (target == null || !target.isOnline()) {
                Players.msg(sender, "<red>Player not found or offline.");
                return;
              }

              if (target.equals(sender)) {
                Players.msg(sender, "<red>You cannot send money to yourself!");
                return;
              }

              double amount;
              try {
                amount = Double.parseDouble(ctx.arg(1));
              } catch (NumberFormatException e) {
                Players.msg(sender, "<red>Invalid amount specified.");
                return;
              }

              if (amount <= 0) {
                Players.msg(sender, "<red>Amount must be greater than 0.");
                return;
              }

              // Atomically transfer funds
              EconomyResult result = Economy.transfer(sender, target, amount);
              if (result.isSuccess()) {
                Players.msg(
                    sender,
                    "<green>✔ Sent <bold>"
                        + Economy.format(amount)
                        + "</bold> to <yellow>"
                        + target.getName()
                        + "</yellow>! <gray>(New Balance: "
                        + Economy.format(result.getBalance())
                        + ")</gray>");
                Players.sound(sender, Sound.ENTITY_PLAYER_LEVELUP);

                Players.msg(
                    target,
                    "<green>✔ Received <bold>"
                        + Economy.format(amount)
                        + "</bold> from <yellow>"
                        + sender.getName()
                        + "</yellow>!");
                Players.sound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
              } else {
                Players.msg(sender, "<red>✖ Payment failed: " + result.getErrorMessage());
                Players.sound(sender, Sound.BLOCK_NOTE_BLOCK_BASS);
              }
            })
        .register();

    // 3. /baltop command: Top balances leaderboard
    commands
        .create("baltop")
        .description("View the richest players")
        .playerOnly()
        .executesPlayer(
            (player, ctx) -> {
              List<Map.Entry<UUID, Double>> top = Economy.getTopBalances(10);
              Players.msg(
                  player, "<gradient:#ff5555:#ffaa00><bold>✦ RICHEST PLAYERS ✦</bold></gradient>");
              if (top.isEmpty()) {
                Players.msg(
                    player,
                    "<gray>No balance records available (or using external Vault provider).");
                return;
              }

              int rank = 1;
              for (Map.Entry<UUID, Double> entry : top) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                String name = op.getName() != null ? op.getName() : "Unknown";
                Players.msg(
                    player,
                    "<yellow>#"
                        + rank
                        + " <white>"
                        + name
                        + ": <green>"
                        + Economy.format(entry.getValue()));
                rank++;
              }
            })
        .register();

    // 4. /eco admin command: Add, take, and set balances
    // Usage: /eco <add|take|set> <player> <amount>
    commands
        .create("eco")
        .permission("javaskript.admin.eco")
        .description("Manage player balances")
        .executes(
            (sender, ctx) -> {
              if (ctx.args().length < 3) {
                sender.sendMessage("§cUsage: /eco <add|take|set> <player> <amount>");
                return;
              }

              String action = ctx.arg(0).toLowerCase();
              OfflinePlayer target = Bukkit.getOfflinePlayer(ctx.arg(1));
              double amount;
              try {
                amount = Double.parseDouble(ctx.arg(2));
              } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount.");
                return;
              }

              EconomyResult res;
              switch (action) {
                case "add", "deposit", "give" -> {
                  res = Economy.deposit(target, amount);
                  if (res.isSuccess()) {
                    sender.sendMessage(
                        "§aAdded "
                            + Economy.format(amount)
                            + " to "
                            + target.getName()
                            + " §7(New: "
                            + Economy.format(res.getBalance())
                            + ")");
                  } else {
                    sender.sendMessage("§cFailed: " + res.getErrorMessage());
                  }
                }
                case "take", "withdraw", "remove" -> {
                  res = Economy.withdraw(target, amount);
                  if (res.isSuccess()) {
                    sender.sendMessage(
                        "§aTook "
                            + Economy.format(amount)
                            + " from "
                            + target.getName()
                            + " §7(New: "
                            + Economy.format(res.getBalance())
                            + ")");
                  } else {
                    sender.sendMessage("§cFailed: " + res.getErrorMessage());
                  }
                }
                case "set" -> {
                  res = Economy.set(target, amount);
                  if (res.isSuccess()) {
                    sender.sendMessage(
                        "§aSet " + target.getName() + "'s balance to " + Economy.format(amount));
                  } else {
                    sender.sendMessage("§cFailed: " + res.getErrorMessage());
                  }
                }
                default -> sender.sendMessage("§cUnknown action. Use: add, take, or set.");
              }
            })
        .register();
  }
}
