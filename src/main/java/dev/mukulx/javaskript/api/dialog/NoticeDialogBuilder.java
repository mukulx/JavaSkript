package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;

/** Fluent builder for Notice dialogs (single button). */
public final class NoticeDialogBuilder extends BaseDialogBuilder<NoticeDialogBuilder> {

  private ActionButton button = null;

  public NoticeDialogBuilder button(ActionButton button) {
    this.button = button;
    return this;
  }

  public NoticeDialogBuilder button(Component label, DialogAction action) {
    return button(DialogButtons.of(label, action));
  }

  public NoticeDialogBuilder button(String label, DialogAction action) {
    return button(parseText(label), action);
  }

  public NoticeDialogBuilder button(Component label, DialogActions.PlayerCallback callback) {
    return button(DialogButtons.playerClickable(label, callback));
  }

  public NoticeDialogBuilder button(String label, DialogActions.PlayerCallback callback) {
    return button(parseText(label), callback);
  }

  public NoticeDialogBuilder button(
      Component label, java.util.function.Consumer<org.bukkit.entity.Player> action) {
    return button(DialogButtons.playerAction(label, action));
  }

  public NoticeDialogBuilder button(
      String label, java.util.function.Consumer<org.bukkit.entity.Player> action) {
    return button(parseText(label), action);
  }

  public NoticeDialogBuilder button(Component label) {
    return button(DialogButtons.of(label));
  }

  public NoticeDialogBuilder button(String label) {
    return button(parseText(label));
  }

  public NoticeDialogBuilder button(
      Component label, Component tooltip, int width, DialogActions.PlayerCallback callback) {
    return button(DialogButtons.playerClickable(label, tooltip, width, callback));
  }

  public NoticeDialogBuilder button(
      String label, String tooltip, int width, DialogActions.PlayerCallback callback) {
    return button(
        DialogButtons.playerClickable(parseText(label), parseText(tooltip), width, callback));
  }

  public NoticeDialogBuilder button(
      Component label,
      Component tooltip,
      int width,
      java.util.function.Consumer<org.bukkit.entity.Player> action) {
    return button(DialogButtons.playerAction(label, tooltip, width, action));
  }

  public NoticeDialogBuilder button(
      String label,
      String tooltip,
      int width,
      java.util.function.Consumer<org.bukkit.entity.Player> action) {
    return button(DialogButtons.playerAction(parseText(label), parseText(tooltip), width, action));
  }

  @Override
  public Dialog build() {
    DialogType type = (button == null) ? DialogType.notice() : DialogType.notice(button);
    return Dialog.create(b -> b.empty().base(buildBase()).type(type));
  }
}
