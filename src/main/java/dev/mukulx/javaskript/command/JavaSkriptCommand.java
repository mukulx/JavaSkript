package dev.mukulx.javaskript.command;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptInstance;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
      case "list" -> handleList(sender);
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
      // Reload specific script
      String scriptName = target;
      if (!scriptName.endsWith(".java")) {
        scriptName += ".java";
      }

      sender.sendMessage(
          Component.text("Reloading script: " + scriptName).color(NamedTextColor.YELLOW));

      File scriptFile = new File(plugin.getScriptManager().getScriptsFolder(), scriptName);

      if (!scriptFile.exists()) {
        sender.sendMessage(
            Component.text("Script not found: " + scriptName).color(NamedTextColor.RED));
        return;
      }

      plugin.getScriptManager().unloadScript(scriptName);
      boolean success = plugin.getScriptManager().loadScript(scriptFile);

      if (success) {
        sender.sendMessage(
            Component.text("Successfully reloaded: " + scriptName).color(NamedTextColor.GREEN));
      } else {
        sender.sendMessage(
            Component.text("Failed to reload: " + scriptName).color(NamedTextColor.RED));
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

        // Small delay to ensure cleanup
        plugin
            .getServer()
            .getScheduler()
            .runTaskLater(
                plugin,
                () -> {
                  plugin.getScriptManager().loadAllScripts();
                  int count = plugin.getScriptManager().getLoadedScripts().size();
                  sender.sendMessage(
                      Component.text("Successfully restarted " + count + " script(s)!")
                          .color(NamedTextColor.GREEN));
                },
                20L); // 1 second delay
      } catch (Exception e) {
        sender.sendMessage(
            Component.text("Error restarting scripts: " + e.getMessage())
                .color(NamedTextColor.RED));
        plugin.getLogger().severe("Error restarting scripts: " + e.getMessage());
      }
    } else {
      // Restart specific script
      String scriptName = target;
      if (!scriptName.endsWith(".java")) {
        scriptName += ".java";
      }

      sender.sendMessage(
          Component.text("Restarting script: " + scriptName).color(NamedTextColor.YELLOW));

      File scriptFile = new File(plugin.getScriptManager().getScriptsFolder(), scriptName);

      if (!scriptFile.exists()) {
        sender.sendMessage(
            Component.text("Script not found: " + scriptName).color(NamedTextColor.RED));
        return;
      }

      // Unload
      plugin.getScriptManager().unloadScript(scriptName);
      sender.sendMessage(Component.text("Script unloaded").color(NamedTextColor.GRAY));

      // Reload after delay
      String finalScriptName = scriptName;
      plugin
          .getServer()
          .getScheduler()
          .runTaskLater(
              plugin,
              () -> {
                boolean success = plugin.getScriptManager().loadScript(scriptFile);

                if (success) {
                  sender.sendMessage(
                      Component.text("Successfully restarted: " + finalScriptName)
                          .color(NamedTextColor.GREEN));
                } else {
                  sender.sendMessage(
                      Component.text("Failed to restart: " + finalScriptName)
                          .color(NamedTextColor.RED));
                }
              },
              20L); // 1 second delay
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

  private void handleList(CommandSender sender) {
    Map<String, ScriptInstance> scripts = plugin.getScriptManager().getLoadedScripts();
    var disabledScripts = plugin.getScriptManager().getDisabledScripts();

    sender.sendMessage(Component.text("=== JavaSkript Scripts ===").color(NamedTextColor.GOLD));

    if (scripts.isEmpty() && disabledScripts.isEmpty()) {
      sender.sendMessage(Component.text("No scripts found.").color(NamedTextColor.YELLOW));
      return;
    }

    if (!scripts.isEmpty()) {
      sender.sendMessage(
          Component.text("Loaded Scripts (" + scripts.size() + "):").color(NamedTextColor.GREEN));
      for (Map.Entry<String, ScriptInstance> entry : scripts.entrySet()) {
        ScriptInstance instance = entry.getValue();
        String className = instance.getScriptClass().getSimpleName();
        sender.sendMessage(
            Component.text("  ✓ " + entry.getKey() + " (" + className + ")")
                .color(NamedTextColor.GREEN));
      }
    }

    if (!disabledScripts.isEmpty()) {
      sender.sendMessage(
          Component.text("Disabled Scripts (" + disabledScripts.size() + "):")
              .color(NamedTextColor.RED));
      for (String scriptName : disabledScripts) {
        sender.sendMessage(Component.text("  ✗ " + scriptName).color(NamedTextColor.RED));
      }
    }
  }

  private void handleLoad(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript load <script>").color(NamedTextColor.RED));
      return;
    }

    String scriptName = args[1];
    if (!scriptName.endsWith(".java")) {
      scriptName += ".java";
    }

    // Check if already loaded
    if (plugin.getScriptManager().getScript(scriptName) != null) {
      sender.sendMessage(
          Component.text("Script already loaded: " + scriptName).color(NamedTextColor.RED));
      sender.sendMessage(
          Component.text("Use /js reload " + scriptName + " to reload it")
              .color(NamedTextColor.YELLOW));
      return;
    }

    File scriptFile = new File(plugin.getScriptManager().getScriptsFolder(), scriptName);

    if (!scriptFile.exists()) {
      sender.sendMessage(
          Component.text("Script not found: " + scriptName).color(NamedTextColor.RED));
      return;
    }

    sender.sendMessage(
        Component.text("Loading script: " + scriptName).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().loadScript(scriptFile);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully loaded: " + scriptName).color(NamedTextColor.GREEN));
    } else {
      sender.sendMessage(Component.text("Failed to load: " + scriptName).color(NamedTextColor.RED));
    }
  }

  private void handleUnload(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript unload <script>").color(NamedTextColor.RED));
      return;
    }

    String scriptName = args[1];
    if (!scriptName.endsWith(".java")) {
      scriptName += ".java";
    }

    sender.sendMessage(
        Component.text("Unloading script: " + scriptName).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().unloadScript(scriptName);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully unloaded: " + scriptName).color(NamedTextColor.GREEN));
    } else {
      sender.sendMessage(
          Component.text("Script not loaded: " + scriptName).color(NamedTextColor.RED));
    }
  }

  private void handleInfo(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript info <script>").color(NamedTextColor.RED));
      return;
    }

    String scriptName = args[1];
    if (!scriptName.endsWith(".java")) {
      scriptName += ".java";
    }

    ScriptInstance instance = plugin.getScriptManager().getScript(scriptName);

    if (instance == null) {
      // Check if disabled
      if (plugin.getScriptManager().isScriptDisabled(scriptName)) {
        sender.sendMessage(
            Component.text("Script is disabled: " + scriptName).color(NamedTextColor.RED));
        sender.sendMessage(
            Component.text("Use /js enable " + scriptName + " to enable it")
                .color(NamedTextColor.YELLOW));
      } else {
        sender.sendMessage(
            Component.text("Script not loaded: " + scriptName).color(NamedTextColor.RED));
      }
      return;
    }

    sender.sendMessage(Component.text("Script Information:").color(NamedTextColor.GOLD));
    sender.sendMessage(
        Component.text("  Name: " + instance.getName()).color(NamedTextColor.YELLOW));
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
          Component.text("Usage: /javaskript enable <script>").color(NamedTextColor.RED));
      return;
    }

    String scriptName = args[1];
    if (!scriptName.endsWith(".java")) {
      scriptName += ".java";
    }

    // Check if script file exists
    File scriptFile = new File(plugin.getScriptManager().getScriptsFolder(), scriptName);
    if (!scriptFile.exists()) {
      sender.sendMessage(
          Component.text("Script not found: " + scriptName).color(NamedTextColor.RED));
      return;
    }

    // Check if already enabled
    if (!plugin.getScriptManager().isScriptDisabled(scriptName)) {
      sender.sendMessage(
          Component.text("Script is already enabled: " + scriptName).color(NamedTextColor.YELLOW));
      return;
    }

    sender.sendMessage(
        Component.text("Enabling script: " + scriptName).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().enableScript(scriptName);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully enabled: " + scriptName).color(NamedTextColor.GREEN));
    } else {
      sender.sendMessage(
          Component.text("Failed to enable: " + scriptName).color(NamedTextColor.RED));
    }
  }

  private void handleDisable(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          Component.text("Usage: /javaskript disable <script>").color(NamedTextColor.RED));
      return;
    }

    String scriptName = args[1];
    if (!scriptName.endsWith(".java")) {
      scriptName += ".java";
    }

    // Check if script file exists
    File scriptFile = new File(plugin.getScriptManager().getScriptsFolder(), scriptName);
    if (!scriptFile.exists()) {
      sender.sendMessage(
          Component.text("Script not found: " + scriptName).color(NamedTextColor.RED));
      return;
    }

    // Check if already disabled
    if (plugin.getScriptManager().isScriptDisabled(scriptName)) {
      sender.sendMessage(
          Component.text("Script is already disabled: " + scriptName).color(NamedTextColor.YELLOW));
      return;
    }

    sender.sendMessage(
        Component.text("Disabling script: " + scriptName).color(NamedTextColor.YELLOW));

    boolean success = plugin.getScriptManager().disableScript(scriptName);

    if (success) {
      sender.sendMessage(
          Component.text("Successfully disabled: " + scriptName).color(NamedTextColor.GREEN));
      sender.sendMessage(
          Component.text("Script will not load until enabled again").color(NamedTextColor.GRAY));
    } else {
      sender.sendMessage(
          Component.text("Failed to disable: " + scriptName).color(NamedTextColor.RED));
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
        Component.text("  /js load <script> - Load a script").color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js unload <script> - Unload a script").color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js enable <script> - Enable a disabled script")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js disable <script> - Disable a script").color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js info <script> - Show script information")
            .color(NamedTextColor.YELLOW));
    sender.sendMessage(
        Component.text("  /js debug - Toggle debug mode").color(NamedTextColor.YELLOW));
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

    if (args.length == 2) {
      String subCommand = args[0].toLowerCase();

      if (subCommand.equals("reload") || subCommand.equals("restart")) {
        // Suggest "all" or loaded scripts
        List<String> suggestions = new ArrayList<>();
        suggestions.add("all");
        suggestions.addAll(
            plugin.getScriptManager().getLoadedScripts().keySet().stream()
                .map(s -> s.replace(".java", ""))
                .collect(Collectors.toList()));

        return suggestions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
            .collect(Collectors.toList());
      }

      if (subCommand.equals("unload")
          || subCommand.equals("info")
          || subCommand.equals("disable")) {
        // Suggest loaded scripts
        return plugin.getScriptManager().getLoadedScripts().keySet().stream()
            .map(s -> s.replace(".java", ""))
            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
            .collect(Collectors.toList());
      }

      if (subCommand.equals("enable")) {
        // Suggest disabled scripts
        return plugin.getScriptManager().getDisabledScripts().stream()
            .map(s -> s.replace(".java", ""))
            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
            .collect(Collectors.toList());
      }

      if (subCommand.equals("load")) {
        // Suggest all scripts in folder that are not disabled
        File[] files = plugin.getScriptManager().getScriptsFolder().listFiles();
        if (files != null) {
          return Arrays.stream(files)
              .filter(f -> f.getName().endsWith(".java"))
              .filter(f -> !plugin.getScriptManager().isScriptDisabled(f.getName()))
              .map(f -> f.getName().replace(".java", ""))
              .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
              .collect(Collectors.toList());
        }
      }
    }

    return List.of();
  }
}
