package dev.mukulx.javaskript.profiler;

import net.kyori.adventure.text.format.NamedTextColor;

/** Holds benchmark execution results and performance metrics. */
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

  public NamedTextColor getGradeTextColor() {
    return switch (grade) {
      case "A+", "A" -> NamedTextColor.GREEN;
      case "B" -> NamedTextColor.YELLOW;
      case "C" -> NamedTextColor.GOLD;
      default -> NamedTextColor.RED;
    };
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
