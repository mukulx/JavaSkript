package dev.mukulx.javaskript.api.event;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

/**
 * High-performance, functional event registration engine for JavaSkript.
 *
 * <p>Enables registering event listeners in a single line using modern lambdas, eliminating the
 * boilerplate of separate {@link Listener} classes and {@code @EventHandler} annotations.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>1-line lambda listeners: {@code events.on(PlayerJoinEvent.class, e -> ...)}
 *   <li>Run-once listeners: {@code events.once(PlayerInteractEvent.class, e -> ...)}
 *   <li>Predicate filtering: {@code events.on(BlockBreakEvent.class, e -> e.getBlock().getType() ==
 *       Material.DIAMOND_ORE, e -> ...)}
 *   <li>Auto-expiration via {@link Duration} or server ticks
 *   <li>Built-in execution count limits
 *   <li>Full Folia & Paper multi-threaded safety
 *   <li>Automatic cleanup when the script is unloaded or reloaded
 *   <li>Automatic integration with the JavaSkript performance profiler
 * </ul>
 */
public class EventHelper {

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private final List<EventSubscriptionImpl<?>> subscriptions = new CopyOnWriteArrayList<>();

  public EventHelper(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName = scriptName != null ? scriptName : "script";
  }

  // ==========================================
  // Primary Registration Methods
  // ==========================================

  /**
   * Listen to an event at normal priority.
   *
   * @param eventClass The Bukkit event class
   * @param handler The lambda handler consumer
   * @return Active {@link EventSubscription}
   */
  public <T extends Event> EventSubscription<T> on(Class<T> eventClass, Consumer<T> handler) {
    return on(eventClass, EventPriority.NORMAL, false, null, handler);
  }

  /**
   * Listen to an event with a specific Bukkit {@link EventPriority}.
   *
   * @param eventClass The Bukkit event class
   * @param priority Event priority
   * @param handler The lambda handler consumer
   * @return Active {@link EventSubscription}
   */
  public <T extends Event> EventSubscription<T> on(
      Class<T> eventClass, EventPriority priority, Consumer<T> handler) {
    return on(eventClass, priority, false, null, handler);
  }

  /**
   * Listen to an event only when a predicate filter condition passes.
   *
   * @param eventClass The Bukkit event class
   * @param filter Predicate condition
   * @param handler The lambda handler consumer
   * @return Active {@link EventSubscription}
   */
  public <T extends Event> EventSubscription<T> on(
      Class<T> eventClass, Predicate<T> filter, Consumer<T> handler) {
    return on(eventClass, EventPriority.NORMAL, false, filter, handler);
  }

  /**
   * Listen to an event with priority and a predicate filter condition.
   *
   * @param eventClass The Bukkit event class
   * @param priority Event priority
   * @param filter Predicate condition
   * @param handler The lambda handler consumer
   * @return Active {@link EventSubscription}
   */
  public <T extends Event> EventSubscription<T> on(
      Class<T> eventClass, EventPriority priority, Predicate<T> filter, Consumer<T> handler) {
    return on(eventClass, priority, false, filter, handler);
  }

  /**
   * Comprehensive event registration method.
   *
   * @param eventClass The Bukkit event class
   * @param priority Event priority
   * @param ignoreCancelled Whether to ignore cancelled events
   * @param filter Optional predicate filter (null for all)
   * @param handler The lambda handler consumer
   * @return Active {@link EventSubscription}
   */
  public <T extends Event> EventSubscription<T> on(
      Class<T> eventClass,
      EventPriority priority,
      boolean ignoreCancelled,
      Predicate<T> filter,
      Consumer<T> handler) {

    if (eventClass == null) {
      throw new IllegalArgumentException("eventClass cannot be null");
    }
    if (handler == null) {
      throw new IllegalArgumentException("handler cannot be null");
    }

    EventSubscriptionImpl<T> subscription =
        new EventSubscriptionImpl<>(eventClass, priority, ignoreCancelled, filter, handler);
    subscription.register();
    subscriptions.add(subscription);
    return subscription;
  }

  // ==========================================
  // Run-Once Listeners
  // ==========================================

