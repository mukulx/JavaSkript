package examples;

import dev.mukulx.javaskript.api.chat.ChatHelper;
import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.api.player.Players;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Modern Interactive Chat API Example for JavaSkript.
 *
 * <p>Demonstrates:
 *
 * <ul>
 *   <li>Interactive Clickable & Hoverable Text with Commands, URLs, and Lambda Callbacks
 *   <li>1-Line Confirmations with Clickable [CONFIRM] and [CANCEL] Buttons
 *   <li>Private Chat Prompts with Validation (Text & Numbers)
 *   <li>Paginated Chat Lists with Clickable [◀ Previous] & [Next ▶] Page Controls
 *   <li>Pixel-Perfect Centered Chat Banners
 * </ul>
 */
public class ChatAPIExample {

  // Auto-injected by JavaSkript
  private ChatHelper chat;
  private CommandHelper commands;

  public void onEnable() {

    // Main demo command: /chatdemo
    commands
        .create("chatdemo")
        .description("Explore JavaSkript Interactive Chat API")
        .subcommand(
            "buttons",
            sub ->
                sub.executesPlayer(
                    ctx -> {
                      Player player = ctx.sender();

                      // 1. Rich Interactive Clickable Buttons
                      chat.builder()
                          .text(
                              "<gradient:#ff5555:#ffaa00><bold>✦ CHAT BUTTONS ✦</bold></gradient>")
                          .newLine()
                          .text("<yellow>Interactive Actions: ")
                          .newLine()
                          // Click to run command:
                          .clickRun(
                              "<green><bold>[Claim Kit]</bold></green>",
                              "/kit starter",
                              "<gray>Click to receive Starter Kit!")
                          .space()
                          // Click to suggest command into chatbox:
                          .clickSuggest(
                              "<aqua><bold>[Message Admin]</bold></aqua>",
                              "/msg admin ",
                              "<gray>Click to pre-fill message command")
                          .space()
                          // Click to copy text to clipboard:
                          .clickCopy(
                              "<light_purple><bold>[Copy IP]</bold></light_purple>",
                              "play.myserver.net",
                              "<gray>Click to copy server IP to clipboard")
                          .space()
                          // Click to open external URL:
                          .clickUrl(
                              "<gold><bold>[Discord]</bold></gold>",
                              "https://discord.gg/minecraft",
                              "<gray>Click to open our Discord community")
                          .newLine()
                          // Click to execute an in-memory Java lambda callback directly!
                          .clickAction(
                              "<yellow><bold>[⚡ Mystery Reward (Java Lambda)]</bold></yellow>",
                              p -> {
                                Players.sound(p, Sound.ENTITY_PLAYER_LEVELUP);
                                Players.title(p, "<gold>MYSTERY REWARD!", "<yellow>+500 Coins");
                                Players.msg(
                                    p, "<green>✔ You triggered a live Java lambda click action!");
                              },
                              "<gray>Click to trigger custom Java code!")
                          .send(player);
                    }))
        .subcommand(
            "confirm",
            sub ->
                sub.executesPlayer(
                    ctx -> {
                      Player player = ctx.sender();

                      // 2. Interactive Confirm/Deny Workflow
                      chat.confirm(player)
                          .question(
                              "<gold>Purchase <yellow>Fly Perk</yellow> for <green>$5,000</green>?")
                          .accept(
                              "<green><bold>[CONFIRM PURCHASE]</bold></green>",
                              p -> {
                                Players.sound(p, Sound.ENTITY_PLAYER_LEVELUP);
                                Players.msg(p, "<green>✔ Perk purchased! Flight mode enabled.");
                              },
                              "<gray>Click to confirm purchase")
                          .deny(
                              "<red><bold>[CANCEL]</bold></red>",
                              p -> {
                                Players.sound(p, Sound.BLOCK_NOTE_BLOCK_BASS);
                                Players.msg(p, "<red>✖ Purchase canceled.");
                              },
                              "<gray>Click to reject purchase")
                          .timeout(Duration.ofSeconds(20))
                          .onTimeout(p -> Players.msg(p, "<gray>Transaction timed out."))
                          .send();
                    }))
        .subcommand(
            "prompt",
            sub ->
                sub.executesPlayer(
                    ctx -> {
                      Player player = ctx.sender();

                      // 3. Private Chat Input Prompt
                      // Captures player's next chat message, hides it from everyone else!
                      chat.prompt(
                          player,
                          "<yellow>Type your new player title in chat (or type 'cancel'):",
                          (p, title) -> {
                            Players.sound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
                            Players.msg(
                                p, "<green>✔ Title updated to: <yellow>" + title + "</yellow>!");
                          });
                    }))
        .subcommand(
            "number",
            sub ->
                sub.executesPlayer(
                    ctx -> {
                      Player player = ctx.sender();

                      // 4. Validated Numeric Prompt (1 - 1,000)
                      chat.promptInteger(
                          player,
                          "<yellow>Enter diamond bet amount (1 - 1,000):",
                          1,
                          1000,
                          (p, amount) -> {
                            Players.sound(p, Sound.ENTITY_PLAYER_LEVELUP);
                            Players.msg(
                                p,
                                "<aqua>💎 You placed a bet of <bold>"
                                    + amount
                                    + "</bold> Diamonds!");
                          });
                    }))
        .subcommand(
            "pager",
            sub ->
                sub.executesPlayer(
                    ctx -> {
                      Player player = ctx.sender();

                      // 5. Paginated Interactive List with Clickable [◀ Previous] & [Next ▶]
                      List<String> items = new ArrayList<>();
                      for (int i = 1; i <= 25; i++) {
                        items.add("Warp Island #" + i);
                      }

                      chat.pager(player, "Server Warps", items)
                          .pageSize(6)
                          .header("<gold>✦ %title% <gray>(Page %page%/%max%)</gray> ✦")
                          .formatter((index, warp) -> "<yellow>#" + index + " <white>" + warp)
                          .send(1);
                    }))
        .subcommand(
            "banner",
            sub ->
                sub.executesPlayer(
                    ctx -> {
                      Player player = ctx.sender();

                      // 6. Pixel-Perfect Centered Chat Banners
                      chat.sendCentered(
                          player, "<gold>------------------------------------</gold>");
                      chat.sendCentered(
                          player,
                          "<gradient:#ff5555:#ffaa00><bold>✦ MYTHIC EVENT ACTIVE ✦</bold></gradient>");
                      chat.sendCentered(
                          player, "<yellow>Double EXP & Loot enabled across all realms!</yellow>");
                      chat.sendCentered(player, "<gray>Ends in: 2h 45m</gray>");
                      chat.sendCentered(
                          player, "<gold>------------------------------------</gold>");
                    }))
        .register();
  }
}
