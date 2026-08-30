package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;

/** Base builder for all Paper Dialog types in JavaSkript. */
public abstract class BaseDialogBuilder<B extends BaseDialogBuilder<B>> {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  protected Component title = Component.empty();
  protected Component externalTitle = null;
  protected boolean canCloseWithEscape = true;
  protected boolean pause = false;
  protected DialogAfterAction afterAction = DialogAfterAction.CLOSE;
  protected final List<DialogBody> bodies = new ArrayList<>();
  protected final List<DialogInput> inputs = new ArrayList<>();

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }

  public static Component parseText(String text) {
    if (text == null) {
      return Component.empty();
    }
    if (text.contains("<") && text.contains(">")) {
      try {
        return MINI_MESSAGE.deserialize(text);
      } catch (Exception ignored) {
      }
    }
    return Component.text(text);
  }

  public B title(Component title) {
    this.title = title != null ? title : Component.empty();
    return self();
  }

  public B title(String title) {
    this.title = parseText(title);
    return self();
  }

  public B externalTitle(Component externalTitle) {
    this.externalTitle = externalTitle;
    return self();
  }

  public B externalTitle(String externalTitle) {
    this.externalTitle = parseText(externalTitle);
    return self();
  }

  public B canCloseWithEscape(boolean value) {
    this.canCloseWithEscape = value;
    return self();
  }

  public B pause(boolean value) {
    this.pause = value;
    return self();
  }

  public B afterAction(DialogAfterAction afterAction) {
    this.afterAction = afterAction;
    return self();
  }

  public B body(Component text) {
    this.bodies.add(DialogBodies.text(text));
    return self();
  }

  public B body(String text) {
    this.bodies.add(DialogBodies.text(parseText(text)));
    return self();
  }

  public B body(Component text, int width) {
    this.bodies.add(DialogBodies.text(text, width));
    return self();
  }

  public B body(String text, int width) {
    this.bodies.add(DialogBodies.text(parseText(text), width));
    return self();
  }

  public B body(DialogBody body) {
    if (body != null) {
      this.bodies.add(body);
    }
    return self();
  }

  public B item(ItemStack item) {
    this.bodies.add(DialogBodies.item(item));
    return self();
  }

  public B item(ItemStack item, int width, int height) {
    this.bodies.add(DialogBodies.item(item, width, height));
    return self();
  }

  public B item(
      ItemStack item,
      Component description,
      boolean showDecorations,
      boolean showTooltip,
      int width,
      int height) {
    this.bodies.add(
        DialogBodies.item(item, description, showDecorations, showTooltip, width, height));
    return self();
  }

  public B item(
      ItemStack item,
      String description,
      boolean showDecorations,
      boolean showTooltip,
      int width,
      int height) {
    return item(item, parseText(description), showDecorations, showTooltip, width, height);
  }

  public B input(DialogInput input) {
    if (input != null) {
      this.inputs.add(input);
    }
    return self();
  }

  public B textInput(String key, Component label) {
    this.inputs.add(DialogInputs.text(key, label).build());
    return self();
  }

  public B textInput(String key, String label) {
    return textInput(key, parseText(label));
  }

  public B textInput(String key, Component label, String initial, int maxLength) {
    this.inputs.add(DialogInputs.text(key, label, initial, maxLength));
    return self();
  }

  public B textInput(String key, String label, String initial, int maxLength) {
    return textInput(key, parseText(label), initial, maxLength);
  }

  public B textInput(
      String key,
      String label,
      String initial,
      int maxLength,
      boolean labelVisible,
      TextDialogInput.MultilineOptions multiline) {
    this.inputs.add(
        DialogInputs.text(key, parseText(label), initial, maxLength, labelVisible, multiline));
    return self();
  }

  public B booleanInput(String key, Component label, boolean initial) {
    this.inputs.add(DialogInputs.booleanInput(key, label, initial));
    return self();
  }

  public B booleanInput(String key, String label, boolean initial) {
    return booleanInput(key, parseText(label), initial);
  }

  public B numberInput(String key, Component label, float start, float end) {
    this.inputs.add(DialogInputs.number(key, label, start, end));
    return self();
  }

  public B numberInput(String key, String label, float start, float end) {
    return numberInput(key, parseText(label), start, end);
  }

  public B numberInput(
      String key, Component label, float start, float end, Float initial, Float step) {
    this.inputs.add(DialogInputs.number(key, label, start, end, initial, step));
    return self();
  }

  public B numberInput(
      String key, String label, float start, float end, Float initial, Float step) {
    return numberInput(key, parseText(label), start, end, initial, step);
  }

  public B numberInput(
      String key,
      Component label,
      float start,
      float end,
      Float initial,
      Float step,
      int width,
      String labelFormat) {
    this.inputs.add(DialogInputs.number(key, label, start, end, initial, step, width, labelFormat));
    return self();
  }

  public B numberInput(
      String key,
      String label,
      float start,
      float end,
      Float initial,
      Float step,
      int width,
      String labelFormat) {
    return numberInput(key, parseText(label), start, end, initial, step, width, labelFormat);
  }

  public B optionInput(
      String key, Component label, SingleOptionDialogInput.OptionEntry... entries) {
    this.inputs.add(DialogInputs.option(key, label, List.of(entries)));
    return self();
  }

  public B optionInput(String key, String label, SingleOptionDialogInput.OptionEntry... entries) {
    return optionInput(key, parseText(label), entries);
  }

  public B optionInput(
      String key, Component label, List<SingleOptionDialogInput.OptionEntry> entries) {
    this.inputs.add(DialogInputs.option(key, label, entries));
    return self();
  }

  /** Build the DialogBase instance. */
  protected DialogBase buildBase() {
    DialogBase.Builder builder = DialogBase.builder(title);
    builder.canCloseWithEscape(canCloseWithEscape);
    builder.pause(pause);
    builder.afterAction(afterAction);
    if (externalTitle != null) {
      builder.externalTitle(externalTitle);
    }
    builder.body(bodies);
    builder.inputs(inputs);
    return builder.build();
  }

  public abstract Dialog build();

  /** Show this dialog directly to an audience or player. */
  public void send(Audience audience) {
    if (audience != null) {
      audience.showDialog(build());
    }
  }
}
