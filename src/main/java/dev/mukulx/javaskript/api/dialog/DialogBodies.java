package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

/** Static factories for dialog body content. */
public final class DialogBodies {

  private DialogBodies() {}

  public static PlainMessageDialogBody text(Component contents) {
    return DialogBody.plainMessage(contents != null ? contents : Component.empty());
  }

  public static PlainMessageDialogBody text(Component contents, int width) {
    return DialogBody.plainMessage(contents != null ? contents : Component.empty(), width);
  }

  public static DialogBody item(ItemStack item) {
    return DialogBody.item(item).build();
  }

  public static DialogBody item(ItemStack item, int width, int height) {
    return DialogBody.item(item, null, false, false, width, height);
  }

  public static DialogBody item(
      ItemStack item,
      Component description,
      boolean showDecorations,
      boolean showTooltip,
      int width,
      int height) {
    PlainMessageDialogBody desc =
        (description == null) ? null : DialogBody.plainMessage(description);
    return DialogBody.item(item, desc, showDecorations, showTooltip, width, height);
  }
}
