package dev.mukulx.javaskript.profiler;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe statistical record for profiling execution times of script events, tasks, and
 * commands.
 */
public class ProfileRecord {

  private final String category; // EVENT, TASK, COMMAND
  private final String identifier; // e.g. "PlayerMoveEvent", "runTimer#1", "/heal"
  private final LongAdder count = new LongAdder();
  private final LongAdder totalNanos = new LongAdder();
  private final AtomicLong maxNanos = new AtomicLong(0);
  private final AtomicLong minNanos = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong recentNanos = new AtomicLong(0);

  public ProfileRecord(String category, String identifier) {
    this.category = category;
    this.identifier = identifier;
  }

  /**
   * Record an execution sample.
   *
   * @param durationNanos Duration in nanoseconds
   */
  public void record(long durationNanos) {
    count.increment();
    totalNanos.add(durationNanos);
    recentNanos.set(durationNanos);

    // Update max
    maxNanos.accumulateAndGet(durationNanos, Math::max);

    // Update min
    minNanos.accumulateAndGet(durationNanos, Math::min);
  }

  public String getCategory() {
    return category;
  }

  public String getIdentifier() {
    return identifier;
  }

  public long getCount() {
    return count.sum();
  }

  public long getTotalNanos() {
    return totalNanos.sum();
  }

  public double getTotalMillis() {
    return totalNanos.sum() / 1_000_000.0;
  }

  public double getAverageMicros() {
    long c = count.sum();
    if (c == 0) return 0.0;
    return (totalNanos.sum() / (double) c) / 1_000.0;
  }

  public double getAverageMillis() {
    long c = count.sum();
    if (c == 0) return 0.0;
    return (totalNanos.sum() / (double) c) / 1_000_000.0;
  }

  public double getMaxMillis() {
    long max = maxNanos.get();
    return max / 1_000_000.0;
  }

  public double getMinMillis() {
    long min = minNanos.get();
    if (min == Long.MAX_VALUE) return 0.0;
    return min / 1_000_000.0;
  }

  public double getRecentMillis() {
    return recentNanos.get() / 1_000_000.0;
  }

  /**
   * Performance rating color indicator: 🟢 Green: < 0.05 ms (optimal) 🟡 Yellow: 0.05ms - 0.50ms
   * (moderate) 🟠 Gold: 0.50ms - 2.00ms (heavy) 🔴 Red: > 2.00ms (lag spike / slow)
   */
  public String getStatusColor() {
    double avg = getAverageMillis();
    if (avg < 0.05) return "§a"; // Green
    if (avg < 0.50) return "§e"; // Yellow
    if (avg < 2.00) return "§6"; // Gold
    return "§c"; // Red
  }

  public String getFormattedAverage() {
    double avgMicros = getAverageMicros();
    if (avgMicros < 1000.0) {
      return String.format("%.2f µs", avgMicros);
    }
    return String.format("%.3f ms", getAverageMillis());
  }

  public String getFormattedTotal() {
    double totalMs = getTotalMillis();
    if (totalMs < 1000.0) {
      return String.format("%.2f ms", totalMs);
    }
    return String.format("%.2f s", totalMs / 1000.0);
  }

  public String getFormattedMax() {
    double maxMs = getMaxMillis();
    if (maxMs < 1.0) {
      return String.format("%.2f µs", maxMs * 1000.0);
    }
    return String.format("%.2f ms", maxMs);
  }

  public void reset() {
    count.reset();
    totalNanos.reset();
    maxNanos.set(0);
    minNanos.set(Long.MAX_VALUE);
    recentNanos.set(0);
  }
}
