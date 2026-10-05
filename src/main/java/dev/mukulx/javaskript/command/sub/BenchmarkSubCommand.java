package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class BenchmarkSubCommand extends SubCommand {

  public BenchmarkSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /js benchmark <script> [iterations]").color(NamedTextColor.RED));
      return;
    }

    String rawTarget = args[1];
    File scriptFile = plugin.getScriptManager().resolveScriptFile(rawTarget);
    if (!scriptFile.exists()) {
      sender.sendMessage(
          Component.text("Script not found: " + rawTarget).color(NamedTextColor.RED));
      return;
    }

    String scriptKey = plugin.getScriptManager().getScriptKey(scriptFile);
    int iterations = 50000;
    if (args.length >= 3) {
      try {
        iterations = Integer.parseInt(args[2]);
        if (iterations <= 0 || iterations > 500000) {
          iterations = 50000;
        }
      } catch (NumberFormatException ignored) {
      }
    }

    sender.sendMessage(
        Component.text(
                "Running synthetic benchmark on " + scriptKey + " (" + iterations + " ops)...")
            .color(NamedTextColor.YELLOW));

    final int iter = iterations;
    Runnable benchTask =
        () -> {
          dev.mukulx.javaskript.profiler.BenchmarkResult result =
              plugin.getProfiler().runBenchmark(scriptKey, iter);
          if (result == null) {
            sender.sendMessage(
                Component.text("Could not benchmark script (is it loaded?)")
                    .color(NamedTextColor.RED));
            return;
          }

          sender.sendMessage(
              Component.text("Script Benchmark: " + result.getScriptKey())
                  .color(NamedTextColor.GOLD));
          sender.sendMessage(
              Component.text("  Grade: ")
                  .color(NamedTextColor.GRAY)
                  .append(Component.text(result.getGrade()).color(result.getGradeTextColor())));
          sender.sendMessage(
              Component.text("  Throughput: ")
                  .color(NamedTextColor.GRAY)
                  .append(
                      Component.text(String.format("%,.0f ops/sec", result.getOpsPerSecond()))
                          .color(
                              result.getOpsPerSecond() >= 500_000
                                  ? NamedTextColor.GREEN
                                  : (result.getOpsPerSecond() >= 100_000
                                      ? NamedTextColor.YELLOW
                                      : NamedTextColor.RED))));
          sender.sendMessage(
              Component.text("  Avg Latency: ")
                  .color(NamedTextColor.GRAY)
                  .append(
                      Component.text(
                              String.format(
                                  "%.2f µs (%.4f ms)",
                                  result.getAvgLatencyMicros(),
                                  result.getAvgLatencyMicros() / 1000.0))
                          .color(
                              result.getAvgLatencyMicros() <= 25.0
                                  ? NamedTextColor.GREEN
                                  : (result.getAvgLatencyMicros() <= 150.0
                                      ? NamedTextColor.YELLOW
                                      : NamedTextColor.RED))));
          sender.sendMessage(
              Component.text("  99th Percentile (P99): ")
                  .color(NamedTextColor.GRAY)
                  .append(
                      Component.text(String.format("%.2f µs", result.getP99LatencyMicros()))
                          .color(
                              result.getP99LatencyMicros() <= 100.0
                                  ? NamedTextColor.GREEN
                                  : (result.getP99LatencyMicros() <= 500.0
                                      ? NamedTextColor.YELLOW
                                      : NamedTextColor.RED))));
          sender.sendMessage(
              Component.text("  Min / Max: ")
                  .color(NamedTextColor.GRAY)
                  .append(
                      Component.text(
                              String.format(
                                  "%.2f µs / %.2f µs",
                                  result.getMinLatencyMicros(), result.getMaxLatencyMicros()))
                          .color(NamedTextColor.GRAY)));
          sender.sendMessage(
              Component.text("  Footprint: ")
                  .color(NamedTextColor.GRAY)
                  .append(
                      Component.text(
                              String.format(
                                  "Bytecode: %,.1f KB | Source: %,.1f KB (%d class%s)",
                                  result.getBytecodeSizeBytes() / 1024.0,
                                  result.getSourceSizeBytes() / 1024.0,
                                  result.getClassesCount(),
                                  result.getClassesCount() > 1 ? "es" : ""))
                          .color(NamedTextColor.GRAY)));
        };

    if (ServerUtil.isFolia()) {
      try {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> benchTask.run());
      } catch (Exception ignored) {
        benchTask.run();
      }
    } else {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, benchTask);
    }
  }

  @Override
  public List<String> complete(CommandSender sender, String[] args) {
    List<String> subdirs =
        plugin.getScriptManager().getSubdirectories().stream()
            .map(d -> d + "/")
            .collect(Collectors.toList());
    if (args.length == 2) {
      String arg1 = args[1].toLowerCase();
      List<String> suggestions = new ArrayList<>(subdirs);
      for (String key : plugin.getScriptManager().getLoadedScripts().keySet()) {
        String cleanKey = key.replace(".java", "");
        suggestions.add(cleanKey);
        int lastSlash = cleanKey.lastIndexOf('/');
        if (lastSlash >= 0) {
          suggestions.add(cleanKey.substring(lastSlash + 1));
        }
      }
      return suggestions.stream()
          .filter(s -> s.toLowerCase().startsWith(arg1))
          .distinct()
          .collect(Collectors.toList());
    } else if (args.length == 3) {
      return Arrays.asList("10000", "50000", "100000").stream()
          .filter(s -> s.startsWith(args[2]))
          .collect(Collectors.toList());
    }
    return List.of();
  }
}
