package dev.mukulx.javaskript.api.event;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.event.Event;

/**
 * Represents an active event subscription registered through {@link EventHelper}.
 *
 * <p>Provides fluent controls to filter, set execution limits, auto-expire, or cancel the
 * subscription at any time.
 *
 * @param <T> Event type
 */
public interface EventSubscription<T extends Event> {

  /** Unregister this event listener immediately. */
  void unsubscribe();

  /** Alias for {@link #unsubscribe()}. */
  default void cancel() {
    unsubscribe();
  }

  /** Whether this subscription is currently registered and listening. */
  boolean isSubscribed();

  /** Returns how many times this subscription has executed. */
  int getExecutionCount();

  /** Add an additional predicate filter that must pass before the handler is invoked. */
  EventSubscription<T> filter(Predicate<T> filter);

  /**
   * Automatically unsubscribe after this listener has successfully handled the event N times.
   *
   * @param maxExecutions Maximum executions allowed
   */
  EventSubscription<T> maxExecutions(int maxExecutions);

  /**
   * Automatically unsubscribe after the specified real-world duration has elapsed.
   *
   * @param duration Real-world duration
   */
  EventSubscription<T> expireAfter(Duration duration);

  /**
   * Automatically unsubscribe after the specified server ticks have passed.
   *
   * @param ticks Minecraft server ticks
   */
  EventSubscription<T> expireAfter(long ticks);

  /** Callback invoked when the subscription is cancelled or expires. */
  EventSubscription<T> onExpire(Runnable callback);

  /** Callback invoked if the event consumer throws an exception. */
  EventSubscription<T> onError(Consumer<Throwable> errorHandler);
}
