package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.type.DialogType;

/** Fluent builder for Server Links dialogs. */
public final class ServerLinksDialogBuilder extends BaseDialogBuilder<ServerLinksDialogBuilder> {

  private int columns = 2;
  private int buttonWidth = 200;
  private ActionButton exitAction = null;

  public ServerLinksDialogBuilder columns(int columns) {
    this.columns = columns;
    return this;
  }

  public ServerLinksDialogBuilder buttonWidth(int buttonWidth) {
    this.buttonWidth = buttonWidth;
    return this;
  }

  public ServerLinksDialogBuilder exitAction(ActionButton button) {
    this.exitAction = button;
    return this;
  }

  public ServerLinksDialogBuilder exitAction(String label) {
    this.exitAction = DialogButtons.of(parseText(label));
    return this;
  }

  @Override
  public Dialog build() {
    DialogType type = DialogType.serverLinks(exitAction, columns, buttonWidth);
    return Dialog.create(b -> b.empty().base(buildBase()).type(type));
  }
}
