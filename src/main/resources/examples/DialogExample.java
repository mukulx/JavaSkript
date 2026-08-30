package examples;

import dev.mukulx.javaskript.api.DialogHelper;
import dev.mukulx.javaskript.api.dialog.DialogActions;
import dev.mukulx.javaskript.api.dialog.DialogResponses;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Dialog API Example for JavaSkript.
 *
 * <p>Demonstrates native Minecraft Dialog windows with zero boilerplate: - Simple Notices & Alerts
 * - Interactive Yes/No Confirmations - Multi-action Menus with URLs and Commands - Form Inputs
 * (Text, Boolean Checkboxes, Number Sliders) - Item Showcases
 */
public class DialogExample implements CommandExecutor {

  // Auto-injected by JavaSkript
  private DialogHelper dialog;

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("This command is for players only.");
      return true;
    }

    if (args.length == 0) {
      openMainMenu(player);
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "alert" ->
          dialog.alert(
              player, "<gold>Notice</gold>", "<yellow>This is an instant alert dialog!</yellow>");
      case "confirm" -> openConfirmation(player);
      case "form" -> openSettingsForm(player);
      case "item" -> openItemShowcase(player);
      default -> openMainMenu(player);
    }
    return true;
  }

  /** 1. Multi-Action Main Menu */
  private void openMainMenu(Player player) {
    dialog
        .multiAction("<gradient:#ff5555:#ffaa00>Server Menu</gradient>")
        .body(
            "<gray>Welcome, <white>"
                + player.getName()
                + "</white>! Choose an action below:</gray>")
        .columns(2)
        .button(
            "Heal Me",
            "Restores full health",
            p -> {
              p.setHealth(p.getMaxHealth());
              p.sendMessage("§aYou have been healed!");
            })
        .button("Website", DialogActions.openUrl("https://papermc.io"))
        .button("Discord", DialogActions.openUrl("https://discord.gg"))
        .button("Showcase Item", p -> openItemShowcase(p))
        .button("Settings Form", p -> openSettingsForm(p))
        .exitAction("Close")
        .send(player);
  }

  /** 2. Confirmation Dialog */
  private void openConfirmation(Player player) {
    dialog
        .confirmation("<red>Reset Progress?</red>")
        .body("<gray>Are you sure you want to reset your statistics? This cannot be undone.</gray>")
        .onYes(
            "<red>Yes, Reset</red>",
            p -> {
              p.sendMessage("§cYour statistics have been reset.");
            })
        .onNo(
            "<green>Cancel</green>",
            p -> {
              p.sendMessage("§aCancelled.");
            })
        .send(player);
  }

  /** 3. Form Input Dialog */
  private void openSettingsForm(Player player) {
    dialog
        .notice("<aqua>Profile Settings</aqua>")
        .body("<gray>Update your server preferences below:</gray>")
        .textInput("nickname", "Nickname", player.getName(), 16)
        .booleanInput("notifications", "Receive Notifications?", true)
        .numberInput("volume", "Music Volume", 0f, 100f, 75f, 5f)
        .button(
            "<green>Save Changes</green>",
            (view, p) -> {
              String nick = DialogResponses.text(view, "nickname");
              Boolean notify = DialogResponses.bool(view, "notifications");
              Float vol = DialogResponses.number(view, "volume");

              p.sendMessage(
                  "§aSaved! Nick: "
                      + nick
                      + " | Notifications: "
                      + notify
                      + " | Volume: "
                      + vol
                      + "%");
            })
        .send(player);
  }

  /** 4. Item Showcase Dialog */
  private void openItemShowcase(Player player) {
    ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
    dialog
        .notice("<gold>Netherite Sword</gold>")
        .item(sword, 64, 64)
        .button("<green>Close</green>")
        .send(player);
  }
}
