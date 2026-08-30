package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.dialog.DialogResponseView;

/** Helpers for reading values out of a {@link DialogResponseView}. */
public final class DialogResponses {

  private DialogResponses() {}

  public static String text(DialogResponseView view, String key) {
    return view != null ? view.getText(key) : null;
  }

  public static Boolean bool(DialogResponseView view, String key) {
    return view != null ? view.getBoolean(key) : null;
  }

  public static Float number(DialogResponseView view, String key) {
    return view != null ? view.getFloat(key) : null;
  }
}
