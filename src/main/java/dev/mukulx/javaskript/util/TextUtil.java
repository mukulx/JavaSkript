package dev.mukulx.javaskript.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * High-performance text and component parser supporting MiniMessage, legacy ampersand, section
 * symbol formatting, and zero-allocation plain-text fast paths.
 */
public final class TextUtil {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.builder().character('&').hexColors().build();

  private TextUtil() {}

  /**
   * Parse formatted text with support for MiniMessage tags (<gold>, <bold>, <gradient:...>) and
   * legacy color codes (&a, &l). Default italics are disabled.
   *
   * @param input Raw text string
   * @return Formatted Adventure Component
   */
  public static Component parse(String input) {
    if (input == null || input.isEmpty()) {
      return Component.empty();
    }

    // Fast-path: return plain text component immediately if no formatting tags present
    if (!input.contains("<") && !input.contains("&") && !input.contains("§")) {
      return Component.text(input).decoration(TextDecoration.ITALIC, false);
    }

    Component comp;
    if (input.contains("<") && input.contains(">")) {
      try {
        comp = MINI_MESSAGE.deserialize(input);
      } catch (Exception ignored) {
        comp = LEGACY_AMPERSAND.deserialize(input.replace('§', '&'));
      }
    } else {
      comp = LEGACY_AMPERSAND.deserialize(input.replace('§', '&'));
    }

    if (!comp.hasDecoration(TextDecoration.ITALIC)) {
      comp = comp.decoration(TextDecoration.ITALIC, false);
    }
    return comp;
  }
}
