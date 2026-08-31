package dev.mukulx.javaskript.api.event;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Thread-safe inter-script Pub/Sub Event Bus. Enables scripts to broadcast custom events and listen
 * to other scripts without class dependencies.
 */
public class ScriptEventBus {

  public static class BusSubscription {
    private final String scriptKey;
    private final String eventName;
    private final Consumer<CustomEventContext> handler;
    private final boolean once;

    public BusSubscription(
        String scriptKey, String eventName, Consumer<CustomEventContext> handler, boolean once) {
      this.scriptKey = scriptKey;
      this.eventName = eventName;
      this.handler = handler;
      this.once = once;
    }

    public String getScriptKey() {
      return scriptKey;
    }

    public String getEventName() {
      return eventName;
    }

    public Consumer<CustomEventContext> getHandler() {
      return handler;
    }

    public boolean isOnce() {
      return once;
    }
  }

  private final JavaSkriptPlugin plugin;
  // eventName (lowercase) -> List of subscriptions
  private final Map<String, List<BusSubscription>> listeners = new ConcurrentHashMap<>();

  public ScriptEventBus(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Broadcast a custom event across all scripts.
   *
   * @param eventName Name of the event (e.g. "quest_completed", "jackpot_won")
   * @param args Arguments to pass
   * @return The populated context
   */
  public CustomEventContext fire(String eventName, Object... args) {
    if (eventName == null || eventName.isEmpty()) {
      return new CustomEventContext("", args);
    }
    String key = eventName.toLowerCase();
    CustomEventContext ctx = new CustomEventContext(eventName, args);

    List<BusSubscription> subs = listeners.get(key);
    if (subs == null || subs.isEmpty()) {
      return ctx;
    }

    List<BusSubscription> toRemove = null;
    for (BusSubscription sub : subs) {
      if (ctx.isCancelled()) {
        break;
      }
      try {
        sub.getHandler().accept(ctx);
      } catch (Throwable t) {
        plugin
            .getLogger()
            .severe(
                "[EventBus] Error executing listener in "
                    + sub.getScriptKey()
                    + " for event '"
                    + eventName
                    + "': "
                    + t.getMessage());
      }
      if (sub.isOnce()) {
        if (toRemove == null) {
          toRemove = new ArrayList<>();
        }
        toRemove.add(sub);
      }
    }

    if (toRemove != null) {
      subs.removeAll(toRemove);
    }

    return ctx;
  }

  /**
   * Register a listener for a custom event.
   *
   * @param scriptKey The script registering this listener
   * @param eventName Name of the event
   * @param handler Consumer handling the context
   * @return The created subscription
   */
  public BusSubscription subscribe(
      String scriptKey, String eventName, Consumer<CustomEventContext> handler) {
    return subscribe(scriptKey, eventName, handler, false);
  }

  /**
   * Register a one-time listener for a custom event.
   *
   * @param scriptKey The script registering this listener
   * @param eventName Name of the event
   * @param handler Consumer handling the context
   * @return The created subscription
   */
  public BusSubscription subscribeOnce(
      String scriptKey, String eventName, Consumer<CustomEventContext> handler) {
    return subscribe(scriptKey, eventName, handler, true);
  }

  private BusSubscription subscribe(
      String scriptKey, String eventName, Consumer<CustomEventContext> handler, boolean once) {
    if (eventName == null || handler == null) {
      return null;
    }
    String key = eventName.toLowerCase();
    BusSubscription sub =
        new BusSubscription(scriptKey != null ? scriptKey : "unknown", eventName, handler, once);
    listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(sub);
    return sub;
  }

  /**
   * Unsubscribe a specific subscription.
   *
   * @param sub The subscription
   */
  public void unsubscribe(BusSubscription sub) {
    if (sub == null) {
      return;
    }
    String key = sub.getEventName().toLowerCase();
    List<BusSubscription> subs = listeners.get(key);
    if (subs != null) {
      subs.remove(sub);
    }
  }

  /**
   * Unregister all custom listeners registered by a specific script. Called automatically during
   * script unload/reload.
   *
   * @param scriptKey The script key
   */
  public void unregisterAll(String scriptKey) {
    if (scriptKey == null) {
      return;
    }
    for (List<BusSubscription> subs : listeners.values()) {
      subs.removeIf(sub -> scriptKey.equalsIgnoreCase(sub.getScriptKey()));
    }
  }

  /** Clear all listeners. */
  public void clear() {
    listeners.clear();
  }
}
