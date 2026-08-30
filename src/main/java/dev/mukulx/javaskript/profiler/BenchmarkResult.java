package dev.mukulx.javaskript.profiler;

import java.util.ArrayList;
import java.util.List;

/** Holds truthful benchmark execution results and performance analysis. */
public class BenchmarkResult {

  private final String scriptKey;
  private final int iterations;
  private final long totalNanos;
  private final double opsPerSecond;
  private final double avgLatencyMicros;
  private final double minLatencyMicros;
  private final double p99LatencyMicros;
  private final double maxLatencyMicros;
  private final long bytecodeSizeBytes;
  private final long sourceSizeBytes;
  private final int classesCount;
  private final String grade;
  private final List<String> suggestions = new ArrayList<>();

  public BenchmarkResult(
      String scriptKey,
      int iterations,
      long totalNanos,
      double avgLatencyMicros,
      double minLatencyMicros,
      double p99LatencyMicros,
      double maxLatencyMicros,
      long bytecodeSizeBytes,
      long sourceSizeBytes,
      int classesCount) {
    this.scriptKey = scriptKey;
    this.iterations = iterations;
    this.totalNanos = totalNanos;
    this.avgLatencyMicros = avgLatencyMicros;
    this.minLatencyMicros = minLatencyMicros;
    this.p99LatencyMicros = p99LatencyMicros;
    this.maxLatencyMicros = maxLatencyMicros;
    this.bytecodeSizeBytes = bytecodeSizeBytes;
    this.sourceSizeBytes = sourceSizeBytes;
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
    if (avgLatencyMicros < 2.0) {
      return "A+";
    } else if (avgLatencyMicros < 25.0) {
      return "A";
    } else if (avgLatencyMicros < 150.0) {
      return "B";
    } else if (avgLatencyMicros < 1000.0) {
      return "C";
    } else {
      return "F";
    }
  }

  private void generateSuggestions() {
    if (avgLatencyMicros >= 500.0) {
      suggestions.add(
          "Average execution time is high (>0.5ms). Consider moving heavy operations to async tasks.");
    } else if (p99LatencyMicros >= 2000.0) {
      suggestions.add(
          "Occasional latency spikes detected in 99th percentile (>2ms). Check for synchronous I/O or large iterations.");
    }

    if (bytecodeSizeBytes > 500 * 1024) {
      suggestions.add(
          "Compiled bytecode is over 500KB. Consider splitting into multiple modular scripts.");
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

  public double getP99LatencyMicros() {
    return p99LatencyMicros;
  }

  public double getMaxLatencyMicros() {
    return maxLatencyMicros;
  }

  public long getBytecodeSizeBytes() {
    return bytecodeSizeBytes;
  }

  public long getSourceSizeBytes() {
    return sourceSizeBytes;
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
