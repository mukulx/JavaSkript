package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Fluent builder for Multi-Action dialogs. */
public final class MultiActionDialogBuilder extends BaseDialogBuilder<MultiActionDialogBuilder> {

  private final List<ActionButton> actions = new ArrayList<>();
  private int columns = 2;
  private ActionButton exitAction = null;

  public MultiActionDialogBuilder button(ActionButton button) {
    if (button != null) {
      this.actions.add(button);
    }
    return this;
  }

  public MultiActionDialogBuilder button(Component label, DialogAction action) {
    return button(DialogButtons.of(label, action));
  }

  public MultiActionDialogBuilder button(String label, DialogAction action) {
    return button(parseText(label), action);
  }

  public MultiActionDialogBuilder button(Component label) {
    return button(DialogButtons.of(label));
  }

  public MultiActionDialogBuilder button(String label) {
    return button(parseText(label));
  }

  public MultiActionDialogBuilder button(Component label, DialogActions.PlayerCallback callback) {
    return button(DialogButtons.playerClickable(label, callback));
  }

  public MultiActionDialogBuilder button(String label, DialogActions.PlayerCallback callback) {
    return button(parseText(label), callback);
  }

  public MultiActionDialogBuilder button(
      Component label, java.util.function.Consumer<Player> consumer) {
    return button(DialogButtons.playerAction(label, consumer));
  }

  public MultiActionDialogBuilder button(
      String label, java.util.function.Consumer<Player> consumer) {
    return button(parseText(label), consumer);
  }

  public MultiActionDialogBuilder button(
      Component label, Component tooltip, int width, DialogActions.PlayerCallback callback) {
    return button(DialogButtons.playerClickable(label, tooltip, width, callback));
  }

  public MultiActionDialogBuilder button(
      String label, String tooltip, int width, DialogActions.PlayerCallback callback) {
    return button(
        DialogButtons.playerClickable(parseText(label), parseText(tooltip), width, callback));
  }

  public MultiActionDialogBuilder button(
      Component label, Component tooltip, int width, java.util.function.Consumer<Player> consumer) {
    return button(DialogButtons.playerAction(label, tooltip, width, consumer));
  }

  public MultiActionDialogBuilder button(
      String label, String tooltip, int width, java.util.function.Consumer<Player> consumer) {
    return button(
        DialogButtons.playerAction(parseText(label), parseText(tooltip), width, consumer));
  }

  public MultiActionDialogBuilder button(
      String label, String tooltip, DialogActions.PlayerCallback callback) {
    return button(
        DialogButtons.playerClickable(parseText(label), parseText(tooltip), 150, callback));
  }

  public MultiActionDialogBuilder button(
      String label, String tooltip, java.util.function.Consumer<Player> consumer) {
    return button(DialogButtons.playerAction(parseText(label), parseText(tooltip), 150, consumer));
  }

  public MultiActionDialogBuilder columns(int columns) {
    this.columns = columns;
    return this;
  }

  public MultiActionDialogBuilder exitAction(ActionButton button) {
    this.exitAction = button;
    return this;
  }

  public MultiActionDialogBuilder exitAction(Component label) {
    this.exitAction = DialogButtons.of(label);
    return this;
  }

  public MultiActionDialogBuilder exitAction(String label) {
    this.exitAction = DialogButtons.of(parseText(label));
    return this;
  }

  public MultiActionDialogBuilder exitAction(Component label, DialogAction action) {
    this.exitAction = DialogButtons.of(label, action);
    return this;
  }

  public MultiActionDialogBuilder exitAction(String label, DialogAction action) {
    this.exitAction = DialogButtons.of(parseText(label), action);
    return this;
  }

  public MultiActionDialogBuilder exitAction(
      Component label, DialogActions.PlayerCallback callback) {
    this.exitAction = DialogButtons.playerClickable(label, callback);
    return this;
  }

  public MultiActionDialogBuilder exitAction(String label, DialogActions.PlayerCallback callback) {
    this.exitAction = DialogButtons.playerClickable(parseText(label), callback);
    return this;
  }

  public MultiActionDialogBuilder exitAction(
      Component label, java.util.function.Consumer<Player> consumer) {
    this.exitAction = DialogButtons.playerAction(label, consumer);
    return this;
  }

  public MultiActionDialogBuilder exitAction(
      String label, java.util.function.Consumer<Player> consumer) {
    this.exitAction = DialogButtons.playerAction(parseText(label), consumer);
    return this;
  }

  @Override
  public Dialog build() {
    DialogType type = DialogType.multiAction(actions, exitAction, columns);
    return Dialog.create(b -> b.empty().base(buildBase()).type(type));
  }
}
