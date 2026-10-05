package dev.mukulx.javaskript.api.chat;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * Universal, high-performance Chat API for JavaSkript.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>Interactive {@link ChatBuilder} for clickable text and hover tooltips
 *   <li>Interactive {@link ChatConfirmation} for Yes/No and Confirm/Cancel workflows
 *   <li>Private chat prompts and typed number validation ({@link #prompt})
 *   <li>Paginated interactive lists with next/previous buttons ({@link #pager})
 *   <li>Pixel-perfect centered text banners ({@link #sendCentered})
 *   <li>Automatic token cleanup and Folia-safe execution
 * </ul>
 */
public class ChatHelper implements Listener {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY_AMPERSAND =
      LegacyComponentSerializer.builder().character('&').hexColors().build();
  private static final PlainTextComponentSerializer PLAIN_SERIALIZER =
      PlainTextComponentSerializer.plainText();

  // Global action token registry: token -> ActionEntry
  private static final Map<String, ActionEntry> GLOBAL_ACTIONS = new ConcurrentHashMap<>();

  private static final java.util.regex.Pattern STRIP_AMP =
      java.util.regex.Pattern.compile("&[0-9a-fk-orA-FK-OR]");
  private static final java.util.regex.Pattern STRIP_SECTION =
      java.util.regex.Pattern.compile("§[0-9a-fk-orA-FK-OR]");
  private static final java.util.regex.Pattern STRIP_TAGS =
      java.util.regex.Pattern.compile("<[^>]*>");

  private final JavaSkriptPlugin plugin;
  private final String scriptKey;

  // Active chat prompts: player UUID -> PromptContext
  private final Map<UUID, PromptContext> activePrompts = new ConcurrentHashMap<>();
  private boolean listenerRegistered = false;

  public ChatHelper(JavaSkriptPlugin plugin, String scriptKey) {
    this.plugin = plugin;
    this.scriptKey = scriptKey != null ? scriptKey : "api";
  }

  public JavaSkriptPlugin getPlugin() {
    return plugin;
  }

  // ==========================================
  // Core Builders & Workflows
  // ==========================================

  /** Create a new fluent {@link ChatBuilder} for interactive components. */
  public ChatBuilder builder() {
    return new ChatBuilder(this);
  }

  /** Create an interactive Yes/No or Confirm/Cancel workflow for a player. */
  public ChatConfirmation confirm(Player player) {
    return new ChatConfirmation(this, player);
  }

  /** Create a paginated list view with clickable navigation buttons. */
  public <T> ChatPager<T> pager(Player player, String title, List<T> items) {
    return new ChatPager<>(this, player, title, items);
  }

  // ==========================================
  // Private Chat Input Prompts
  // ==========================================

  /**
   * Prompt a player for chat input. Their next chat message will be hidden from everyone else and
   * delivered directly to the callback.
   *
   * @param player The player to prompt
   * @param message Prompt question displayed to the player
   * @param onInput Consumer receiving (Player, rawInputText)
   */
  public void prompt(Player player, String message, BiConsumer<Player, String> onInput) {
    prompt(player, message, Duration.ofSeconds(60), onInput, null);
  }

  /**
   * Prompt a player for chat input with custom timeout and cancel handler.
   *
   * @param player The player to prompt
   * @param message Prompt question displayed to the player
   * @param timeout Maximum wait duration before expiring
   * @param onInput Consumer receiving (Player, rawInputText)
   * @param onCancel Optional callback if player types 'cancel' or times out
   */
  public void prompt(
      Player player,
      String message,
      Duration timeout,
      BiConsumer<Player, String> onInput,
      Consumer<Player> onCancel) {

    if (player == null || !player.isOnline()) return;

    ensureListenerRegistered();
    send(player, message);

    UUID uuid = player.getUniqueId();
    PromptContext ctx = new PromptContext(onInput, onCancel);
    activePrompts.put(uuid, ctx);

    // Schedule timeout
    if (timeout != null && !timeout.isZero() && !timeout.isNegative()) {
      long delayTicks = Math.max(1L, timeout.toMillis() / 50L);
      Runnable timeoutTask =
          () -> {
            PromptContext removed = activePrompts.remove(uuid);
            if (removed != null && player.isOnline()) {
              send(player, "<gray>[Prompt expired due to inactivity.]</gray>");
              if (removed.onCancel != null) {
                removed.onCancel.accept(player);
              }
            }
          };

      ServerUtil.runLaterSync(plugin, timeoutTask, delayTicks);
    }
  }

  /**
   * Prompt a player for an integer with automatic bounds validation. Re-prompts on invalid input.
   */
  public void promptInteger(
      Player player, String message, int min, int max, BiConsumer<Player, Integer> onInput) {
    prompt(
        player,
        message,
        (p, text) -> {
          try {
            int val = Integer.parseInt(text.trim());
            if (val < min || val > max) {
              send(
                  p,
                  "<red>Value must be between "
                      + min
                      + " and "
                      + max
                      + ". Try again (or 'cancel'):</red>");
              promptInteger(p, message, min, max, onInput);
              return;
            }
            onInput.accept(p, val);
          } catch (NumberFormatException e) {
            send(p, "<red>Invalid number. Try again (or 'cancel'):</red>");
            promptInteger(p, message, min, max, onInput);
          }
        });
  }

  /** Prompt a player for a double with automatic bounds validation. Re-prompts on invalid input. */
  public void promptDouble(
      Player player, String message, double min, double max, BiConsumer<Player, Double> onInput) {
    prompt(
        player,
        message,
        (p, text) -> {
          try {
            double val = Double.parseDouble(text.trim());
            if (!Double.isFinite(val) || val < min || val > max) {
              send(
                  p,
                  "<red>Value must be between "
                      + min
                      + " and "
                      + max
                      + ". Try again (or 'cancel'):</red>");
              promptDouble(p, message, min, max, onInput);
              return;
            }
            onInput.accept(p, val);
          } catch (NumberFormatException e) {
            send(p, "<red>Invalid decimal number. Try again (or 'cancel'):</red>");
            promptDouble(p, message, min, max, onInput);
          }
        });
  }

  // ==========================================
  // Centered Chat Text
  // ==========================================

  /** Send pixel-perfect centered text to a command sender or player. */
  public void sendCentered(CommandSender sender, String message) {
    if (sender == null || message == null) return;
    sender.sendMessage(parse(center(message)));
  }

  /** Calculate and prepend spaces to center a chat line based on Minecraft character widths. */
  public String center(String message) {
    if (message == null || message.isEmpty()) return "";

    // Strip formatting tags using precompiled patterns
    String stripped =
        STRIP_TAGS
            .matcher(
                STRIP_SECTION.matcher(STRIP_AMP.matcher(message).replaceAll("")).replaceAll(""))
            .replaceAll("");

    int messagePxSize = 0;
    boolean isBold = message.contains("<bold>") || message.contains("&l") || message.contains("§l");

    for (char c : stripped.toCharArray()) {
      DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
      messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
    }

    int halvedMessageSize = messagePxSize / 2;
    int toCompensate = DefaultFontInfo.CENTER_PX - halvedMessageSize;
    int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
    int compensated = 0;
    StringBuilder sb = new StringBuilder();

    while (compensated < toCompensate) {
      sb.append(" ");
      compensated += spaceLength;
    }

    return sb.toString() + message;
  }

  // ==========================================
  // Chat Cleaning & Utilities
  // ==========================================

  /** Clear the chat box for a specific player by printing blank lines. */
  public void clearChat(Player player) {
    if (player != null && player.isOnline()) {
      for (int i = 0; i < 100; i++) {
        player.sendMessage(Component.empty());
      }
    }
  }

  /** Clear the chat box for all online players. */
  public void clearChatAll() {
    for (Player p : Bukkit.getOnlinePlayers()) {
      clearChat(p);
    }
  }

  /** Send formatted message (MiniMessage & legacy & supported). */
  public void send(CommandSender sender, String message) {
    if (sender != null && message != null) {
      sender.sendMessage(parse(message));
    }
  }

  /** Broadcast formatted message. */
  public void broadcast(String message) {
    if (message != null) {
      Bukkit.broadcast(parse(message));
    }
  }

  /** Parse text into Adventure Component with default italics disabled. */
  public Component parse(String input) {
    if (input == null || input.isEmpty()) return Component.empty();

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

  // ==========================================
  // Action Token Management
  // ==========================================

  public String registerAction(Consumer<Player> action, boolean singleUse) {
    String token = UUID.randomUUID().toString().substring(0, 8);
    GLOBAL_ACTIONS.put(token, new ActionEntry(action, singleUse, scriptKey));
    return token;
  }

  public void invalidateAction(String token) {
    if (token != null) {
      GLOBAL_ACTIONS.remove(token);
    }
  }

  public static boolean executeGlobalAction(Player player, String token) {
    if (token == null || player == null) return false;
    ActionEntry entry = GLOBAL_ACTIONS.get(token);
    if (entry != null) {
      if (entry.singleUse) {
        GLOBAL_ACTIONS.remove(token);
      }
      try {
        entry.action.accept(player);
        return true;
      } catch (Throwable t) {
        Bukkit.getLogger().warning("Error running chat action: " + t.getMessage());
      }
    }
    return false;
  }

  // ==========================================
  // Prompt Interception Listener
  // ==========================================

  private synchronized void ensureListenerRegistered() {
    if (!listenerRegistered && plugin != null) {
      Bukkit.getPluginManager().registerEvents(this, plugin);
      listenerRegistered = true;
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onAsyncChat(AsyncChatEvent event) {
    Player player = event.getPlayer();
    PromptContext ctx = activePrompts.remove(player.getUniqueId());
    if (ctx != null) {
      event.setCancelled(true);
      String text = PLAIN_SERIALIZER.serialize(event.message());
      dispatchPromptResult(player, text, ctx);
    }
  }

  private void dispatchPromptResult(Player player, String text, PromptContext ctx) {
    if (player == null || ctx == null) return;
    final String finalText = (text == null) ? "" : text;

    if (finalText.equalsIgnoreCase("cancel")) {
      send(player, "<gray>[Action canceled.]</gray>");
      if (ctx.onCancel != null) {
        ctx.onCancel.accept(player);
      }
      return;
    }

    // Run callback on server/regional scheduler thread safely
    Runnable action =
        () -> {
          try {
            ctx.onInput.accept(player, finalText);
          } catch (Throwable t) {
            plugin.getLogger().severe("Error in chat prompt callback: " + t.getMessage());
          }
        };

    ServerUtil.runForPlayer(plugin, player, action);
  }

  // ==========================================
  // Lifecycle & Cleanup
  // ==========================================

  public void cleanup() {
    activePrompts.clear();
    // Remove actions registered by this script
    GLOBAL_ACTIONS.entrySet().removeIf(e -> e.getValue().scriptKey.equals(this.scriptKey));
    if (listenerRegistered) {
      HandlerList.unregisterAll(this);
      listenerRegistered = false;
    }
  }

  private static class ActionEntry {
    final Consumer<Player> action;
    final boolean singleUse;
    final String scriptKey;

    ActionEntry(Consumer<Player> action, boolean singleUse, String scriptKey) {
      this.action = action;
      this.singleUse = singleUse;
      this.scriptKey = scriptKey;
    }
  }

  private static class PromptContext {
    final BiConsumer<Player, String> onInput;
    final Consumer<Player> onCancel;

    PromptContext(BiConsumer<Player, String> onInput, Consumer<Player> onCancel) {
      this.onInput = onInput;
      this.onCancel = onCancel;
    }
  }
}