  /**
   * Listen to an event exactly once, automatically unregistering afterwards.
   *
   * @param eventClass The Bukkit event class
   * @param handler The lambda handler consumer
   * @return Active {@link EventSubscription}
   */
  public <T extends Event> EventSubscription<T> once(Class<T> eventClass, Consumer<T> handler) {
    return on(eventClass, handler).maxExecutions(1);
  }

  /**
   * Listen to an event exactly once when a predicate filter condition is met.
   *
   * @param eventClass The Bukkit event class
   * @param filter Predicate condition
   * @param handler The lambda handler consumer
   * @return Active {@link EventSubscription}
   */
  public <T extends Event> EventSubscription<T> once(
      Class<T> eventClass, Predicate<T> filter, Consumer<T> handler) {
    return on(eventClass, filter, handler).maxExecutions(1);
  }

  /**
   * Listen to an event exactly once with priority and a predicate filter condition.
   *
   * @param eventClass The Bukkit event class
   * @param priority Event priority
   * @param filter Predicate condition
   * @param handler The lambda handler consumer
   * @return Active {@link EventSubscription}
   */
  public <T extends Event> EventSubscription<T> once(
      Class<T> eventClass, EventPriority priority, Predicate<T> filter, Consumer<T> handler) {
    return on(eventClass, priority, filter, handler).maxExecutions(1);
  }

  // ==========================================
  // Inter-Script Custom Events (Pub/Sub)
  // ==========================================

  /**
   * Broadcast a custom event across all scripts.
   *
   * @param eventName Name of the custom event (e.g. "quest_completed", "jackpot_won")
   * @param args Arguments to pass
   * @return The populated context
   */
  public CustomEventContext fire(String eventName, Object... args) {
    if (plugin != null && plugin.getEventBus() != null) {
      return plugin.getEventBus().fire(eventName, args);
    }
    return new CustomEventContext(eventName, args);
  }

  /**
   * Listen for a custom event broadcast by any script or external plugin. Automatically cleaned up
   * when this script unloads or reloads.
   *
   * @param eventName Name of the custom event
   * @param handler Consumer accepting CustomEventContext
   * @return BusSubscription handle
   */
  public ScriptEventBus.BusSubscription onCustom(
      String eventName, Consumer<CustomEventContext> handler) {
    if (plugin != null && plugin.getEventBus() != null) {
      return plugin.getEventBus().subscribe(scriptName, eventName, handler);
    }
    return null;
  }

  /**
   * Listen for a custom event once, then automatically unsubscribe.
   *
   * @param eventName Name of the custom event
   * @param handler Consumer accepting CustomEventContext
   * @return BusSubscription handle
   */
  public ScriptEventBus.BusSubscription onceCustom(
      String eventName, Consumer<CustomEventContext> handler) {
    if (plugin != null && plugin.getEventBus() != null) {
      return plugin.getEventBus().subscribeOnce(scriptName, eventName, handler);
    }
    return null;
  }

  // ==========================================
  // Lifecycle & Cleanup
  // ==========================================

  /** Unregister all active subscriptions registered by this helper. */
  public void unregisterAll() {
    for (EventSubscriptionImpl<?> sub : subscriptions) {
      sub.unsubscribe();
    }
    subscriptions.clear();
    if (plugin != null && plugin.getEventBus() != null) {
      plugin.getEventBus().unregisterAll(scriptName);
    }
  }

  /** Returns the number of currently active subscriptions. */
  public int getActiveSubscriptionCount() {
    return (int) subscriptions.stream().filter(EventSubscription::isSubscribed).count();
  }

  // ==========================================
  // Subscription Implementation
  // ==========================================

  private class EventSubscriptionImpl<T extends Event> implements EventSubscription<T>, Listener {

    private final Class<T> eventClass;
    private final EventPriority priority;
    private final boolean ignoreCancelled;
    private final List<Predicate<T>> filters = new ArrayList<>();
    private final Consumer<T> handler;

    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private int maxExecutions = -1;

    private Runnable onExpireCallback = null;
    private Consumer<Throwable> errorHandler = null;
    private Object scheduledTask = null;

    public EventSubscriptionImpl(
        Class<T> eventClass,
        EventPriority priority,
        boolean ignoreCancelled,
        Predicate<T> filter,
        Consumer<T> handler) {
      this.eventClass = eventClass;
      this.priority = priority != null ? priority : EventPriority.NORMAL;
      this.ignoreCancelled = ignoreCancelled;
      if (filter != null) {
        this.filters.add(filter);
      }
      this.handler = handler;
    }

