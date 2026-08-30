package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import java.util.List;
import net.kyori.adventure.text.Component;

/** Static factories for dialog inputs (text, boolean, number, options). */
public final class DialogInputs {

  private DialogInputs() {}

  public static TextDialogInput.Builder text(String key, Component label) {
    return DialogInput.text(key, label);
  }

  public static DialogInput text(String key, Component label, String initial, int maxLength) {
    return DialogInput.text(key, label).initial(initial).maxLength(maxLength).build();
  }

  public static DialogInput text(
      String key,
      Component label,
      String initial,
      int maxLength,
      boolean labelVisible,
      TextDialogInput.MultilineOptions multiline) {
    TextDialogInput.Builder builder =
        DialogInput.text(key, label)
            .initial(initial)
            .maxLength(maxLength)
            .labelVisible(labelVisible);
    if (multiline != null) {
      builder.multiline(multiline);
    }
    return builder.build();
  }

  public static TextDialogInput.MultilineOptions multiline(Integer maxLines, Integer height) {
    return TextDialogInput.MultilineOptions.create(maxLines, height);
  }

  public static BooleanDialogInput booleanInput(String key, Component label, boolean initial) {
    return DialogInput.bool(key, label).initial(initial).build();
  }

  public static NumberRangeDialogInput number(String key, Component label, float start, float end) {
    return DialogInput.numberRange(key, label, start, end).build();
  }

  public static NumberRangeDialogInput number(
      String key, Component label, float start, float end, Float initial, Float step) {
    return number(key, label, start, end, initial, step, 0, null);
  }

  public static NumberRangeDialogInput number(
      String key,
      Component label,
      float start,
      float end,
      Float initial,
      Float step,
      int width,
      String labelFormat) {
    NumberRangeDialogInput.Builder builder = DialogInput.numberRange(key, label, start, end);
    if (initial != null) {
      builder.initial(initial);
    }
    if (step != null) {
      builder.step(step);
    }
    if (width > 0) {
      builder.width(width);
    }
    if (labelFormat != null) {
      builder.labelFormat(labelFormat);
    }
    return builder.build();
  }

  public static SingleOptionDialogInput option(
      String key, Component label, SingleOptionDialogInput.OptionEntry... entries) {
    return DialogInput.singleOption(key, label, List.of(entries)).build();
  }

  public static SingleOptionDialogInput option(
      String key, Component label, List<SingleOptionDialogInput.OptionEntry> entries) {
    return DialogInput.singleOption(key, label, entries).build();
  }

  public static SingleOptionDialogInput.OptionEntry optionEntry(
      String id, Component display, boolean initial) {
    return SingleOptionDialogInput.OptionEntry.create(id, display, initial);
  }
}
