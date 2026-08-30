package dev.mukulx.javaskript.profiler;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptInstance;
import dev.mukulx.javaskript.util.ServerUtil;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitTask;

/**
 * High-performance Script Profiler and Benchmarking Engine for JavaSkript.
 *
 * <p>Tracks exact nano-level CPU time, call counts, average latencies, and lag spikes for event
 * handlers, scheduler tasks, and commands across all scripts.
 */
public class ScriptProfiler {

  private final JavaSkriptPlugin plugin;
  private final AtomicBoolean profilingActive = new AtomicBoolean(false);
  private long sessionStartTime = 0;
  private long sessionEndTime = 0;
  private BukkitTask bukkitStopTask = null;
  private Object foliaStopTask = null;

  // scriptKey -> (category:identifier -> ProfileRecord)
  private final Map<String, Map<String, ProfileRecord>> metrics = new ConcurrentHashMap<>();

  public ScriptProfiler(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  // ==========================================
  // Session Management
  // ==========================================

  /**
   * Start a profiling session for a given duration in seconds.
   *
   * @param durationSeconds Session duration in seconds (0 = manual stop)
   * @param sender The sender who started the session, or null
   */
  public void startSession(int durationSeconds, CommandSender sender) {
    cancelStopTask();
    reset();

    sessionStartTime = System.currentTimeMillis();
    sessionEndTime = 0;
    profilingActive.set(true);

    if (sender != null) {
      sender.sendMessage(
          Component.text("Script profiling session STARTED.").color(NamedTextColor.GREEN));
      if (durationSeconds > 0) {
        sender.sendMessage(
            Component.text("Session will automatically stop after " + durationSeconds + " seconds.")
                .color(NamedTextColor.GRAY));
      } else {
        sender.sendMessage(
            Component.text("Run '/js profile stop' to finish and view results.")
                .color(NamedTextColor.GRAY));
      }
    }

    if (durationSeconds > 0) {
      long delayTicks = durationSeconds * 20L;
      Runnable stopAction =
          () -> {
            if (profilingActive.get()) {
              stopSession(sender);
              if (sender != null) {
                sender.sendMessage(
                    Component.text(
                            "Profiling session completed! Run '/js profile top' to view results.")
                        .color(NamedTextColor.GOLD));
              }
            }
          };

      if (ServerUtil.isFolia()) {
        try {
          foliaStopTask =
              Bukkit.getGlobalRegionScheduler()
                  .runDelayed(plugin, task -> stopAction.run(), delayTicks);
        } catch (Exception ignored) {
        }
      } else {
        bukkitStopTask = Bukkit.getScheduler().runTaskLater(plugin, stopAction, delayTicks);
      }
    }
  }

  /**
   * Stop the active profiling session.
   *
   * @param sender The sender who stopped the session, or null
   */
  public void stopSession(CommandSender sender) {
    cancelStopTask();
    sessionEndTime = System.currentTimeMillis();
    profilingActive.set(false);

    long elapsedMs = getSessionDurationMillis();
    if (sender != null) {
      sender.sendMessage(
          Component.text(
                  "Script profiling session STOPPED (Duration: " + (elapsedMs / 1000.0) + "s).")
              .color(NamedTextColor.YELLOW));
      sender.sendMessage(
          Component.text(
                  "Use '/js profile top' or '/js profile <script>' to view detailed timings.")
              .color(NamedTextColor.GRAY));
    }
  }

  public boolean isProfilingActive() {
    return profilingActive.get();
  }

  public long getSessionDurationMillis() {
    if (sessionStartTime == 0) return 0;
    long end = (sessionEndTime > 0) ? sessionEndTime : System.currentTimeMillis();
    return Math.max(0, end - sessionStartTime);
  }

  public void reset() {
    metrics.clear();
    sessionStartTime = 0;
    sessionEndTime = 0;
  }

  private void cancelStopTask() {
    if (bukkitStopTask != null) {
      bukkitStopTask.cancel();
      bukkitStopTask = null;
    }
    if (foliaStopTask != null) {
      try {
        ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) foliaStopTask).cancel();
      } catch (Exception ignored) {
      }
      foliaStopTask = null;
    }
  }

  // ==========================================
  // Recording
  // ==========================================

  /**
   * Record an execution duration for a script item.
   *
   * @param scriptKey Script identifier (e.g. "pvp/Combat.java")
   * @param category Metric category ("EVENT", "TASK", "COMMAND")
   * @param identifier Specific name (e.g. "PlayerMoveEvent", "timer#1", "/heal")
   * @param durationNanos Nanoseconds elapsed
   */
  public void record(String scriptKey, String category, String identifier, long durationNanos) {
    if (!profilingActive.get()) {
      return;
    }
    if (scriptKey == null || category == null || identifier == null) {
      return;
    }

    Map<String, ProfileRecord> scriptMap =
        metrics.computeIfAbsent(scriptKey, k -> new ConcurrentHashMap<>());
    String metricKey = category + ":" + identifier;
    ProfileRecord record =
        scriptMap.computeIfAbsent(metricKey, k -> new ProfileRecord(category, identifier));
    record.record(durationNanos);
  }

  // ==========================================
  // Queries & Analytics
  // ==========================================

  public Map<String, ProfileRecord> getScriptRecords(String scriptKey) {
    return metrics.getOrDefault(scriptKey, Collections.emptyMap());
  }

  public Map<String, Map<String, ProfileRecord>> getAllMetrics() {
    return Collections.unmodifiableMap(metrics);
  }

  /**
   * Get top slowest records across all scripts, filtered by category (or all if category is null).
   */
  public List<Map.Entry<String, ProfileRecord>> getTopRecords(String category, int limit) {
    List<Map.Entry<String, ProfileRecord>> all = new ArrayList<>();

    for (Map.Entry<String, Map<String, ProfileRecord>> scriptEntry : metrics.entrySet()) {
      String sKey = scriptEntry.getKey();
      for (ProfileRecord record : scriptEntry.getValue().values()) {
        if (category == null || record.getCategory().equalsIgnoreCase(category)) {
          all.add(new AbstractMap.SimpleEntry<>(sKey, record));
        }
      }
    }

    // Sort by total execution time descending
    all.sort((a, b) -> Long.compare(b.getValue().getTotalNanos(), a.getValue().getTotalNanos()));

    if (all.size() > limit) {
      return all.subList(0, limit);
    }
    return all;
  }

  /** Get scripts ranked by total CPU time consumed. */
  public List<Map.Entry<String, Long>> getTopScripts(int limit) {
    Map<String, Long> scriptTotals = new HashMap<>();

    for (Map.Entry<String, Map<String, ProfileRecord>> entry : metrics.entrySet()) {
      long total = 0;
      for (ProfileRecord rec : entry.getValue().values()) {
        total += rec.getTotalNanos();
      }
      scriptTotals.put(entry.getKey(), total);
    }

    List<Map.Entry<String, Long>> sorted = new ArrayList<>(scriptTotals.entrySet());
    sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

    if (sorted.size() > limit) {
      return sorted.subList(0, limit);
    }
    return sorted;
  }

  /** Export the current profiling results to a formatted file in plugins/JavaSkript/profiles/. */
  public File dumpProfileToFile() throws IOException {
    File profilesDir = new File(plugin.getDataFolder(), "profiles");
    if (!profilesDir.exists()) {
      profilesDir.mkdirs();
    }

    String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    File dumpFile = new File(profilesDir, "profile-" + timestamp + ".txt");

    StringBuilder sb = new StringBuilder();
    sb.append("=================================================================\n");
    sb.append("                 JAVASKRIPT PROFILING REPORT                     \n");
    sb.append("=================================================================\n");
    sb.append("Date: ").append(new Date()).append("\n");
    sb.append("Session Duration: ").append(getSessionDurationMillis() / 1000.0).append("s\n");
    sb.append("Server Engine: ").append(ServerUtil.isFolia() ? "Folia" : "Paper").append("\n");
    sb.append("=================================================================\n\n");

    sb.append("--- TOP CPU CONSUMING SCRIPTS ---\n");
    List<Map.Entry<String, Long>> topScripts = getTopScripts(20);
    for (Map.Entry<String, Long> entry : topScripts) {
      double ms = entry.getValue() / 1_000_000.0;
      sb.append(String.format("  * %-30s Total: %8.2f ms\n", entry.getKey(), ms));
    }
    sb.append("\n");

    sb.append("--- DETAILED SCRIPT METRICS ---\n");
    for (Map.Entry<String, Map<String, ProfileRecord>> entry : metrics.entrySet()) {
      sb.append("\n[Script: ").append(entry.getKey()).append("]\n");
      sb.append(
          String.format(
              "  %-10s %-30s %8s %12s %12s %12s\n",
              "TYPE", "IDENTIFIER", "CALLS", "AVG", "MAX", "TOTAL"));
      sb.append(
          "  --------------------------------------------------------------------------------\n");

      for (ProfileRecord rec : entry.getValue().values()) {
        sb.append(
            String.format(
                "  %-10s %-30s %8d %12s %12s %12s\n",
                rec.getCategory(),
                rec.getIdentifier(),
                rec.getCount(),
                rec.getFormattedAverage(),
                rec.getFormattedMax(),
                rec.getFormattedTotal()));
      }
    }

    Files.writeString(dumpFile.toPath(), sb.toString());
    return dumpFile;
  }

  // ==========================================
  // Synthetic Benchmarker
  // ==========================================

  /**
   * Run a high-speed synthetic throughput benchmark on a loaded script.
   *
   * @param scriptKey Script identifier
   * @param iterations Number of test iterations (e.g. 50,000)
   * @return BenchmarkResult
   */
  public BenchmarkResult runBenchmark(String scriptKey, int iterations) {
    ScriptInstance instance = plugin.getScriptManager().getScript(scriptKey);
    if (instance == null) {
      return null;
    }

    Object scriptObj = instance.getInstance();
    Class<?> clazz = instance.getScriptClass();

    // Find any testable method (event handler, command, or public method)
    Method targetMethod = null;
    for (Method m : clazz.getDeclaredMethods()) {
      if (m.isAnnotationPresent(EventHandler.class)) {
        targetMethod = m;
        break;
      }
    }
    if (targetMethod == null) {
      for (Method m : clazz.getDeclaredMethods()) {
        if (java.lang.reflect.Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0) {
          targetMethod = m;
          break;
        }
      }
    }

    // Measure memory delta
    System.gc();
    long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    long minNanos = Long.MAX_VALUE;
    long maxNanos = 0;
    long totalNanos = 0;

    // Warm up JIT compiler
    if (targetMethod != null && targetMethod.getParameterCount() == 0) {
      targetMethod.setAccessible(true);
      for (int i = 0; i < 500; i++) {
        try {
          targetMethod.invoke(scriptObj);
        } catch (Exception ignored) {
        }
      }

      // Active timed loop
      for (int i = 0; i < iterations; i++) {
        long t0 = System.nanoTime();
        try {
          targetMethod.invoke(scriptObj);
        } catch (Exception ignored) {
        }
        long dt = System.nanoTime() - t0;
        totalNanos += dt;
        if (dt < minNanos) minNanos = dt;
        if (dt > maxNanos) maxNanos = dt;
      }
    } else {
      // Benchmark reflection overhead and method access dispatch
      for (int i = 0; i < iterations; i++) {
        long t0 = System.nanoTime();
        clazz.getDeclaredMethods();
        long dt = System.nanoTime() - t0;
        totalNanos += dt;
        if (dt < minNanos) minNanos = dt;
        if (dt > maxNanos) maxNanos = dt;
      }
    }

    long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    long memDelta = Math.max(0, memAfter - memBefore);

    double avgLatencyMicros = (totalNanos / (double) iterations) / 1000.0;
    double minLatencyMicros = (minNanos == Long.MAX_VALUE) ? 0.0 : (minNanos / 1000.0);
    double maxLatencyMicros = maxNanos / 1000.0;

    int classesCount = 1;

    return new BenchmarkResult(
        scriptKey,
        iterations,
        totalNanos,
        avgLatencyMicros,
        minLatencyMicros,
        maxLatencyMicros,
        memDelta,
        classesCount);
  }
}
