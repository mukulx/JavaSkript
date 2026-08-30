package dev.mukulx.javaskript.api.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Fluent builder for creating rich interactive chat components with clickable actions, hover
 * tooltips, URLs, and executable lambda callbacks.
 */
public class ChatBuilder {

  private final ChatHelper chatHelper;
  private final List<Component> parts = new ArrayList<>();

  public ChatBuilder(ChatHelper chatHelper) {
    this.chatHelper = chatHelper;
  }

  /** Append formatted text (supports MiniMessage and legacy & color codes). */
  public ChatBuilder text(String text) {
    if (text != null && !text.isEmpty()) {
      parts.add(chatHelper.parse(text));
    }
    return this;
  }

  /** Append an Adventure Component directly. */
  public ChatBuilder text(Component component) {
    if (component != null) {
      parts.add(component);
    }
    return this;
  }

  /**
   * Append clickable text that executes a console/chat command when clicked, with an optional hover
   * tooltip.
   *
   * @param text Button display text
   * @param command Command string to run (e.g. "/warp spawn")
   * @param hoverTooltip Optional tooltip text on mouse hover
   */
  public ChatBuilder clickRun(String text, String command, String hoverTooltip) {
    if (text == null) return this;
    Component comp = chatHelper.parse(text);
    if (command != null && !command.isEmpty()) {
      String cmd = command.startsWith("/") ? command : "/" + command;
      comp = comp.clickEvent(ClickEvent.runCommand(cmd));
    }
    if (hoverTooltip != null && !hoverTooltip.isEmpty()) {
      comp = comp.hoverEvent(HoverEvent.showText(chatHelper.parse(hoverTooltip)));
    }
    parts.add(comp);
    return this;
  }

  /**
   * Append clickable text that pre-fills the player's chat prompt with command/template text.
   *
   * @param text Display text
   * @param commandTemplate Text to insert in chat
   * @param hoverTooltip Optional tooltip text
   */
  public ChatBuilder clickSuggest(String text, String commandTemplate, String hoverTooltip) {
    if (text == null) return this;
    Component comp = chatHelper.parse(text);
    if (commandTemplate != null) {
      comp = comp.clickEvent(ClickEvent.suggestCommand(commandTemplate));
    }
    if (hoverTooltip != null && !hoverTooltip.isEmpty()) {
      comp = comp.hoverEvent(HoverEvent.showText(chatHelper.parse(hoverTooltip)));
    }
    parts.add(comp);
    return this;
  }

  /**
   * Append clickable text that opens a URL in the player's web browser.
   *
   * @param text Display text
   * @param url Web URL (e.g. "https://discord.gg/example")
   * @param hoverTooltip Optional tooltip text
   */
  public ChatBuilder clickUrl(String text, String url, String hoverTooltip) {
    if (text == null) return this;
    Component comp = chatHelper.parse(text);
    if (url != null && !url.isEmpty()) {
      comp = comp.clickEvent(ClickEvent.openUrl(url));
    }
    if (hoverTooltip != null && !hoverTooltip.isEmpty()) {
      comp = comp.hoverEvent(HoverEvent.showText(chatHelper.parse(hoverTooltip)));
    }
    parts.add(comp);
    return this;
  }

  /**
   * Append clickable text that copies text to the player's clipboard.
   *
   * @param text Display text
   * @param toClipboard Text to copy
   * @param hoverTooltip Optional tooltip text
   */
  public ChatBuilder clickCopy(String text, String toClipboard, String hoverTooltip) {
    if (text == null) return this;
    Component comp = chatHelper.parse(text);
    if (toClipboard != null) {
      comp = comp.clickEvent(ClickEvent.copyToClipboard(toClipboard));
    }
    if (hoverTooltip != null && !hoverTooltip.isEmpty()) {
      comp = comp.hoverEvent(HoverEvent.showText(chatHelper.parse(hoverTooltip)));
    }
    parts.add(comp);
    return this;
  }

  /**
   * Append clickable text that executes a Java lambda {@link Consumer} callback when clicked by a
   * player!
   *
   * @param text Display text
   * @param action Lambda executed when the clicking player clicks this text
   * @param hoverTooltip Optional tooltip text
   */
  public ChatBuilder clickAction(String text, Consumer<Player> action, String hoverTooltip) {
    if (text == null) return this;
    String token = chatHelper.registerAction(action, false);
    return clickRun(text, "/__jsk_action " + token, hoverTooltip);
  }

  /** Append text with a hover tooltip. */
  public ChatBuilder hover(String text, String hoverTooltip) {
    if (text == null) return this;
    Component comp = chatHelper.parse(text);
    if (hoverTooltip != null && !hoverTooltip.isEmpty()) {
      comp = comp.hoverEvent(HoverEvent.showText(chatHelper.parse(hoverTooltip)));
    }
    parts.add(comp);
    return this;
  }

  /** Append a space. */
  public ChatBuilder space() {
    parts.add(Component.text(" "));
    return this;
  }

  /** Append a new line. */
  public ChatBuilder newLine() {
    parts.add(Component.newline());
    return this;
  }

  /** Build the complete assembled {@link Component}. */
  public Component build() {
    if (parts.isEmpty()) return Component.empty();
    Component root = Component.empty();
    for (Component part : parts) {
      root = root.append(part);
    }
    return root;
  }

  /** Send this assembled message to a player or command sender. */
  public void send(CommandSender sender) {
    if (sender != null) {
      sender.sendMessage(build());
    }
  }

  /** Broadcast this assembled message to all online players and console. */
  public void broadcast() {
    Bukkit.broadcast(build());
  }
}
