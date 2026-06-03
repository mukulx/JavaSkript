# Quick Start

Get JavaSkript running in 5 minutes.

## Installation

1. Build `JavaSkript.jar` using `./gradlew build`
2. Put it in `plugins/` folder
3. Restart server
4. Scripts folder created at `plugins/JavaSkript/scripts/`

## Your First Script

Create `plugins/JavaSkript/scripts/Welcome.java`:

```java
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class Welcome implements Listener {
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        player.sendMessage(
            Component.text("Welcome!").color(NamedTextColor.GOLD)
        );
    }
}
```

Save the file. It loads automatically. Done.

## Create a Command

Create `FlyCommand.java`:

```java
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.List;

@FoliaSupport
public class FlyCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        
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

Command `/fly` is now registered. No plugin.yml needed.

## Use APIs

APIs are auto-injected:

```java
import dev.mukulx.javaskript.api.*;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class MyScript implements Listener {
    
    private ScriptScheduler scheduler;  // Auto-injected
    private ScriptConfig config;        // Auto-injected
    private DatabaseHelper database;    // Auto-injected
    
    // Use them anywhere
}
```

## Commands

- `/js reload [script]` - Reload all scripts or specific script
- `/js list` - List all scripts (loaded and disabled)
- `/js load <script>` - Load a specific script
- `/js unload <script>` - Unload a specific script
- `/js enable <script>` - Enable a disabled script
- `/js disable <script>` - Disable a script
- `/js info <script>` - Show script information

**Aliases:** `/javaskript`, `/jskript`

## Next Steps

- Check `plugins/JavaSkript/scripts/` for example scripts
- Read [API.md](API.md) for full API documentation
- Read [COMMAND_REGISTRATION.md](COMMAND_REGISTRATION.md) to understand dynamic commands
- Read [DEPENDENCIES.md](DEPENDENCIES.md) to use Maven libraries
- Read [FOLIA.md](FOLIA.md) for Folia compatibility
- Read [PERFORMANCE.md](PERFORMANCE.md) for optimization tips
- Read [EXAMPLES.md](EXAMPLES.md) for more examples
