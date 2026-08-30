package dev.mukulx.javaskript.profiler;

import java.util.ArrayList;
import java.util.List;

/** Holds benchmark execution results and performance analysis. */
public class BenchmarkResult {

  private final String scriptKey;
  private final int iterations;
  private final long totalNanos;
  private final double opsPerSecond;
  private final double avgLatencyMicros;
  private final double minLatencyMicros;
  private final double maxLatencyMicros;
  private final long memoryUsedBytes;
  private final int classesCount;
  private final String grade;
  private final List<String> suggestions = new ArrayList<>();

  public BenchmarkResult(
      String scriptKey,
      int iterations,
      long totalNanos,
      double avgLatencyMicros,
      double minLatencyMicros,
      double maxLatencyMicros,
      long memoryUsedBytes,
      int classesCount) {
    this.scriptKey = scriptKey;
    this.iterations = iterations;
    this.totalNanos = totalNanos;
    this.avgLatencyMicros = avgLatencyMicros;
    this.minLatencyMicros = minLatencyMicros;
    this.maxLatencyMicros = maxLatencyMicros;
    this.memoryUsedBytes = memoryUsedBytes;
    this.classesCount = classesCount;

    if (totalNanos > 0) {
      this.opsPerSecond = (iterations / (totalNanos / 1_000_000_000.0));
    } else {
      this.opsPerSecond = 0;
    }

    this.grade = calculateGrade();
    generateSuggestions();
  }

  private String calculateGrade() {
    if (avgLatencyMicros < 1.0) {
      return "A+";
    } else if (avgLatencyMicros < 10.0) {
      return "A";
    } else if (avgLatencyMicros < 100.0) {
      return "B";
    } else if (avgLatencyMicros < 500.0) {
      return "C";
    } else {
      return "F";
    }
  }

  private void generateSuggestions() {
    if (avgLatencyMicros >= 100.0) {
      suggestions.add(
          "Handler latency is high (>0.1ms). Avoid heavy loops or I/O in event listeners.");
    }
    if (maxLatencyMicros > 5000.0) {
      suggestions.add("Detected spike (>5ms). Use async tasks for database, web, or file queries.");
    }
    if (memoryUsedBytes > 10 * 1024 * 1024) {
      suggestions.add("Memory usage is above 10MB. Check for unreleased collections or caches.");
    }
    if (suggestions.isEmpty()) {
      suggestions.add("Performance is optimal! Ready for high-concurrency production servers.");
    }
  }

  public String getScriptKey() {
    return scriptKey;
  }

  public int getIterations() {
    return iterations;
  }

  public long getTotalNanos() {
    return totalNanos;
  }

  public double getOpsPerSecond() {
    return opsPerSecond;
  }

  public double getAvgLatencyMicros() {
    return avgLatencyMicros;
  }

  public double getMinLatencyMicros() {
    return minLatencyMicros;
  }

  public double getMaxLatencyMicros() {
    return maxLatencyMicros;
  }

  public long getMemoryUsedBytes() {
    return memoryUsedBytes;
  }

  public int getClassesCount() {
    return classesCount;
  }

  public String getGrade() {
    return grade;
  }

  public List<String> getSuggestions() {
    return suggestions;
  }

  public String getGradeColor() {
    return switch (grade) {
      case "A+" -> "§a§l";
      case "A" -> "§a";
      case "B" -> "§e";
      case "C" -> "§6";
      default -> "§c§l";
    };
  }
}
