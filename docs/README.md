# JavaSkript Documentation

Complete documentation for JavaSkript - write Java code in script files for Minecraft Paper and Folia servers.

## Getting Started

- **[Quick Start Guide](QUICKSTART.md)** - Get up and running in 5 minutes
- **[Tutorial](TUTORIAL.md)** - Complete step-by-step guide for beginners
- **[Examples](EXAMPLES.md)** - Comprehensive examples for various use cases

## Reference

- **[Script Lifecycle](LIFECYCLE.md)** - onEnable(), onDisable(), and resource management
- **[API Documentation](API.md)** - Complete API reference for all JavaSkript features
- **[Command Registration](COMMAND_REGISTRATION.md)** - How dynamic command registration works
- **[Dynamic Dependencies](DEPENDENCIES.md)** - Use any Maven library in your scripts
- **[Multiple Classes](MULTIPLE_CLASSES.md)** - Organize code with multiple classes per file
- **[Folia Compatibility](FOLIA.md)** - Guide for writing Folia-compatible scripts
- **[Performance](PERFORMANCE.md)** - Optimization guide for many scripts
- **[Troubleshooting](TROUBLESHOOTING.md)** - Common issues and solutions

## Other

- **[Changelog](CHANGELOG.md)** - Version history and changes

## Quick Links

### For Beginners
1. Start with [Quick Start Guide](QUICKSTART.md)
2. Follow the [Tutorial](TUTORIAL.md)
3. Check [Examples](EXAMPLES.md) for inspiration
4. Try [example scripts](../src/main/resources/examples/) in your server

### For Experienced Developers
1. Read [API Documentation](API.md)
2. Check [Folia Compatibility](FOLIA.md) if using Folia
3. Browse [Examples](EXAMPLES.md) for advanced patterns
4. Study [example scripts](../src/main/resources/examples/) source code

### Having Issues?
1. Check [Troubleshooting](TROUBLESHOOTING.md)
2. Read error messages carefully (they explain the problem!)
3. Check example scripts in `plugins/JavaSkript/scripts/`

## Documentation Structure

```
docs/
├── README.md                 # This file - documentation index
├── QUICKSTART.md             # 5-minute quick start guide
├── TUTORIAL.md               # Complete tutorial for beginners
├── LIFECYCLE.md              # Script lifecycle and resource management
├── EXAMPLES.md               # Comprehensive examples
├── API.md                    # Complete API reference
├── COMMAND_REGISTRATION.md   # Dynamic command registration explained
├── DEPENDENCIES.md           # Maven dependency system
├── MULTIPLE_CLASSES.md       # Multiple classes in one file
├── FOLIA.md                  # Folia compatibility guide
├── PERFORMANCE.md            # Performance optimization guide
└── TROUBLESHOOTING.md        # Common issues and solutions

src/main/resources/examples/ (on GitHub)
├── README.md                 # Example scripts index
├── Example.java              # Welcome message for ops
├── WelcomeScript.java        # Simple welcome message
├── HealCommand.java          # Basic heal command
├── FlyCommand.java           # Flight toggle command
├── LifecycleExample.java     # onEnable() and onDisable() demonstration
├── ConfigExample.java        # Config management
├── DatabaseExample.java      # SQLite database
├── SchedulerExample.java     # Task scheduling
├── GUIExample.java           # Interactive GUIs
├── PlaceholderExample.java   # Custom placeholders
├── PermissionExample.java    # Dynamic permissions
├── MultiClassExample.java    # Multiple classes in one file
└── ExtraInventory.java       # HikariCP + SQLite inventory storage
```

## Key Features

- **Write Java in script files** - No compilation or IDE setup needed
- **Hot reload** - Scripts auto-reload on save (configurable)
- **Multiple classes per file** - Organize with managers and utilities
- **Dynamic Maven dependencies** - Use any library from Maven Central
- **Full Paper/Folia support** - Works on both platforms with automatic detection
- **Dynamic commands** - Commands register automatically, no plugin.yml needed
- **Dynamic permissions** - LuckPerms-style permission registration
- **Built-in APIs** - Scheduler, Config, Database (SQLite), GUI Builder, PlaceholderAPI
- **Auto-injection** - APIs automatically injected into scripts
- **Compilation caching** - 10-50x faster reloads for unchanged scripts
- **Parallel loading** - Fast startup with many scripts
- **Folder validation** - Enforces proper data storage in script-data folder
- **Null-safe** - Built-in error handling and helpful error messages

## Example Script

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

## Commands

- `/js reload [script]` - Reload all scripts or specific script
- `/js list` - List all scripts (loaded and disabled)
- `/js load <script>` - Load a specific script
- `/js unload <script>` - Unload a specific script
- `/js enable <script>` - Enable a disabled script
- `/js disable <script>` - Disable a script
- `/js info <script>` - Show script information

**Aliases:** `/javaskript`, `/jskript`

## Support

- **GitHub Issues** - Report bugs and request features
- **Example Scripts** - Check `plugins/JavaSkript/scripts/` folder
- **Documentation** - You're reading it!

---

