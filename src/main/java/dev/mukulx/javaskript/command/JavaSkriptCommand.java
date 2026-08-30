package dev.mukulx.javaskript.command;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptInstance;
import dev.mukulx.javaskript.util.ServerUtil;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class JavaSkriptCommand implements CommandExecutor, TabCompleter {

  private final JavaSkriptPlugin plugin;

  public JavaSkriptCommand(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("javaskript.admin")) {
      sender.sendMessage(
          Component.text("You don't have permission to use this command!")
              .color(NamedTextColor.RED));
      return true;
    }

    if (args.length == 0) {
      sendHelp(sender);
      return true;
    }

    String subCommand = args[0].toLowerCase();

    switch (subCommand) {
      case "reload" -> handleReload(sender, args);
      case "restart" -> handleRestart(sender, args);
      case "configreload" -> handleConfigReload(sender);
      case "list" -> handleList(sender, args);
      case "load" -> handleLoad(sender, args);
      case "unload" -> handleUnload(sender, args);
      case "enable" -> handleEnable(sender, args);
      case "disable" -> handleDisable(sender, args);
      case "info" -> handleInfo(sender, args);
      case "debug" -> handleDebug(sender);
      default -> sendHelp(sender);
    }

    return true;
  }

  private void handleReload(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /js reload <script|all>").color(NamedTextColor.RED));
      sender.sendMessage(
          Component.text("  /js reload <script> - Reload a specific script")
              .color(NamedTextColor.GRAY));
      sender.sendMessage(
          Component.text("  /js reload all - Reload all scripts").color(NamedTextColor.GRAY));
      return;
    }

    String target = args[1];

    if (target.equalsIgnoreCase("all")) {
      // Reload all scripts
      sender.sendMessage(Component.text("Reloading all scripts...").color(NamedTextColor.YELLOW));

      try {
        plugin.getScriptManager().reloadAllScripts();
        int count = plugin.getScriptManager().getLoadedScripts().size();
        sender.sendMessage(
            Component.text("Successfully reloaded " + count + " script(s)!")
                .color(NamedTextColor.GREEN));
      } catch (Exception e) {
        sender.sendMessage(
            Component.text("Error reloading scripts: " + e.getMessage()).color(NamedTextColor.RED));
        plugin.getLogger().severe("Error reloading scripts: " + e.getMessage());
      }
    } else {
      String rawTarget = joinArgs(args, 1);

      // Check if target is a directory
      if (plugin.getScriptManager().isDirectory(rawTarget)) {
        File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
        sender.sendMessage(
            Component.text("Reloading folder: " + rawTarget).color(NamedTextColor.YELLOW));
        int count = plugin.getScriptManager().reloadDirectory(dir);
        sender.sendMessage(
            Component.text("Successfully reloaded " + count + " script(s) in folder: " + rawTarget)
                .color(NamedTextColor.GREEN));
        return;
      }

      // Reload specific script
      String scriptKey = rawTarget;
      if (!scriptKey.endsWith(".java")) {
        scriptKey += ".java";
      }

      sender.sendMessage(
          Component.text("Reloading script: " + scriptKey).color(NamedTextColor.YELLOW));

      File scriptFile = plugin.getScriptManager().resolveScriptFile(scriptKey);

      if (!scriptFile.exists()) {
        sender.sendMessage(
            Component.text("Script not found: " + scriptKey).color(NamedTextColor.RED));
        return;
      }

      plugin.getScriptManager().unloadScript(scriptKey);
      boolean success = plugin.getScriptManager().loadScript(scriptFile);

      if (success) {
        sender.sendMessage(
            Component.text("Successfully reloaded: " + scriptKey).color(NamedTextColor.GREEN));
      } else {
        sender.sendMessage(
            Component.text("Failed to reload: " + scriptKey).color(NamedTextColor.RED));
      }
    }
  }

  private void handleRestart(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /js restart <script|all>").color(NamedTextColor.RED));
      sender.sendMessage(
          Component.text("  /js restart <script> - Restart a specific script")
              .color(NamedTextColor.GRAY));
      sender.sendMessage(
          Component.text("  /js restart all - Restart all scripts").color(NamedTextColor.GRAY));
      return;
    }

    String target = args[1];

    if (target.equalsIgnoreCase("all")) {
      // Restart all scripts
      sender.sendMessage(Component.text("Restarting all scripts...").color(NamedTextColor.YELLOW));

      try {
        plugin.getScriptManager().unloadAllScripts();
        sender.sendMessage(Component.text("All scripts unloaded").color(NamedTextColor.GRAY));

        // Small delay to ensure cleanup (Folia-safe via global region scheduler)
        Runnable loadTask =
            () -> {
              plugin.getScriptManager().loadAllScripts();
              int count = plugin.getScriptManager().getLoadedScripts().size();
              sender.sendMessage(
                  Component.text("Successfully restarted " + count + " script(s)!")
                      .color(NamedTextColor.GREEN));
            };

        if (ServerUtil.isFolia()) {
          plugin
              .getServer()
              .getGlobalRegionScheduler()
              .runDelayed(plugin, task -> loadTask.run(), 20L);
        } else {
          plugin.getServer().getScheduler().runTaskLater(plugin, loadTask, 20L);
        }
      } catch (Exception e) {
        sender.sendMessage(
            Component.text("Error restarting scripts: " + e.getMessage())
                .color(NamedTextColor.RED));
        plugin.getLogger().severe("Error restarting scripts: " + e.getMessage());
      }
    } else {
      // Restart specific script
      String scriptKey = joinArgs(args, 1);
      if (!scriptKey.endsWith(".java")) {
        scriptKey += ".java";
      }

      sender.sendMessage(
          Component.text("Restarting script: " + scriptKey).color(NamedTextColor.YELLOW));

      File scriptFile = plugin.getScriptManager().resolveScriptFile(scriptKey);

      if (!scriptFile.exists()) {
        sender.sendMessage(
            Component.text("Script not found: " + scriptKey).color(NamedTextColor.RED));
        return;
      }

      // Unload
      plugin.getScriptManager().unloadScript(scriptKey);
      sender.sendMessage(Component.text("Script unloaded").color(NamedTextColor.GRAY));

      // Reload after delay (Folia-safe)
      String finalScriptKey = scriptKey;
      Runnable reloadTask =
          () -> {
            boolean success = plugin.getScriptManager().loadScript(scriptFile);

            if (success) {
              sender.sendMessage(
                  Component.text("Successfully restarted: " + finalScriptKey)
                      .color(NamedTextColor.GREEN));
            } else {
              sender.sendMessage(
                  Component.text("Failed to restart: " + finalScriptKey).color(NamedTextColor.RED));
            }
          };

      if (ServerUtil.isFolia()) {
        plugin
            .getServer()
            .getGlobalRegionScheduler()
            .runDelayed(plugin, task -> reloadTask.run(), 20L);
      } else {
        plugin.getServer().getScheduler().runTaskLater(plugin, reloadTask, 20L);
      }
    }
  }

  private void handleConfigReload(CommandSender sender) {
    sender.sendMessage(Component.text("Reloading configuration...").color(NamedTextColor.YELLOW));

    try {
      plugin.reloadConfig();
      sender.sendMessage(
          Component.text("Configuration reloaded successfully!").color(NamedTextColor.GREEN));
      sender.sendMessage(
          Component.text("Note: Some settings require a plugin restart to take effect")
              .color(NamedTextColor.GRAY));
    } catch (Exception e) {
      sender.sendMessage(
          Component.text("Error reloading configuration: " + e.getMessage())
              .color(NamedTextColor.RED));
      plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
    }
  }

  private void handleList(CommandSender sender, String[] args) {
    Map<String, ScriptInstance> scripts = plugin.getScriptManager().getLoadedScripts();
    Set<String> disabledScripts = plugin.getScriptManager().getDisabledScripts();

    sender.sendMessage(Component.text("=== JavaSkript Scripts ===").color(NamedTextColor.GOLD));

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

  private void handleLoad(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript load <script|folder>").color(NamedTextColor.RED));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    // Check if target is a directory
    if (plugin.getScriptManager().isDirectory(rawTarget)) {
      File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
      sender.sendMessage(
          Component.text("Loading folder: " + rawTarget).color(NamedTextColor.YELLOW));
      int count = plugin.getScriptManager().loadDirectory(dir);
      sender.sendMessage(
          Component.text("Successfully loaded " + count + " script(s) from folder: " + rawTarget)
              .color(NamedTextColor.GREEN));
      return;
    }

    String scriptKey = rawTarget;
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    // Check if already loaded
    if (plugin.getScriptManager().getScript(scriptKey) != null) {
      sender.sendMessage(
          Component.text("Script already loaded: " + scriptKey).color(NamedTextColor.RED));
      sender.sendMessage(
          Component.text("Use /js reload " + scriptKey + " to reload it")
              .color(NamedTextColor.YELLOW));
      return;
    }

    File scriptFile = plugin.getScriptManager().resolveScriptFile(scriptKey);

    if (!scriptFile.exists()) {
      sender.sendMessage(
          Component.text("Script not found: " + scriptKey).color(NamedTextColor.RED));
      return;
    }

    sender.sendMessage(Component.text("Loading script: " + scriptKey).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().loadScript(scriptFile);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully loaded: " + scriptKey).color(NamedTextColor.GREEN));
    } else {
      sender.sendMessage(Component.text("Failed to load: " + scriptKey).color(NamedTextColor.RED));
    }
  }

  private void handleUnload(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript unload <script|folder>").color(NamedTextColor.RED));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    // Check if target is a directory
    if (plugin.getScriptManager().isDirectory(rawTarget)) {
      sender.sendMessage(
          Component.text("Unloading folder: " + rawTarget).color(NamedTextColor.YELLOW));
      int count = plugin.getScriptManager().unloadDirectory(rawTarget);
      sender.sendMessage(
          Component.text("Successfully unloaded " + count + " script(s) in folder: " + rawTarget)
              .color(NamedTextColor.GREEN));
      return;
    }

    String scriptKey = rawTarget;
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    sender.sendMessage(
        Component.text("Unloading script: " + scriptKey).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().unloadScript(scriptKey);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully unloaded: " + scriptKey).color(NamedTextColor.GREEN));
    } else {
      sender.sendMessage(
          Component.text("Script not loaded: " + scriptKey).color(NamedTextColor.RED));
    }
  }

  private void handleInfo(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript info <script>").color(NamedTextColor.RED));
      return;
    }

    String scriptKey = joinArgs(args, 1);
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    ScriptInstance instance = plugin.getScriptManager().getScript(scriptKey);

    if (instance == null) {
      // Check if disabled
      if (plugin.getScriptManager().isScriptDisabled(scriptKey)) {
        sender.sendMessage(
            Component.text("Script is disabled: " + scriptKey).color(NamedTextColor.RED));
        sender.sendMessage(
            Component.text("Use /js enable " + scriptKey + " to enable it")
                .color(NamedTextColor.YELLOW));
      } else {
        sender.sendMessage(
            Component.text("Script not loaded: " + scriptKey).color(NamedTextColor.RED));
      }
      return;
    }

    sender.sendMessage(Component.text("Script Information:").color(NamedTextColor.GOLD));
    sender.sendMessage(
        Component.text("  Name: " + instance.getName()).color(NamedTextColor.YELLOW));
    sender.sendMessage(Component.text("  Path: " + scriptKey).color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  Class: " + instance.getScriptClass().getName())
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  File: " + instance.getScriptFile().getAbsolutePath())
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(Component.text("  Status: Enabled").color(NamedTextColor.GREEN));
    sender.sendMessage(
        Component.text("  Compatibility: " + instance.getFoliaCompatibilityStatus())
            .color(NamedTextColor.YELLOW));

    Class<?> clazz = instance.getScriptClass();
    List<String> interfaces =
        Arrays.stream(clazz.getInterfaces()).map(Class::getSimpleName).collect(Collectors.toList());

    if (!interfaces.isEmpty()) {
      sender.sendMessage(
          Component.text("  Implements: " + String.join(", ", interfaces))
              .color(NamedTextColor.YELLOW));
    }
  }

  private void handleEnable(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript enable <script|folder>").color(NamedTextColor.RED));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    // Check if target is a directory
    if (plugin.getScriptManager().isDirectory(rawTarget)) {
      File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
      sender.sendMessage(
          Component.text("Enabling folder: " + rawTarget).color(NamedTextColor.YELLOW));
      boolean success = plugin.getScriptManager().enableDirectory(dir);
      if (success) {
        sender.sendMessage(
            Component.text("Successfully enabled folder: " + rawTarget)
                .color(NamedTextColor.GREEN));
      } else {
        sender.sendMessage(
            Component.text("Failed to enable folder (not disabled or error): " + rawTarget)
                .color(NamedTextColor.RED));
      }
      return;
    }

    String scriptKey = rawTarget;
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    // Check if already enabled
    if (!plugin.getScriptManager().isScriptDisabled(scriptKey)) {
      sender.sendMessage(
          Component.text("Script is already enabled: " + scriptKey).color(NamedTextColor.YELLOW));
      return;
    }

    sender.sendMessage(
        Component.text("Enabling script: " + scriptKey).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().enableScript(scriptKey);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully enabled: " + scriptKey).color(NamedTextColor.GREEN));
    } else {
      sender.sendMessage(
          Component.text("Failed to enable: " + scriptKey).color(NamedTextColor.RED));
    }
  }

  private void handleDisable(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript disable <script|folder>").color(NamedTextColor.RED));
      return;
    }

    String rawTarget = joinArgs(args, 1);

    // Check if target is a directory
    if (plugin.getScriptManager().isDirectory(rawTarget)) {
      File dir = plugin.getScriptManager().resolveDirectory(rawTarget);
      sender.sendMessage(
          Component.text("Disabling folder: " + rawTarget).color(NamedTextColor.YELLOW));
      boolean success = plugin.getScriptManager().disableDirectory(dir);
      if (success) {
        sender.sendMessage(
            Component.text("Successfully disabled folder: " + rawTarget)
                .color(NamedTextColor.GREEN));
        sender.sendMessage(
            Component.text("Folder renamed with '-' prefix; scripts unloaded")
                .color(NamedTextColor.GRAY));
      } else {
        sender.sendMessage(
            Component.text("Failed to disable folder (already disabled or error): " + rawTarget)
                .color(NamedTextColor.RED));
      }
      return;
    }

    String scriptKey = rawTarget;
    if (!scriptKey.endsWith(".java")) {
      scriptKey += ".java";
    }

    // Check if already disabled
    if (plugin.getScriptManager().isScriptDisabled(scriptKey)) {
      sender.sendMessage(
          Component.text("Script is already disabled: " + scriptKey).color(NamedTextColor.YELLOW));
      return;
    }

    sender.sendMessage(
        Component.text("Disabling script: " + scriptKey).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().disableScript(scriptKey);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully disabled: " + scriptKey).color(NamedTextColor.GREEN));
      sender.sendMessage(
          Component.text("Script will not load until enabled again").color(NamedTextColor.GRAY));
    } else {
      sender.sendMessage(
          Component.text("Failed to disable: " + scriptKey).color(NamedTextColor.RED));
    }
  }

  private void handleDebug(CommandSender sender) {
    boolean currentMode = plugin.isDebugMode();
    boolean newMode = !currentMode;

    plugin.setDebugMode(newMode);

    if (newMode) {
      sender.sendMessage(Component.text("Debug mode ENABLED").color(NamedTextColor.GREEN));
      sender.sendMessage(
          Component.text("Detailed logging is now active").color(NamedTextColor.GRAY));
    } else {
      sender.sendMessage(Component.text("Debug mode DISABLED").color(NamedTextColor.YELLOW));
      sender.sendMessage(Component.text("Logging returned to normal").color(NamedTextColor.GRAY));
    }
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage(Component.text("JavaSkript Commands:").color(NamedTextColor.GOLD));
    sender.sendMessage(
        Component.text("  /js reload <script|all> - Reload a script or all scripts")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js restart <script|all> - Restart a script or all scripts")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js configreload - Reload the configuration file")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js list - List all scripts (loaded and disabled)")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js load <script|folder> - Load a script or entire folder")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js unload <script|folder> - Unload a script or entire folder")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js enable <script|folder> - Enable a disabled script or folder")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js disable <script|folder> - Disable a script or folder")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js info <script> - Show script information")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js debug - Toggle debug mode").color(NamedTextColor.YELLOW));
    sender.sendMessage(Component.text("").color(NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text("Tip: Organize scripts in folders: scripts/pvp/PvP.java")
            .color(NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text("Tip: Prefix with '-' to disable: -PvP.java or folder -examples/")
            .color(NamedTextColor.GRAY));
    sender.sendMessage(
        Component.text("Tip: In code, use @Disabled or // @disabled to disable")
            .color(NamedTextColor.GRAY));
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("javaskript.admin")) {
      return List.of();
    }

    if (args.length == 1) {
      return Arrays.asList(
              "reload",
              "restart",
              "configreload",
              "list",
              "load",
              "unload",
              "enable",
              "disable",
              "info",
              "debug")
          .stream()
          .filter(s -> s.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }

    if (args.length >= 2) {
      String subCommand = args[0].toLowerCase();
      String partial = joinArgs(args, 1).toLowerCase();

      List<String> subdirs =
          plugin.getScriptManager().getSubdirectories().stream()
              .map(d -> d + "/")
              .collect(Collectors.toList());

      if (subCommand.equals("reload") || subCommand.equals("restart")) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("all");
        suggestions.addAll(subdirs);
        suggestions.addAll(
            plugin.getScriptManager().getLoadedScripts().keySet().stream()
                .map(s -> s.replace(".java", ""))
                .collect(Collectors.toList()));

        return suggestions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .collect(Collectors.toList());
      }

      if (subCommand.equals("unload")
          || subCommand.equals("info")
          || subCommand.equals("disable")) {
        List<String> suggestions = new ArrayList<>();
        if (!subCommand.equals("info")) {
          suggestions.addAll(subdirs);
        }
        suggestions.addAll(
            plugin.getScriptManager().getLoadedScripts().keySet().stream()
                .map(s -> s.replace(".java", ""))
                .collect(Collectors.toList()));

        return suggestions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .collect(Collectors.toList());
      }

      if (subCommand.equals("enable")) {
        List<String> suggestions = new ArrayList<>(subdirs);
        suggestions.addAll(
            plugin.getScriptManager().getDisabledScripts().stream()
                .map(s -> s.replace(".java", ""))
                .collect(Collectors.toList()));

        return suggestions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .collect(Collectors.toList());
      }

      if (subCommand.equals("load")) {
        List<String> suggestions = new ArrayList<>(subdirs);
        suggestions.addAll(
            plugin.getScriptManager().getAllScriptKeys().stream()
                .filter(key -> !plugin.getScriptManager().isScriptDisabled(key))
                .map(s -> s.replace(".java", ""))
                .collect(Collectors.toList()));

        return suggestions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .collect(Collectors.toList());
      }
    }

    return List.of();
  }

  // ==========================================
  // Utilities
  // ==========================================

  /** Join args from startIndex onwards, separated by spaces. */
  private String joinArgs(String[] args, int startIndex) {
    StringBuilder sb = new StringBuilder();
    for (int i = startIndex; i < args.length; i++) {
      if (sb.length() > 0) sb.append(" ");
      sb.append(args[i]);
    }
    return sb.toString();
  }

  /** Get the directory portion of a script key, or empty string for root. */
  private String getDirectory(String scriptKey) {
    int lastSlash = scriptKey.lastIndexOf('/');
    if (lastSlash >= 0) {
      return scriptKey.substring(0, lastSlash);
    }
    return "";
  }

  /** Get the filename portion of a script key. */
  private String getFileName(String scriptKey) {
    int lastSlash = scriptKey.lastIndexOf('/');
    if (lastSlash >= 0) {
      return scriptKey.substring(lastSlash + 1);
    }
    return scriptKey;
  }
}
