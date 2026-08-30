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

- `/js reload [script|folder|all]` - Reload all scripts, a specific script, or an entire folder
- `/js list` - Display directory tree of all scripts (loaded and disabled)
- `/js load <script|folder>` - Load a specific script or entire folder
- `/js unload <script|folder>` - Unload a specific script or entire folder
- `/js enable <script|folder>` - Enable a disabled script or folder
- `/js disable <script|folder>` - Disable a script or folder
- `/js info <script>` - Show script details & Folia status

**Aliases:** `/javaskript`, `/jskript`

## Folder Organization & Disabling Scripts

### Subfolders
You can freely organize your scripts into subdirectories inside `plugins/JavaSkript/scripts/`:
```text
plugins/JavaSkript/scripts/
├── pvp/
│   ├── CombatLog.java
│   └── KillStreaks.java
├── admin/
│   └── Moderation.java
└── examples/
    ├── -HealCommand.java
    ├── -FlyCommand.java
    └── ...
```

Run commands using relative paths or simple names:
- `/js reload pvp/CombatLog` or `/js reload CombatLog`
- `/js reload pvp/` (reloads all scripts in the `pvp` folder)

### Disabling Scripts
There are three easy ways to disable scripts:
1. **Filename / Folder Prefix `-`**:
   - Prefixing a file with `-` (e.g. `-CombatLog.java`) disables it.
   - Prefixing a folder with `-` (e.g. `-pvp/`) disables all scripts inside it.
   - Using `/js disable <script|folder>` will automatically rename it with a `-` prefix.
2. **In-Code Annotation or Comment**:
   - Add `@Disabled` or `@Disabled("Under maintenance")` above your class.
   - Or add `// @disabled` anywhere at the top of the file.
3. **Default Examples**:
   - All 26+ built-in examples are extracted into `scripts/examples/` with `-` prefixes (e.g., `-HealCommand.java`), keeping them disabled by default so your server stays clean.
   - To try an example, enable it with `/js enable examples/HealCommand` or remove the leading `-`!

## Next Steps

- Explore `plugins/JavaSkript/scripts/examples/` for 26+ ready-to-use examples
- Read [API.md](API.md) for full API documentation
- Read [COMMAND_REGISTRATION.md](COMMAND_REGISTRATION.md) to understand dynamic commands
- Read [DEPENDENCIES.md](DEPENDENCIES.md) to use Maven libraries
- Read [FOLIA.md](FOLIA.md) for Folia compatibility
- Read [PERFORMANCE.md](PERFORMANCE.md) for optimization tips
- Read [EXAMPLES.md](EXAMPLES.md) for more examples
