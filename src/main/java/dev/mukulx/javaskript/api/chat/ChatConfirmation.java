package dev.mukulx.javaskript.api.chat;

import dev.mukulx.javaskript.util.ServerUtil;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Interactive Yes/No and Confirm/Cancel workflow builder for chat.
 *
 * <p>Sends a formatted question with clickable confirmation buttons. Automatically enforces
 * single-use token invalidation and handles timeouts.
 */
public class ChatConfirmation {

  private final ChatHelper chatHelper;
  private final Player player;

  private String questionText = "<gold>Are you sure you want to proceed?";
  private String acceptText = "<green><bold>[CONFIRM]</bold></green>";
  private String acceptHover = "<gray>Click to confirm action";
  private Consumer<Player> onAccept;

  private String denyText = "<red><bold>[CANCEL]</bold></red>";
  private String denyHover = "<gray>Click to cancel action";
  private Consumer<Player> onDeny;

  private Duration timeout = Duration.ofSeconds(30);
  private Consumer<Player> onTimeout;

  public ChatConfirmation(ChatHelper chatHelper, Player player) {
    this.chatHelper = chatHelper;
    this.player = player;
  }

  /** Set the question text displayed above the buttons. */
  public ChatConfirmation question(String question) {
    this.questionText = question;
    return this;
  }

  /** Configure accept button text, callback, and hover tooltip. */
  public ChatConfirmation accept(String buttonText, Consumer<Player> onAccept, String hover) {
    this.acceptText = buttonText;
    this.onAccept = onAccept;
    if (hover != null) this.acceptHover = hover;
    return this;
  }

  /** Configure accept button text and callback. */
  public ChatConfirmation accept(String buttonText, Consumer<Player> onAccept) {
    return accept(buttonText, onAccept, null);
  }

  /** Configure accept button callback with default [CONFIRM] text. */
  public ChatConfirmation accept(Consumer<Player> onAccept) {
    return accept(acceptText, onAccept, null);
  }

  /** Configure deny button text, callback, and hover tooltip. */
  public ChatConfirmation deny(String buttonText, Consumer<Player> onDeny, String hover) {
    this.denyText = buttonText;
    this.onDeny = onDeny;
    if (hover != null) this.denyHover = hover;
    return this;
  }

  /** Configure deny button text and callback. */
  public ChatConfirmation deny(String buttonText, Consumer<Player> onDeny) {
    return deny(buttonText, onDeny, null);
  }

  /** Configure deny button callback with default [CANCEL] text. */
  public ChatConfirmation deny(Consumer<Player> onDeny) {
    return deny(denyText, onDeny, null);
  }

  /** Set confirmation timeout (defaults to 30 seconds). */
  public ChatConfirmation timeout(Duration duration) {
    this.timeout = duration;
    return this;
  }

  /** Callback invoked if the player fails to click either button before the timeout expires. */
  public ChatConfirmation onTimeout(Consumer<Player> onTimeout) {
    this.onTimeout = onTimeout;
    return this;
  }

  /** Assemble and send the interactive confirmation prompt to the player. */
  public void send() {
    if (player == null || !player.isOnline()) return;

    AtomicBoolean resolved = new AtomicBoolean(false);

    // Register accept action token
    String acceptToken =
        chatHelper.registerAction(
            p -> {
              if (resolved.compareAndSet(false, true)) {
                if (onAccept != null) {
                  onAccept.accept(p);
                }
              }
            },
            true);

    // Register deny action token
    String denyToken =
        chatHelper.registerAction(
            p -> {
              if (resolved.compareAndSet(false, true)) {
                if (onDeny != null) {
                  onDeny.accept(p);
                }
              }
            },
            true);

    // Build interactive component
    chatHelper
        .builder()
        .text(questionText)
        .space()
        .clickRun(acceptText, "/__jsk_action " + acceptToken, acceptHover)
        .space()
        .clickRun(denyText, "/__jsk_action " + denyToken, denyHover)
        .send(player);

    // Schedule timeout
    if (timeout != null && !timeout.isZero() && !timeout.isNegative()) {
      long delayTicks = Math.max(1L, timeout.toMillis() / 50L);
      Runnable timeoutTask =
          () -> {
            if (resolved.compareAndSet(false, true)) {
              chatHelper.invalidateAction(acceptToken);
              chatHelper.invalidateAction(denyToken);
              if (onTimeout != null && player.isOnline()) {
                onTimeout.accept(player);
              }
            }
          };

      if (ServerUtil.isFolia()) {
        try {
          Bukkit.getGlobalRegionScheduler()
              .runDelayed(chatHelper.getPlugin(), task -> timeoutTask.run(), delayTicks);
        } catch (Throwable ignored) {
        }
      } else {
        Bukkit.getScheduler().runTaskLater(chatHelper.getPlugin(), timeoutTask, delayTicks);
      }
    }
  }
}
