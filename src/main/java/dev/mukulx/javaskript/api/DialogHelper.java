package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.dialog.*;
import io.papermc.paper.dialog.Dialog;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Modern Paper Dialog API Helper for JavaSkript scripts.
 *
 * <p>Provides ultra-fast, zero-boilerplate creation of native Minecraft dialogs:
 *
 * <ul>
 *   <li>Notice Dialogs (single button alerts & forms)
 *   <li>Confirmation Dialogs (Yes/No prompts)
 *   <li>Multi-Action Dialogs (multi-button categorized menus)
 *   <li>Dialog Lists (nested dialog navigation)
 *   <li>Server Links Dialogs (built-in server link showcases)
 * </ul>
 */
public class DialogHelper {

  private final JavaSkriptPlugin plugin;

  public DialogHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  // ==========================================
  // Fluent Builders
  // ==========================================

  public NoticeDialogBuilder notice() {
    return new NoticeDialogBuilder();
  }

  public NoticeDialogBuilder notice(String title) {
    return new NoticeDialogBuilder().title(title);
  }

  public NoticeDialogBuilder notice(String title, String body) {
    return new NoticeDialogBuilder().title(title).body(body);
  }

  public NoticeDialogBuilder notice(Component title) {
    return new NoticeDialogBuilder().title(title);
  }

  public NoticeDialogBuilder notice(Component title, Component body) {
    return new NoticeDialogBuilder().title(title).body(body);
  }

  public ConfirmationDialogBuilder confirmation() {
    return new ConfirmationDialogBuilder();
  }

  public ConfirmationDialogBuilder confirmation(String title) {
    return new ConfirmationDialogBuilder().title(title);
  }

  public ConfirmationDialogBuilder confirmation(String title, String body) {
    return new ConfirmationDialogBuilder().title(title).body(body);
  }

  public ConfirmationDialogBuilder confirmation(Component title) {
    return new ConfirmationDialogBuilder().title(title);
  }

  public ConfirmationDialogBuilder confirmation(Component title, Component body) {
    return new ConfirmationDialogBuilder().title(title).body(body);
  }

  public MultiActionDialogBuilder multiAction() {
    return new MultiActionDialogBuilder();
  }

  public MultiActionDialogBuilder multiAction(String title) {
    return new MultiActionDialogBuilder().title(title);
  }

  public MultiActionDialogBuilder multiAction(String title, String body) {
    return new MultiActionDialogBuilder().title(title).body(body);
  }

  public MultiActionDialogBuilder multiAction(Component title) {
    return new MultiActionDialogBuilder().title(title);
  }

  public MultiActionDialogBuilder multiAction(Component title, Component body) {
    return new MultiActionDialogBuilder().title(title).body(body);
  }

  public DialogListBuilder dialogList() {
    return new DialogListBuilder();
  }

  public DialogListBuilder dialogList(String title) {
    return new DialogListBuilder().title(title);
  }

  public DialogListBuilder dialogList(Component title) {
    return new DialogListBuilder().title(title);
  }

  public ServerLinksDialogBuilder serverLinks() {
    return new ServerLinksDialogBuilder();
  }

  public ServerLinksDialogBuilder serverLinks(String title) {
    return new ServerLinksDialogBuilder().title(title);
  }

  public ServerLinksDialogBuilder serverLinks(Component title) {
    return new ServerLinksDialogBuilder().title(title);
  }

  // ==========================================
  // Display & Management
  // ==========================================

  /** Show a built dialog to an audience or player. */
  public void show(Audience audience, Dialog dialog) {
    if (audience != null && dialog != null) {
      audience.showDialog(dialog);
    }
  }

  /** Close any active dialog on an audience or player. */
  public void close(Audience audience) {
    if (audience != null) {
      audience.closeDialog();
    }
  }

  // ==========================================
  // One-Liner Quick Dialogs
  // ==========================================

  /** Quick alert notice dialog. */
  public void alert(Player player, String title, String message) {
    notice(title, message).button("OK").send(player);
  }

  /** Quick confirmation dialog with yes callback. */
  public void confirm(Player player, String title, String message, Consumer<Player> onYes) {
    confirmation(title, message).onYes("Yes", onYes).onNo("No").send(player);
  }

  /** Quick confirmation dialog with yes and no callbacks. */
  public void confirm(
      Player player, String title, String message, Consumer<Player> onYes, Consumer<Player> onNo) {
    confirmation(title, message).onYes("Yes", onYes).onNo("No", onNo).send(player);
  }

  /** Quick text prompt input dialog. */
  public void input(
      Player player, String title, String prompt, BiConsumer<String, Player> onInput) {
    notice(title, prompt)
        .textInput("input_val", prompt, "", 64)
        .button(
            "Submit",
            (view, p) -> {
              String val = DialogResponses.text(view, "input_val");
              if (onInput != null) {
                onInput.accept(val, p);
              }
            })
        .send(player);
  }
}
