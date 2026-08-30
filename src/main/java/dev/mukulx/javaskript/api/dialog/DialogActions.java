package dev.mukulx.javaskript.api.dialog;

import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import java.time.Duration;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

/** Static factories for creating DialogActions with zero boilerplate. */
public final class DialogActions {

  private DialogActions() {}

  /** Runs a command template such as {@code "give $(player) diamond"}. */
  public static DialogAction commandTemplate(String template) {
    return DialogAction.commandTemplate(template);
  }

  /** Performs a static adventure {@link ClickEvent}. */
  public static DialogAction staticClick(ClickEvent clickEvent) {
    return DialogAction.staticAction(clickEvent);
  }

  public static DialogAction openUrl(String url) {
    return DialogAction.staticAction(ClickEvent.openUrl(url));
  }

  public static DialogAction runCommand(String command) {
    return DialogAction.staticAction(ClickEvent.runCommand(command));
  }

  public static DialogAction suggestCommand(String command) {
    return DialogAction.staticAction(ClickEvent.suggestCommand(command));
  }

  public static DialogAction copyToClipboard(String text) {
    return DialogAction.staticAction(ClickEvent.copyToClipboard(text));
  }

  /** Runs a server-side callback once. */
  public static DialogAction custom(DialogActionCallback callback) {
    return DialogAction.customClick(callback, defaultOptions(1));
  }

  /** Runs a server-side callback with a Player-friendly lambda. */
  public static DialogAction playerCallback(PlayerCallback callback) {
    return custom(
        (view, audience) -> {
          if (audience instanceof Player player && callback != null) {
            callback.accept(view, player);
          }
        });
  }

  /** Runs a server-side callback with a simple Consumer<Player>. */
  public static DialogAction playerAction(java.util.function.Consumer<Player> action) {
    return custom(
        (view, audience) -> {
          if (audience instanceof Player player && action != null) {
            action.accept(player);
          }
        });
  }

  /** Runs a server-side callback usable {@code uses} times. */
  public static DialogAction custom(DialogActionCallback callback, int uses) {
    return DialogAction.customClick(callback, defaultOptions(uses));
  }

  /** Runs a server-side callback with a custom lifetime. */
  public static DialogAction custom(DialogActionCallback callback, int uses, Duration lifetime) {
    return DialogAction.customClick(
        callback, ClickCallback.Options.builder().uses(uses).lifetime(lifetime).build());
  }

  /** Custom click identified by {@code Key}. */
  public static DialogAction identifier(Key id) {
    return DialogAction.customClick(id, null);
  }

  /** Custom click identified by {@code Key} with extra NBT. */
  public static DialogAction identifier(Key id, BinaryTagHolder additions) {
    return DialogAction.customClick(id, additions);
  }

  private static ClickCallback.Options defaultOptions(int uses) {
    return ClickCallback.Options.builder()
        .uses(uses)
        .lifetime(ClickCallback.DEFAULT_LIFETIME)
        .build();
  }

  @FunctionalInterface
  public interface PlayerCallback {
    void accept(io.papermc.paper.dialog.DialogResponseView response, Player player);
  }
}
