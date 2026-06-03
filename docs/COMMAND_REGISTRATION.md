# Command Registration in JavaSkript

This document explains how JavaSkript automatically registers commands for scripts without needing `plugin.yml` entries.

---

## Overview

JavaSkript uses **dynamic command registration** to automatically register commands from scripts at runtime. This means:
- ✅ No `plugin.yml` entries needed
- ✅ Commands are registered when scripts load
- ✅ Commands are unregistered when scripts unload
- ✅ Tab completion works automatically
- ✅ Works on Paper 1.19.4+

---

## How It Works

### 1. Script Detection

When a script class implements `CommandExecutor`, JavaSkript automatically detects it:

```java
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class FlyCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Command logic here
        return true;
    }
}
```

### 2. Command Name Extraction

The command name is automatically derived from the class name:

| Class Name | Command Name | How It Works |
|------------|--------------|--------------|
| `FlyCommand` | `/fly` | Removes "Command" suffix, converts to lowercase |
| `HealCommand` | `/heal` | Removes "Command" suffix, converts to lowercase |
| `TeleportCmd` | `/teleport` | Removes "Cmd" suffix, converts to lowercase |
| `Warp` | `/warp` | Uses class name as-is, converts to lowercase |

**Code in `ScriptInstance.java`:**
```java
String className = scriptClass.getSimpleName();
String commandName = className.toLowerCase()
    .replace("command", "")
    .replace("cmd", "");

if (commandName.isEmpty()) {
    commandName = className.toLowerCase();
}
```

### 3. Dynamic Registration

JavaSkript uses the `DynamicCommandRegistry` to register commands:

**Step-by-step process:**

1. **Create PluginCommand**: Uses reflection to create a `PluginCommand` instance
   ```java
   Constructor<PluginCommand> constructor = 
       PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
   constructor.setAccessible(true);
   PluginCommand command = constructor.newInstance(commandName, plugin);
   ```

2. **Set Executor**: Assigns the script as the command executor
   ```java
   command.setExecutor(executor);
   ```

3. **Set Tab Completer**: If the script implements `TabCompleter`, it's automatically set
   ```java
   if (executor instanceof TabCompleter) {
       command.setTabCompleter((TabCompleter) executor);
   }
   ```

4. **Register with CommandMap**: Adds the command to Bukkit's command map
   ```java
   CommandMap commandMap = getCommandMap();
   commandMap.register(plugin.getName().toLowerCase(), command);
   ```

5. **Sync to Clients**: Updates tab completion for all online players
   ```java
   Bukkit.getOnlinePlayers().forEach(player -> player.updateCommands());
   ```

---

## Complete Example

### Basic Command

```java
import dev.mukulx.javaskript.script.FoliaSupport;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import java.util.List;

@FoliaSupport
public class FlyCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command"));
            return true;
        }
        
        boolean fly = !player.getAllowFlight();
        player.setAllowFlight(fly);
        player.sendMessage(Component.text("Flight: " + (fly ? "ON" : "OFF")));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }
}
```

**Result:** `/fly` command is automatically registered!

### Command with Arguments

```java
import dev.mukulx.javaskript.script.FoliaSupport;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import java.util.List;
import java.util.stream.Collectors;

@FoliaSupport
public class HealCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player target;
        
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Usage: /heal <player>"));
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[0]));
                return true;
            }
        }
        
        target.setHealth(target.getMaxHealth());
        target.setFoodLevel(20);
        target.sendMessage(Component.text("You have been healed!"));
        
        if (!sender.equals(target)) {
            sender.sendMessage(Component.text("Healed " + target.getName()));
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
```

**Result:** `/heal` and `/heal <player>` with tab completion!

---

## Lifecycle

### When Scripts Load

1. Script is compiled
2. Class is loaded
3. Instance is created
4. JavaSkript checks if it implements `CommandExecutor`
5. If yes, command name is extracted from class name
6. Command is registered via `DynamicCommandRegistry`
7. Tab completion is synced to all players

### When Scripts Unload

1. Script unload is triggered (via `/js unload`, `/js reload`, or server shutdown)
2. Command is unregistered from CommandMap
3. All aliases are removed
4. Tab completion is synced to all players
5. Script instance is destroyed

---

## Technical Details

### Why No plugin.yml?

JavaSkript uses Paper's modern plugin system with `paper-plugin.yml` instead of the legacy `plugin.yml`.

