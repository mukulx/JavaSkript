package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import net.kyori.adventure.text.Component;

/** Static factories for {@link ActionButton}. */
public final class DialogButtons {

  private DialogButtons() {}

  public static ActionButton of(Component label) {
    return ActionButton.builder(label).build();
  }

  public static ActionButton of(Component label, DialogAction action) {
    return ActionButton.builder(label).action(action).build();
  }

  public static ActionButton of(
      Component label, Component tooltip, int width, DialogAction action) {
    ActionButton.Builder builder = ActionButton.builder(label).action(action);
    if (tooltip != null) {
      builder.tooltip(tooltip);
    }
    if (width > 0) {
      builder.width(width);
    }
    return builder.build();
  }

  public static ActionButton custom(Component label, DialogActionCallback callback) {
    return of(label, DialogActions.custom(callback));
  }

  public static ActionButton custom(
      Component label, Component tooltip, int width, DialogActionCallback callback) {
    return of(label, tooltip, width, DialogActions.custom(callback));
  }

  public static ActionButton playerClickable(
      Component label, DialogActions.PlayerCallback callback) {
    return of(label, DialogActions.playerCallback(callback));
  }

  public static ActionButton playerClickable(
      Component label, Component tooltip, int width, DialogActions.PlayerCallback callback) {
    return of(label, tooltip, width, DialogActions.playerCallback(callback));
  }

  public static ActionButton playerAction(
      Component label, java.util.function.Consumer<org.bukkit.entity.Player> action) {
    return of(label, DialogActions.playerAction(action));
  }

  public static ActionButton playerAction(
      Component label,
      Component tooltip,
      int width,
      java.util.function.Consumer<org.bukkit.entity.Player> action) {
    return of(label, tooltip, width, DialogActions.playerAction(action));
  }
}
