package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ProfileSubCommand extends SubCommand {

  public ProfileSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    var profiler = plugin.getProfiler();

    if (args.length == 1) {
      sender.sendMessage(
          Component.text("JavaSkript Performance Profiler").color(NamedTextColor.GOLD));
      boolean active = profiler.isProfilingActive();
      long elapsedSec = profiler.getSessionDurationMillis() / 1000;
      sender.sendMessage(
          Component.text("  Status: ")
              .color(NamedTextColor.YELLOW)
              .append(
                  active
                      ? Component.text("ACTIVE (" + elapsedSec + "s)").color(NamedTextColor.GREEN)
                      : Component.text("PAUSED").color(NamedTextColor.RED))
              .append(
                  Component.text(" | Scripts: " + profiler.getAllMetrics().size())
                      .color(NamedTextColor.GRAY))
              .append(
                  Component.text(" | Calls: " + profiler.getTotalRecordedCalls())
                      .color(NamedTextColor.GRAY)));

      // Show top 5 active records immediately right in chat
      var top = profiler.getTopRecords(null, 5);
      if (!top.isEmpty()) {
        sender.sendMessage(Component.text("  Top Active Handlers:").color(NamedTextColor.YELLOW));
        int rank = 1;
        for (var entry : top) {
          var rec = entry.getValue();
          String line =
              String.format(
                  "   #%d [%s] %s :: %s",
                  rank++, rec.getCategory(), entry.getKey(), rec.getIdentifier());
          sender.sendMessage(Component.text(line).color(NamedTextColor.YELLOW));
          String stats =
              String.format(
                  "      Calls: %d | Avg: %s%s§7 | Max: %s | Total: %s",
                  rec.getCount(),
                  rec.getStatusColor(),
                  rec.getFormattedAverage(),
                  rec.getFormattedMax(),
                  rec.getFormattedTotal());
          sender.sendMessage(Component.text(stats).color(NamedTextColor.GRAY));
        }
      } else {
        sender.sendMessage(
            Component.text(
                    "  No handler invocations recorded yet. Normal events and tasks track automatically.")
                .color(NamedTextColor.GRAY));
      }

      sender.sendMessage(Component.text(""));
      sender.sendMessage(
          Component.text("  /js profile top [count] - Display full rankings")
              .color(NamedTextColor.AQUA));
      sender.sendMessage(
          Component.text("  /js profile <script> - Inspect script breakdown")
              .color(NamedTextColor.AQUA));
      sender.sendMessage(
          Component.text("  /js profile reset - Clear current metrics").color(NamedTextColor.AQUA));
      sender.sendMessage(
          Component.text("  /js profile dump - Export complete report to file")
              .color(NamedTextColor.AQUA));
      sender.sendMessage(
          Component.text("  /js profile start [sec] - Start dedicated capture window")
              .color(NamedTextColor.AQUA));
      sender.sendMessage(
          Component.text("  /js benchmark <script> - Run synthetic throughput test")
              .color(NamedTextColor.AQUA));
      return;
    }

    String action = args[1].toLowerCase();

    if (action.equals("start")) {
      int duration = 60;
      if (args.length >= 3) {
        try {
          duration = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
        }
      }
      profiler.startSession(duration, sender);
      return;
    }

    if (action.equals("stop")) {
      profiler.stopSession(sender);
      return;
    }

    if (action.equals("reset")) {
      profiler.reset();
      sender.sendMessage(
          Component.text("Profiler metrics have been reset.").color(NamedTextColor.GREEN));
      return;
    }

    if (action.equals("dump")) {
      try {
        File file = profiler.dumpProfileToFile();
        sender.sendMessage(
            Component.text("Profile report exported successfully!").color(NamedTextColor.GREEN));
        sender.sendMessage(
            Component.text("Saved to: " + file.getAbsolutePath()).color(NamedTextColor.YELLOW));
      } catch (IOException e) {
        sender.sendMessage(
            Component.text("Failed to save report: " + e.getMessage()).color(NamedTextColor.RED));
      }
      return;
    }

    if (action.equals("top")) {
      int limit = 10;
      if (args.length >= 3) {
        try {
          limit = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
        }
      }

      var topRecords = profiler.getTopRecords(null, limit);
      sender.sendMessage(Component.text("Top Slowest Handlers").color(NamedTextColor.GOLD));

      if (topRecords.isEmpty()) {
        sender.sendMessage(
            Component.text(
                    "No profiling metrics recorded yet. Ensure scripts are enabled with /js enable <script> and their events/commands are active.")
                .color(NamedTextColor.YELLOW));
        return;
      }

      int rank = 1;
      for (var entry : topRecords) {
        String scriptKey = entry.getKey();
        dev.mukulx.javaskript.profiler.ProfileRecord rec = entry.getValue();

        String line =
            String.format(
                "  #%d [%s] %s :: %s", rank++, rec.getCategory(), scriptKey, rec.getIdentifier());
        sender.sendMessage(Component.text(line).color(NamedTextColor.YELLOW));

        String stats =
            String.format(
                "     Calls: %d | Avg: %s%s§7 | Max: %s | Total: %s",
                rec.getCount(),
                rec.getStatusColor(),
                rec.getFormattedAverage(),
                rec.getFormattedMax(),
                rec.getFormattedTotal());
        sender.sendMessage(Component.text(stats).color(NamedTextColor.GRAY));
      }
      return;
    }

    // Otherwise, treat as script name: /js profile <script>
    String rawTarget = joinArgs(args, 1);
    var records = profiler.getScriptRecords(rawTarget);
    String matchedKey = profiler.findMatchingScriptKey(rawTarget);
    String displayKey = matchedKey != null ? matchedKey : rawTarget;

    sender.sendMessage(
        Component.text("Performance Profile: " + displayKey).color(NamedTextColor.GOLD));

    if (records.isEmpty()) {
      File scriptFile = plugin.getScriptManager().resolveScriptFile(rawTarget);
      if (scriptFile != null && scriptFile.exists()) {
        sender.sendMessage(
            Component.text(
                    "Script is loaded, but has not had any event handlers or tasks executed yet.")
                .color(NamedTextColor.YELLOW));
      } else {
        sender.sendMessage(
            Component.text("Script not found: " + rawTarget).color(NamedTextColor.RED));
      }
      return;
    }

    for (dev.mukulx.javaskript.profiler.ProfileRecord rec : records.values()) {
      String title = String.format("  [%s] %s", rec.getCategory(), rec.getIdentifier());
      sender.sendMessage(Component.text(title).color(NamedTextColor.AQUA));
      String stats =
          String.format(
              "    Calls: %d | Avg: %s%s§7 | Max: %s | Total: %s",
              rec.getCount(),
              rec.getStatusColor(),
              rec.getFormattedAverage(),
              rec.getFormattedMax(),
              rec.getFormattedTotal());
      sender.sendMessage(Component.text(stats).color(NamedTextColor.GRAY));
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
      List<String> suggestions =
          new ArrayList<>(Arrays.asList("start", "stop", "top", "dump", "reset"));
      suggestions.addAll(subdirs);
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
    } else if (args.length == 3 && args[1].equalsIgnoreCase("start")) {
      return Arrays.asList("30", "60", "120", "300").stream()
          .filter(s -> s.startsWith(args[2]))
          .collect(Collectors.toList());
    } else if (args.length == 3 && args[1].equalsIgnoreCase("top")) {
      return Arrays.asList("5", "10", "20", "50").stream()
          .filter(s -> s.startsWith(args[2]))
          .collect(Collectors.toList());
    }
    return List.of();
  }
}
