package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptLoadResult;
import dev.mukulx.javaskript.script.compiler.CompileDiagnostics;
import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** One {@code /js} subcommand: its execution and its tab completion. */
public abstract class SubCommand {

  protected final JavaSkriptPlugin plugin;

  protected SubCommand(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  /** Runs the subcommand. {@code args[0]} is the subcommand name itself. */
  public abstract void execute(CommandSender sender, String[] args);

  /** Suggestions for {@code args}, where {@code args.length >= 2}. */
  public List<String> complete(CommandSender sender, String[] args) {
    return List.of();
  }

  /** {@code all} (optional), folders and the given script keys, filtered by what is typed. */
  protected List<String> suggestScripts(
      String[] args, boolean includeAll, Collection<String> keys) {
    String partial = joinArgs(args, 1).toLowerCase();
    List<String> suggestions = new ArrayList<>();
    if (includeAll) {
      suggestions.add("all");
      plugin.getScriptManager().getSubdirectories().forEach(d -> suggestions.add(d + "/"));
    }
    keys.forEach(k -> suggestions.add(k.replace(".java", "")));
    return suggestions.stream()
        .filter(s -> s.toLowerCase().startsWith(partial))
        .collect(Collectors.toList());
  }

  /**
   * Tell the sender how an async load went. Runs on the server thread once the script has compiled,
   * so a slow compile no longer freezes the server while the sender waits.
   */
  protected void sendLoadResult(
      CommandSender sender, String scriptKey, String past, String verb, ScriptLoadResult result) {
    switch (result.status()) {
      case LOADED ->
          sender.sendMessage(
              Component.text("Successfully " + past + ": " + scriptKey)
                  .color(NamedTextColor.GREEN));
      case FAILED -> {
        String reason =
            result.errors().isEmpty()
                ? scriptKey + " (see console)"
                : CompileDiagnostics.summary(scriptKey, result.errors());
        sender.sendMessage(
            Component.text("Failed to " + verb + ": " + reason).color(NamedTextColor.RED));
        if (plugin.getScriptManager().getScript(scriptKey) != null) {
          sender.sendMessage(
              Component.text("The previous version is still running").color(NamedTextColor.GRAY));
        }
      }
      case SKIPPED ->
          sender.sendMessage(
              Component.text(
                      "Skipped "
                          + scriptKey
                          + ": it is disabled, marked @Disabled, or was cancelled by another plugin")
                  .color(NamedTextColor.YELLOW));
      case SUPERSEDED ->
          sender.sendMessage(
              Component.text("The " + verb + " of " + scriptKey + " was replaced by a newer load")
                  .color(NamedTextColor.GRAY));
    }
  }

  /** Join args from startIndex onwards, separated by spaces. */
  protected String joinArgs(String[] args, int startIndex) {
    StringBuilder sb = new StringBuilder();
    for (int i = startIndex; i < args.length; i++) {
      if (sb.length() > 0) sb.append(" ");
      sb.append(args[i]);
    }
    return sb.toString();
  }

  /** Get the directory portion of a script key, or empty string for root. */
  protected String getDirectory(String scriptKey) {
    int lastSlash = scriptKey.lastIndexOf('/');
    if (lastSlash >= 0) {
      return scriptKey.substring(0, lastSlash);
    }
    return "";
  }

  /** Get the filename portion of a script key. */
  protected String getFileName(String scriptKey) {
    int lastSlash = scriptKey.lastIndexOf('/');
    if (lastSlash >= 0) {
      return scriptKey.substring(lastSlash + 1);
    }
    return scriptKey;
  }
}
