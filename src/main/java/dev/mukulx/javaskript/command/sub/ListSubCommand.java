package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptInstance;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ListSubCommand extends SubCommand {

  public ListSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    Map<String, ScriptInstance> scripts = plugin.getScriptManager().getLoadedScripts();
    Set<String> disabledScripts = plugin.getScriptManager().getDisabledScripts();

    sender.sendMessage(Component.text("JavaSkript Scripts").color(NamedTextColor.GOLD));

    if (scripts.isEmpty() && disabledScripts.isEmpty()) {
      sender.sendMessage(Component.text("No scripts found.").color(NamedTextColor.YELLOW));
      return;
    }

    // Build a directory tree view
    // Group scripts by their directory
    Map<String, List<String>> loadedByDir = new TreeMap<>();
    Map<String, List<String>> disabledByDir = new TreeMap<>();

    for (String key : scripts.keySet()) {
      String dir = getDirectory(key);
      String name = getFileName(key);
      loadedByDir.computeIfAbsent(dir, k -> new ArrayList<>()).add(name);
    }

    for (String key : disabledScripts) {
      String dir = getDirectory(key);
      String name = getFileName(key);
      disabledByDir.computeIfAbsent(dir, k -> new ArrayList<>()).add(name);
    }

    // Collect all directories
    Set<String> allDirs = new TreeSet<>();
    allDirs.addAll(loadedByDir.keySet());
    allDirs.addAll(disabledByDir.keySet());

    int totalLoaded = scripts.size();
    int totalDisabled = disabledScripts.size();

    sender.sendMessage(
        Component.text("Total: " + totalLoaded + " loaded, " + totalDisabled + " disabled")
            .color(NamedTextColor.GRAY));

    for (String dir : allDirs) {
      // Directory header
      String displayDir = dir.isEmpty() ? "scripts/" : "scripts/" + dir + "/";
      sender.sendMessage(Component.text("  📁 " + displayDir).color(NamedTextColor.AQUA));

      // Loaded scripts in this directory
      List<String> loaded = loadedByDir.getOrDefault(dir, Collections.emptyList());
      for (String name : loaded) {
        String fullKey = dir.isEmpty() ? name : dir + "/" + name;
        ScriptInstance instance = scripts.get(fullKey);
        String className = instance != null ? instance.getScriptClass().getSimpleName() : name;
        sender.sendMessage(
            Component.text("    ✓ " + name + " (" + className + ")").color(NamedTextColor.GREEN));
      }

      // Disabled scripts in this directory
      List<String> disabled = disabledByDir.getOrDefault(dir, Collections.emptyList());
      for (String name : disabled) {
        sender.sendMessage(Component.text("    ✗ " + name).color(NamedTextColor.RED));
      }
    }
  }
}
