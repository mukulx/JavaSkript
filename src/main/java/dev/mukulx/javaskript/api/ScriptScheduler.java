package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

/**
 * Easy-to-use scheduler API for scripts. Automatically detects Paper vs Folia and uses appropriate
 * scheduling. Automatically tracks and cancels tasks when script unloads.
 *
 * <p>Fully compatible with both Paper and Folia!
 */
public class ScriptScheduler {

  private final JavaSkriptPlugin plugin;
  private static final int PRUNE_THRESHOLD = 128;

  private final List<Object> tasks; // BukkitTask or Folia ScheduledTask
  private volatile int pruneAt = PRUNE_THRESHOLD;
  private final boolean isFolia;
  private final String scriptKey;

  public ScriptScheduler(JavaSkriptPlugin plugin) {
    this(plugin, "unknown");
  }

  public ScriptScheduler(JavaSkriptPlugin plugin, String scriptKey) {
    this.plugin = plugin;
    this.scriptKey = scriptKey != null ? scriptKey : "unknown";
    this.tasks = new CopyOnWriteArrayList<>();
    this.isFolia = ServerUtil.isFolia();
  }

  /**
   * Remember a task so unload can cancel it. One-shot tasks never remove themselves, so drop
   * finished ones whenever the list has grown well past what the last prune left behind.
   */
  private void track(Object task) {
    if (task == null) return;
    tasks.add(task);
    if (tasks.size() >= pruneAt) {
      tasks.removeIf(this::isFinished);
      pruneAt = Math.max(PRUNE_THRESHOLD, tasks.size() * 2);
    }
  }

  private boolean isFinished(Object task) {
    if (task instanceof BukkitTask bukkitTask) {
      if (bukkitTask.isCancelled()) return true;
      int id = bukkitTask.getTaskId();
      return !Bukkit.getScheduler().isQueued(id) && !Bukkit.getScheduler().isCurrentlyRunning(id);
    }
    if (task instanceof ScheduledTask foliaTask) {
      ScheduledTask.ExecutionState state = foliaTask.getExecutionState();
      return state == ScheduledTask.ExecutionState.FINISHED
          || state == ScheduledTask.ExecutionState.CANCELLED;
    }
    return false;
  }

  private Runnable wrap(String type, Runnable runnable) {
    if (runnable == null) return null;
    return () -> {
      long t0 = System.nanoTime();
      try {
        runnable.run();
      } catch (Throwable t) {
        dev.mukulx.javaskript.util.ScriptErrorFormatter.log(
            plugin, scriptKey, "scheduled " + type + " task", t);
      } finally {
        long elapsed = System.nanoTime() - t0;
        plugin.getProfiler().record(scriptKey, "TASK", type, elapsed);
      }
    };
  }

