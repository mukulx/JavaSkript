package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Fluent builder for Confirmation dialogs (Yes / No). */
public final class ConfirmationDialogBuilder extends BaseDialogBuilder<ConfirmationDialogBuilder> {

  private ActionButton yes = null;
  private ActionButton no = null;

  public ConfirmationDialogBuilder onYes(ActionButton button) {
    this.yes = button;
    return this;
  }

  public ConfirmationDialogBuilder onYes(Component label, DialogAction action) {
    return onYes(DialogButtons.of(label, action));
  }

  public ConfirmationDialogBuilder onYes(String label, DialogAction action) {
    return onYes(parseText(label), action);
  }

  public ConfirmationDialogBuilder onYes(Component label, DialogActions.PlayerCallback callback) {
    return onYes(DialogButtons.playerClickable(label, callback));
  }

  public ConfirmationDialogBuilder onYes(String label, DialogActions.PlayerCallback callback) {
    return onYes(parseText(label), callback);
  }

  public ConfirmationDialogBuilder onYes(
      Component label, java.util.function.Consumer<Player> consumer) {
    return onYes(DialogButtons.playerAction(label, consumer));
  }

  public ConfirmationDialogBuilder onYes(
      String label, java.util.function.Consumer<Player> consumer) {
    return onYes(parseText(label), consumer);
  }

  public ConfirmationDialogBuilder onYes(Component label) {
    return onYes(DialogButtons.of(label));
  }

  public ConfirmationDialogBuilder onYes(String label) {
    return onYes(parseText(label));
  }

  public ConfirmationDialogBuilder onNo(ActionButton button) {
    this.no = button;
    return this;
  }

  public ConfirmationDialogBuilder onNo(Component label, DialogAction action) {
    return onNo(DialogButtons.of(label, action));
  }

  public ConfirmationDialogBuilder onNo(String label, DialogAction action) {
    return onNo(parseText(label), action);
  }

  public ConfirmationDialogBuilder onNo(Component label, DialogActions.PlayerCallback callback) {
    return onNo(DialogButtons.playerClickable(label, callback));
  }

  public ConfirmationDialogBuilder onNo(String label, DialogActions.PlayerCallback callback) {
    return onNo(parseText(label), callback);
  }

  public ConfirmationDialogBuilder onNo(
      Component label, java.util.function.Consumer<Player> consumer) {
    return onNo(DialogButtons.playerAction(label, consumer));
  }

  public ConfirmationDialogBuilder onNo(
      String label, java.util.function.Consumer<Player> consumer) {
    return onNo(parseText(label), consumer);
  }

  public ConfirmationDialogBuilder onNo(Component label) {
    return onNo(DialogButtons.of(label));
  }

  public ConfirmationDialogBuilder onNo(String label) {
    return onNo(parseText(label));
  }

  @Override
  public Dialog build() {
    ActionButton yesButton = (yes != null) ? yes : DialogButtons.of(Component.text("Yes"));
    ActionButton noButton = (no != null) ? no : DialogButtons.of(Component.text("No"));
    DialogType type = DialogType.confirmation(yesButton, noButton);
    return Dialog.create(b -> b.empty().base(buildBase()).type(type));
  }
}