Traditional Bukkit/Spigot plugins require commands to be declared in `plugin.yml`:

```yaml
commands:
  fly:
    description: Toggle flight
    usage: /fly
```

**Problems with this approach:**
- ❌ Static - can't add commands at runtime
- ❌ Requires plugin reload to add new commands
- ❌ Scripts would need their own plugin.yml entries
- ❌ Not flexible for dynamic scripting

**JavaSkript's solution:**
- ✅ Uses Paper's modern `paper-plugin.yml` format
- ✅ Uses reflection to create `PluginCommand` instances
- ✅ Registers directly with Bukkit's `CommandMap`
- ✅ Fully dynamic - commands appear/disappear with scripts
- ✅ No command declarations needed in YAML

### Reflection Magic

JavaSkript uses reflection to access internal Bukkit APIs:

```java
// Create PluginCommand (normally only Bukkit can do this)
Constructor<PluginCommand> constructor = 
    PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
constructor.setAccessible(true);
PluginCommand command = constructor.newInstance(commandName, plugin);

// Get CommandMap (normally private)
Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
commandMapField.setAccessible(true);
CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

// Register command
commandMap.register(pluginName, command);
```

### Command Map Structure

Bukkit's `CommandMap` stores all server commands:

```
CommandMap (SimpleCommandMap)
├── knownCommands (Map<String, Command>)
│   ├── "fly" → FlyCommand
│   ├── "heal" → HealCommand
│   ├── "javaskript:fly" → FlyCommand (with plugin prefix)
│   └── ...
```

When a player types `/fly`, Bukkit:
1. Looks up "fly" in `knownCommands`
2. Finds the registered `Command` object
3. Calls its `execute()` method
4. Which calls your script's `onCommand()` method

---

## Permissions

Commands don't automatically have permissions. You can check permissions in your script:

```java
@Override
public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!sender.hasPermission("myscript.fly")) {
        sender.sendMessage(Component.text("No permission!"));
        return true;
    }
    
    // Command logic...
    return true;
}
```

To register permissions, use the `DynamicPermissionRegistry` (see API.md).

---

## Aliases

Currently, JavaSkript doesn't support command aliases from class names. If you need aliases, you can:

1. **Check the label in onCommand:**
   ```java
   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
       // label will be "fly" or whatever alias was used
       if (label.equalsIgnoreCase("f")) {
           // Handle alias
       }
       return true;
   }
   ```

2. **Register multiple commands** (create multiple script classes)

---

## Troubleshooting

### Command Not Registering

**Check:**
1. Does your class implement `CommandExecutor`?
2. Is the class name valid? (e.g., `FlyCommand`, not `Fly Command`)
3. Check console for errors during script load
4. Try `/js reload` to reload the script

### Command Conflicts

If another plugin has the same command:
- JavaSkript will try to override it
- Use the plugin prefix: `/javaskript:fly`
- Or rename your script class

### Tab Completion Not Working

**Check:**
1. Does your class implement `TabCompleter`?
2. Is `onTabComplete()` returning a valid list?
3. Try reconnecting to the server

---

## Comparison with Other Systems

### Skript
```
command /fly:
    permission: skript.fly
    trigger:
        toggle flight of player
```

### JavaSkript
```java
public class FlyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Full Java power!
    }
}
```

**Advantages:**
- ✅ Full Java language features
- ✅ Type safety
- ✅ IDE autocomplete
- ✅ Compile-time error checking
- ✅ Access to all Bukkit APIs

---

## Best Practices

1. **Name your classes clearly**: `FlyCommand`, not `Fly` or `FlyScript`
2. **Implement TabCompleter**: Provides better UX
3. **Check permissions**: Don't rely on external permission plugins
4. **Validate arguments**: Check `args.length` before accessing
5. **Return true**: Always return `true` from `onCommand()` to prevent usage message
6. **Use Components**: Use Adventure API for colored messages

---

## See Also

- [API.md](API.md) - Full API documentation
- [QUICKSTART.md](QUICKSTART.md) - Getting started guide
- [EXAMPLES.md](EXAMPLES.md) - More command examples
- [FlyCommand.java](../src/main/resources/examples/FlyCommand.java) - Example script
- [HealCommand.java](../src/main/resources/examples/HealCommand.java) - Example script

---

**Last Updated:** 2026-05-30