  /**
   * Run a task after a delay on the global region (Folia) or main thread (Paper).
   *
   * @param runnable The task to run
   * @param delayTicks Delay in ticks (20 ticks = 1 second)
   * @return The scheduled task
   */
  public Object runLater(Runnable runnable, long delayTicks) {
    Runnable profiled = wrap("runLater", runnable);
    if (isFolia) {
      return runLaterFolia(profiled, delayTicks);
    } else {
      BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, profiled, delayTicks);
      track(task);
      return task;
    }
  }

  /** Run a task after a delay (ticks, runnable order). */
  public Object runLater(long delayTicks, Runnable runnable) {
    return runLater(runnable, delayTicks);
  }

  /**
   * Run a task repeatedly on the global region (Folia) or main thread (Paper).
   *
   * @param runnable The task to run
   * @param delayTicks Initial delay in ticks
   * @param periodTicks Period between runs in ticks
   * @return The scheduled task
   */
  public Object runTimer(Runnable runnable, long delayTicks, long periodTicks) {
    Runnable profiled = wrap("runTimer", runnable);
    if (isFolia) {
      return runTimerFolia(profiled, delayTicks, periodTicks);
    } else {
      BukkitTask task =
          Bukkit.getScheduler().runTaskTimer(plugin, profiled, delayTicks, periodTicks);
      track(task);
      return task;
    }
  }

  /**
   * Run a task immediately on the global region (Folia) or main thread (Paper).
   *
   * @param runnable The task to run
   * @return The scheduled task
   */
  public Object run(Runnable runnable) {
    Runnable profiled = wrap("run", runnable);
    if (isFolia) {
      return runLaterFolia(profiled, 1L);
    } else {
      BukkitTask task = Bukkit.getScheduler().runTask(plugin, profiled);
      track(task);
      return task;
    }
  }

  /**
   * Run a task asynchronously (works on both Paper and Folia).
   *
   * @param runnable The task to run
   * @return The scheduled task
   */
  public Object runAsync(Runnable runnable) {
    Runnable profiled = wrap("runAsync", runnable);
    if (isFolia) {
      return runAsyncFolia(profiled);
    } else {
      BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, profiled);
      track(task);
      return task;
    }
  }

  /**
   * Run a task asynchronously after a delay (works on both Paper and Folia).
   *
   * @param runnable The task to run
   * @param delayTicks Delay in ticks
   * @return The scheduled task
   */
  public Object runLaterAsync(Runnable runnable, long delayTicks) {
    Runnable profiled = wrap("runLaterAsync", runnable);
    if (isFolia) {
      return runLaterAsyncFolia(profiled, delayTicks);
    } else {
      BukkitTask task =
          Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, profiled, delayTicks);
      track(task);
      return task;
    }
  }

  /**
   * Run a task repeatedly asynchronously (works on both Paper and Folia).
   *
   * @param runnable The task to run
   * @param delayTicks Initial delay in ticks
   * @param periodTicks Period between runs in ticks
   * @return The scheduled task
   */
  public Object runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
    Runnable profiled = wrap("runTimerAsync", runnable);
    if (isFolia) {
      return runTimerAsyncFolia(profiled, delayTicks, periodTicks);
    } else {
      BukkitTask task =
          Bukkit.getScheduler()
              .runTaskTimerAsynchronously(plugin, profiled, delayTicks, periodTicks);
      track(task);
      return task;
    }
  }

  /**
   * Run a task on an entity's region (Folia-specific, falls back to main thread on Paper). Use this
   * when you need to interact with a specific entity.
   *
   * @param entity The entity whose region to run on
   * @param runnable The task to run
   * @return The scheduled task
   */
  public Object runAtEntity(Entity entity, Runnable runnable) {
    if (isFolia) {
      return runAtEntityFolia(entity, runnable);
    } else {
      return run(runnable);
    }
  }

  /**
   * Run a task on an entity's region after a delay.
   *
   * @param entity The entity whose region to run on
   * @param runnable The task to run
   * @param delayTicks Delay in ticks
   * @return The scheduled task
   */
  public Object runAtEntityLater(Entity entity, Runnable runnable, long delayTicks) {
    if (isFolia) {
      return runAtEntityLaterFolia(entity, runnable, delayTicks);
    } else {
      return runLater(runnable, delayTicks);
    }
  }

  /**
   * Run a task and return a CompletableFuture.
   *
   * @param runnable The task to run
   * @return CompletableFuture that completes when task finishes
   */
  public CompletableFuture<Void> runAsyncFuture(Runnable runnable) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Object task =
        runAsync(
            () -> {
              try {
                runnable.run();
                future.complete(null);
              } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
                throw throwable;
              }
            });
    if (task == null) {
      future.completeExceptionally(new IllegalStateException("Unable to schedule async task"));
    }
    return future;
  }

  /**
   * Run every second.
   *
   * @param runnable The task to run
   * @return The scheduled task
   */
  public Object everySecond(Runnable runnable) {
    return runTimer(runnable, 20L, 20L);
  }

  public TaskHandle everySecondHandle(Runnable runnable) {
    return handle(everySecond(runnable));
  }

  public TaskHandle runHandle(Runnable runnable) {
    return handle(run(runnable));
  }

  public TaskHandle runLaterHandle(Runnable runnable, long delayTicks) {
    return handle(runLater(runnable, delayTicks));
  }

  public TaskHandle runTimerHandle(Runnable runnable, long delayTicks, long periodTicks) {
    return handle(runTimer(runnable, delayTicks, periodTicks));
  }

  /**
   * Run every minute.
   *
   * @param runnable The task to run
   * @return The scheduled task
   */
  public Object everyMinute(Runnable runnable) {
    return runTimer(runnable, 1200L, 1200L);
  }

  /**
   * Run every hour.
   *
   * @param runnable The task to run
   * @return The scheduled task
   */
  public Object everyHour(Runnable runnable) {
    return runTimer(runnable, 72000L, 72000L);
  }

  /** Cancel all tasks scheduled by this scheduler. */
  public void cancelAll() {
    for (Object task : tasks) {
      if (task != null) {
        cancelTask(task);
      }
    }
    tasks.clear();
  }

  /**
   * Get all active tasks.
   *
   * @return List of active tasks
   */
  public List<Object> getActiveTasks() {
    return new ArrayList<>(tasks);
  }

  // ========== Folia-specific methods ==========

  private Object runLaterFolia(Runnable runnable, long delayTicks) {
    try {
      Object task =
          Bukkit.getGlobalRegionScheduler()
              .runDelayed(plugin, t -> runnable.run(), Math.max(1L, delayTicks));
      track(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia task: " + e.getMessage());
      return null;
    }
  }

  private Object runTimerFolia(Runnable runnable, long delayTicks, long periodTicks) {
    try {
      Object task =
          Bukkit.getGlobalRegionScheduler()
              .runAtFixedRate(
                  plugin, t -> runnable.run(), Math.max(1L, delayTicks), Math.max(1L, periodTicks));
      track(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia repeating task: " + e.getMessage());
      return null;
    }
  }

  private Object runAsyncFolia(Runnable runnable) {
    try {
      Object task = Bukkit.getAsyncScheduler().runNow(plugin, t -> runnable.run());
      track(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia async task: " + e.getMessage());
      return null;
    }
  }

  private Object runLaterAsyncFolia(Runnable runnable, long delayTicks) {
    try {
      long delayMs = Math.max(0L, delayTicks) * 50; // Convert ticks to milliseconds
      Object task =
          Bukkit.getAsyncScheduler()
              .runDelayed(plugin, t -> runnable.run(), delayMs, TimeUnit.MILLISECONDS);
      track(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia async delayed task: " + e.getMessage());
      return null;
    }
  }

  private Object runTimerAsyncFolia(Runnable runnable, long delayTicks, long periodTicks) {
    try {
      long delayMs = Math.max(0L, delayTicks) * 50;
      long periodMs = Math.max(1L, periodTicks) * 50;
      Object task =
          Bukkit.getAsyncScheduler()
              .runAtFixedRate(
                  plugin, t -> runnable.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
      track(task);
      return task;
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning("Failed to schedule Folia async repeating task: " + e.getMessage());
      return null;
    }
  }

  private Object runAtEntityFolia(Entity entity, Runnable runnable) {
    try {
      Runnable profiled = wrap("runAtEntity", runnable);
      Object task = entity.getScheduler().run(plugin, t -> profiled.run(), null);
      track(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia entity task: " + e.getMessage());
      return null;
    }
  }

  private Object runAtEntityLaterFolia(Entity entity, Runnable runnable, long delayTicks) {
    try {
      Runnable profiled = wrap("runAtEntityLater", runnable);
      Object task =
          entity
              .getScheduler()
              .runDelayed(plugin, t -> profiled.run(), null, Math.max(1L, delayTicks));
      track(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia entity delayed task: " + e.getMessage());
      return null;
    }
  }

  private void cancelTask(Object task) {
    try {
      if (task instanceof BukkitTask bukkitTask) {
        if (!bukkitTask.isCancelled()) {
          bukkitTask.cancel();
        }
      } else {
        // Folia ScheduledTask - use reflection to cancel
        task.getClass().getMethod("cancel").invoke(task);
      }
    } catch (Exception e) {
      // Task already cancelled or doesn't exist
    }
  }

  private TaskHandle handle(Object task) {
    return new TaskHandle() {
      @Override
      public void cancel() {
        cancelTask(task);
      }

      @Override
      public boolean isCancelled() {
        if (task instanceof BukkitTask bukkitTask) return bukkitTask.isCancelled();
        try {
          return (boolean) task.getClass().getMethod("isCancelled").invoke(task);
        } catch (Exception ignored) {
          return false;
        }
      }

      @Override
      public Object unwrap() {
        return task;
      }
    };
  }
}
