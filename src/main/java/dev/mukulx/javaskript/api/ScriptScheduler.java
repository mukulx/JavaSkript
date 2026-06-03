package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
  private final List<Object> tasks; // BukkitTask or Folia ScheduledTask
  private final boolean isFolia;

  public ScriptScheduler(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.tasks = new ArrayList<>();
    this.isFolia = ServerUtil.isFolia();
  }

  /**
   * Run a task after a delay on the global region (Folia) or main thread (Paper).
   *
   * @param runnable The task to run
   * @param delayTicks Delay in ticks (20 ticks = 1 second)
   * @return The scheduled task
   */
  public Object runLater(Runnable runnable, long delayTicks) {
    if (isFolia) {
      return runLaterFolia(runnable, delayTicks);
    } else {
      BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
      tasks.add(task);
      return task;
    }
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
    if (isFolia) {
      return runTimerFolia(runnable, delayTicks, periodTicks);
    } else {
      BukkitTask task =
          Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
      tasks.add(task);
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
    if (isFolia) {
      return runLaterFolia(runnable, 1L);
    } else {
      BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
      tasks.add(task);
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
    if (isFolia) {
      return runAsyncFolia(runnable);
    } else {
      BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
      tasks.add(task);
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
    if (isFolia) {
      return runLaterAsyncFolia(runnable, delayTicks);
    } else {
      BukkitTask task =
          Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
      tasks.add(task);
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
    if (isFolia) {
      return runTimerAsyncFolia(runnable, delayTicks, periodTicks);
    } else {
      BukkitTask task =
          Bukkit.getScheduler()
              .runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
      tasks.add(task);
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
    return CompletableFuture.runAsync(runnable);
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
          Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), delayTicks);
      tasks.add(task);
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
              .runAtFixedRate(plugin, t -> runnable.run(), delayTicks, periodTicks);
      tasks.add(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia repeating task: " + e.getMessage());
      return null;
    }
  }

  private Object runAsyncFolia(Runnable runnable) {
    try {
      Object task = Bukkit.getAsyncScheduler().runNow(plugin, t -> runnable.run());
      tasks.add(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia async task: " + e.getMessage());
      return null;
    }
  }

  private Object runLaterAsyncFolia(Runnable runnable, long delayTicks) {
    try {
      long delayMs = delayTicks * 50; // Convert ticks to milliseconds
      Object task =
          Bukkit.getAsyncScheduler()
              .runDelayed(plugin, t -> runnable.run(), delayMs, TimeUnit.MILLISECONDS);
      tasks.add(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia async delayed task: " + e.getMessage());
      return null;
    }
  }

  private Object runTimerAsyncFolia(Runnable runnable, long delayTicks, long periodTicks) {
    try {
      long delayMs = delayTicks * 50;
      long periodMs = periodTicks * 50;
      Object task =
          Bukkit.getAsyncScheduler()
              .runAtFixedRate(
                  plugin, t -> runnable.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
      tasks.add(task);
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
      Object task = entity.getScheduler().run(plugin, t -> runnable.run(), null);
      tasks.add(task);
      return task;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to schedule Folia entity task: " + e.getMessage());
      return null;
    }
  }

  private Object runAtEntityLaterFolia(Entity entity, Runnable runnable, long delayTicks) {
    try {
      Object task = entity.getScheduler().runDelayed(plugin, t -> runnable.run(), null, delayTicks);
      tasks.add(task);
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
}
