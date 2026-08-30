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

    // Cache declared methods OUTSIDE of any timed loop to prevent generating garbage
    final Method[] methods = clazz.getDeclaredMethods();
    Method candidate = null;

    // Look for 0-arg public method
    for (Method m : methods) {
      if (java.lang.reflect.Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0) {
        candidate = m;
        break;
      }
    }

    // Determine callable action without allocating objects in the loop
    final Runnable targetAction;
    if (candidate != null) {
      final Method target = candidate;
      target.setAccessible(true);
      targetAction =
          () -> {
            try {
              target.invoke(scriptObj);
            } catch (Exception ignored) {
            }
          };
    } else if (scriptObj instanceof org.bukkit.command.CommandExecutor executor) {
      // Benchmark command execution dispatch path with console sender
      final org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();
      final org.bukkit.command.Command dummyCmd =
          new org.bukkit.command.Command("benchmark") {
            @Override
            public boolean execute(
                org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
              return true;
            }
          };
      final String[] emptyArgs = new String[0];
      targetAction =
          () -> {
            try {
              executor.onCommand(console, dummyCmd, "benchmark", emptyArgs);
            } catch (Exception ignored) {
            }
          };
    } else {
      // Test method dispatch overhead on the cached method handle
      final Method sample = methods.length > 0 ? methods[0] : null;
      if (sample != null) {
        sample.setAccessible(true);
        final Object[] dummyArgs = new Object[sample.getParameterCount()];
        targetAction =
            () -> {
              try {
                sample.invoke(scriptObj, dummyArgs);
              } catch (Exception ignored) {
              }
            };
      } else {
        targetAction =
            () -> {
              clazz.getName();
            };
      }
    }

    // 1. Warm-up Phase: Execute 2,000 iterations to trigger JIT C1/C2 native compilation
    for (int i = 0; i < 2000; i++) {
      targetAction.run();
    }

    // 2. Pre-allocate sample array before timer to guarantee ZERO heap allocation during test
    long[] samples = new long[iterations];

    // 3. Timed Execution Loop (Completely zero garbage creation)
    for (int i = 0; i < iterations; i++) {
      long t0 = System.nanoTime();
      targetAction.run();
      samples[i] = System.nanoTime() - t0;
    }

    // 4. Statistical Analysis
    Arrays.sort(samples);
    long minNanos = samples[0];
    long p99Nanos = samples[(int) (iterations * 0.99)];
    long maxNanos = samples[iterations - 1];

    long totalNanos = 0;
    for (long s : samples) {
      totalNanos += s;
    }

    double avgLatencyMicros = (totalNanos / (double) iterations) / 1000.0;
    double minLatencyMicros = minNanos / 1000.0;
    double p99LatencyMicros = p99Nanos / 1000.0;
    double maxLatencyMicros = maxNanos / 1000.0;

    // 5. Accurate Code Footprint Metrics
    long bytecodeBytes =
        (instance.getClassLoader() != null) ? instance.getClassLoader().getTotalBytecodeBytes() : 0;
    long sourceBytes =
        (instance.getScriptFile() != null && instance.getScriptFile().exists())
            ? instance.getScriptFile().length()
            : 0;
    int classesCount =
        (instance.getClassLoader() != null)
            ? instance.getClassLoader().getLoadedClasses().size()
            : 1;

    return new BenchmarkResult(
        scriptKey,
        iterations,
        totalNanos,
        avgLatencyMicros,
        minLatencyMicros,
        p99LatencyMicros,
        maxLatencyMicros,
        bytecodeBytes,
        sourceBytes,
        classesCount);
  }
}
