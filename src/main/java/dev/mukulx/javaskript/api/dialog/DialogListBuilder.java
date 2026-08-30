package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Fluent builder for Dialog List dialogs. */
public final class DialogListBuilder extends BaseDialogBuilder<DialogListBuilder> {

  private final List<Dialog> dialogs = new ArrayList<>();
  private int columns = 2;
  private int buttonWidth = 200;
  private ActionButton exitAction = null;

  public DialogListBuilder dialog(Dialog dialog) {
    if (dialog != null) {
      this.dialogs.add(dialog);
    }
    return this;
  }

  public DialogListBuilder dialogs(Collection<Dialog> dialogs) {
    if (dialogs != null) {
      this.dialogs.addAll(dialogs);
    }
    return this;
  }

  public DialogListBuilder columns(int columns) {
    this.columns = columns;
    return this;
  }

  public DialogListBuilder buttonWidth(int buttonWidth) {
    this.buttonWidth = buttonWidth;
    return this;
  }

  public DialogListBuilder exitAction(ActionButton button) {
    this.exitAction = button;
    return this;
  }

  public DialogListBuilder exitAction(String label) {
    this.exitAction = DialogButtons.of(parseText(label));
    return this;
  }

  @Override
  public Dialog build() {
    RegistrySet<Dialog> set = RegistrySet.valueSet(RegistryKey.DIALOG, dialogs);
    DialogType type = DialogType.dialogList(set, exitAction, columns, buttonWidth);
    return Dialog.create(b -> b.empty().base(buildBase()).type(type));
  }
}