    void register() {
      if (subscribed.compareAndSet(false, true)) {
        EventExecutor executor =
            (listener, event) -> {
              if (!subscribed.get()) {
                return;
              }
              if (eventClass.isInstance(event)) {
                T typed = eventClass.cast(event);
                handleEvent(typed);
              }
            };

        Bukkit.getPluginManager()
            .registerEvent(eventClass, this, priority, executor, plugin, ignoreCancelled);
      }
    }

    private void handleEvent(T event) {
      if (!subscribed.get()) return;

      // Evaluate filters
      for (Predicate<T> f : filters) {
        try {
          if (!f.test(event)) {
            return;
          }
        } catch (Throwable t) {
          if (errorHandler != null) {
            errorHandler.accept(t);
          } else {
            plugin
                .getLogger()
                .warning(
                    "["
                        + scriptName
                        + "] Filter error on "
                        + eventClass.getSimpleName()
                        + ": "
                        + t.getMessage());
          }
          return;
        }
      }

      // Check max executions before increment
      if (maxExecutions > 0 && executionCount.get() >= maxExecutions) {
        unsubscribe();
        return;
      }

      long start = System.nanoTime();
      try {
        handler.accept(event);
      } catch (Throwable t) {
        if (errorHandler != null) {
          errorHandler.accept(t);
        } else {
          dev.mukulx.javaskript.util.ScriptErrorFormatter.log(
              plugin, scriptName, "event handler " + eventClass.getSimpleName(), t);
        }
      } finally {
        long duration = System.nanoTime() - start;
        if (plugin.getProfiler() != null) {
          plugin.getProfiler().record(scriptName, "EVENT", eventClass.getSimpleName(), duration);
        }
      }

      int count = executionCount.incrementAndGet();
      if (maxExecutions > 0 && count >= maxExecutions) {
        unsubscribe();
      }
    }

    @Override
    public void unsubscribe() {
      if (subscribed.compareAndSet(true, false)) {
        HandlerList.unregisterAll(this);
        subscriptions.remove(this);
        cancelScheduledTask();
        if (onExpireCallback != null) {
          try {
            onExpireCallback.run();
          } catch (Throwable ignored) {
          }
        }
      }
    }

    @Override
    public boolean isSubscribed() {
      return subscribed.get();
    }

    @Override
    public int getExecutionCount() {
      return executionCount.get();
    }

    @Override
    public EventSubscription<T> filter(Predicate<T> filter) {
      if (filter != null) {
        filters.add(filter);
      }
      return this;
    }

    @Override
    public EventSubscription<T> maxExecutions(int max) {
      this.maxExecutions = max;
      if (max > 0 && executionCount.get() >= max) {
        unsubscribe();
      }
      return this;
    }

    @Override
    public EventSubscription<T> expireAfter(Duration duration) {
      if (duration != null && !duration.isZero() && !duration.isNegative()) {
        long ticks = Math.max(1L, duration.toMillis() / 50L);
        return expireAfter(ticks);
      }
      return this;
    }

    @Override
    public EventSubscription<T> expireAfter(long ticks) {
      cancelScheduledTask();
      if (ticks <= 0) {
        unsubscribe();
        return this;
      }

      try {
        this.scheduledTask = ServerUtil.runLaterSync(plugin, this::unsubscribe, ticks);
      } catch (Throwable ignored) {
        this.scheduledTask = null;
      }
      return this;
    }

    @Override
    public EventSubscription<T> onExpire(Runnable callback) {
      this.onExpireCallback = callback;
      return this;
    }

    @Override
    public EventSubscription<T> onError(Consumer<Throwable> errorHandler) {
      this.errorHandler = errorHandler;
      return this;
    }

    private void cancelScheduledTask() {
      if (scheduledTask != null) {
        if (scheduledTask instanceof org.bukkit.scheduler.BukkitTask bt) {
          bt.cancel();
        } else {
          try {
            ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) scheduledTask).cancel();
          } catch (Throwable ignored) {
          }
        }
        scheduledTask = null;
      }
    }
  }
}
