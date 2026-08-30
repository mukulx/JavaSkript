package dev.mukulx.javaskript.api.chat;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Interactive, paginated chat list renderer.
 *
 * <p>Automates page calculations and clickable {@code [◀ Previous]} and {@code [Next ▶]} buttons
 * for lists (e.g. warps, leaderboards, shop items, quest logs).
 *
 * @param <T> Item type in list
 */
public class ChatPager<T> {

  private final ChatHelper chatHelper;
  private final Player player;
  private final String title;
  private final List<T> items;

  private int pageSize = 7;
  private String header = "<gold>✦ %title% <gray>(Page %page%/%max%)</gray> ✦";
  private String emptyMessage = "<gray>No entries to display.</gray>";
  private BiFunction<Integer, T, Component> formatter;
  private String pageCommand = null;

  public ChatPager(ChatHelper chatHelper, Player player, String title, List<T> items) {
    this.chatHelper = chatHelper;
    this.player = player;
    this.title = title != null ? title : "List";
    this.items = items != null ? items : Collections.emptyList();
  }

  /** Set number of items per page (default: 7). */
  public ChatPager<T> pageSize(int size) {
    this.pageSize = Math.max(1, size);
    return this;
  }

  /** Set custom header format. Available placeholders: %title%, %page%, %max%, %total%. */
  public ChatPager<T> header(String header) {
    this.header = header;
    return this;
  }

  /** Set message shown when the list is empty. */
  public ChatPager<T> emptyMessage(String emptyMessage) {
    this.emptyMessage = emptyMessage;
    return this;
  }

  /** Format list item as a string (MiniMessage supported). Receives (1-based index, item). */
  public ChatPager<T> formatter(BiFunction<Integer, T, String> stringFormatter) {
    if (stringFormatter != null) {
      this.formatter = (idx, item) -> chatHelper.parse(stringFormatter.apply(idx, item));
    }
    return this;
  }

  /** Format list item as an Adventure {@link Component}. Receives (1-based index, item). */
  public ChatPager<T> componentFormatter(BiFunction<Integer, T, Component> componentFormatter) {
    this.formatter = componentFormatter;
    return this;
  }

  /**
   * Set custom command to run on page flip (e.g. "/warps %page%"). If not set, interactive
   * callbacks are used.
   */
  public ChatPager<T> pageCommand(String pageCommand) {
    this.pageCommand = pageCommand;
    return this;
  }

  /** Render and send the specified page (1-based). */
  public void send(int page) {
    if (player == null || !player.isOnline()) return;

    if (items.isEmpty()) {
      chatHelper.send(player, emptyMessage);
      return;
    }

    int totalItems = items.size();
    int maxPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
    int currentPage = Math.max(1, Math.min(page, maxPages));

    // 1. Send Header
    String renderedHeader =
        header
            .replace("%title%", title)
            .replace("%page%", String.valueOf(currentPage))
            .replace("%max%", String.valueOf(maxPages))
            .replace("%total%", String.valueOf(totalItems));
    chatHelper.send(player, renderedHeader);

    // 2. Send Items
    int startIndex = (currentPage - 1) * pageSize;
    int endIndex = Math.min(startIndex + pageSize, totalItems);

    for (int i = startIndex; i < endIndex; i++) {
      T item = items.get(i);
      int displayIndex = i + 1;

      Component lineComp;
      if (formatter != null) {
        lineComp = formatter.apply(displayIndex, item);
      } else {
        lineComp = chatHelper.parse("<yellow>#" + displayIndex + " <white>" + String.valueOf(item));
      }
      player.sendMessage(lineComp);
    }

    // 3. Send Interactive Footer with [◀ Previous] and [Next ▶] buttons
    ChatBuilder footer = chatHelper.builder();

    // Previous Button
    if (currentPage > 1) {
      int prevPage = currentPage - 1;
      if (pageCommand != null) {
        String cmd = pageCommand.replace("%page%", String.valueOf(prevPage));
        footer.clickRun(
            "<gold><bold>[◀ Previous]</bold></gold>", cmd, "<gray>Go to page " + prevPage);
      } else {
        footer.clickAction(
            "<gold><bold>[◀ Previous]</bold></gold>",
            p -> send(prevPage),
            "<gray>Go to page " + prevPage);
      }
    } else {
      footer.text("<dark_gray>[◀ Previous]</dark_gray>");
    }

    footer.space();
    footer.text("<gray>Page " + currentPage + "/" + maxPages + "</gray>");
    footer.space();

    // Next Button
    if (currentPage < maxPages) {
      int nextPage = currentPage + 1;
      if (pageCommand != null) {
        String cmd = pageCommand.replace("%page%", String.valueOf(nextPage));
        footer.clickRun("<gold><bold>[Next ▶]</bold></gold>", cmd, "<gray>Go to page " + nextPage);
      } else {
        footer.clickAction(
            "<gold><bold>[Next ▶]</bold></gold>",
            p -> send(nextPage),
            "<gray>Go to page " + nextPage);
      }
    } else {
      footer.text("<dark_gray>[Next ▶]</dark_gray>");
    }

    footer.send(player);
  }
}
