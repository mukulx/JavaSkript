<p align="center">
  <img src="assets/banner.jpg" alt="JavaSkript Banner" width="800">
</p>

#  JavaSkript

> **Work in Progress** - This project is under active development. Features may change and bugs may exist.

JavaSkript is a scripting engine for Paper and Folia which allows developers to write scripts in .java files with hot-reload and automatic compilation, without the hassle of setting up a new project each time.

If you already know Java, JavaSkript makes rapid prototyping and testing much faster. If you're learning Java, JavaSkript provides an easy way to experiment with the Paper API without the complexity of traditional plugin development.

> **🔒 SECURITY WARNING** - Scripts run with plugin-level access. Review scripts before loading them.

## Requirements

- Paper 1.21.1+ or Folia 1.21.1+ (Bukkit/Spigot NOT supported)
- Java 21 or higher

## Features

- Write Java in `.java` files - no IDE or build tools required
- Hot-reload - scripts auto-reload when files change
- Dynamic Maven dependencies - use any library from Maven Central
- Full Paper and Folia support
- Built-in APIs: Scheduler, Config, Database (SQLite), GUI, PlaceholderAPI
- Dynamic commands and permissions - no plugin.yml needed
- Multiple classes per file
- High performance - compiles to native bytecode

## Quick Start

1. Build: `./gradlew clean build`
2. Place `build/libs/JavaSkript-1.0.0.jar` in `plugins/` folder
3. Restart server
4. Write scripts in `plugins/JavaSkript/scripts/`

### Example Script

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
        if (event == null || event.getPlayer() == null) return;
        
        event.getPlayer().sendMessage(
            Component.text("Welcome!").color(NamedTextColor.GOLD)
        );
    }
}
```

Save the file - it loads automatically!

## Documentation

- **[Quick Start](docs/QUICKSTART.md)** - Get started in 5 minutes
- **[Tutorial](docs/TUTORIAL.md)** - Step-by-step guide
- **[API Documentation](docs/API.md)** - Complete API reference
- **[Examples](docs/EXAMPLES.md)** - Code examples and patterns
- **[Lifecycle](docs/LIFECYCLE.md)** - Resource management
- **[Folia Support](docs/FOLIA.md)** - Folia compatibility guide
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues

## Commands

All commands require `javaskript.admin` permission (default: op). Aliases: `/js`, `/jskript`

- `/js reload [script]` - Reload scripts
- `/js list` - List all scripts
- `/js load <script>` - Load a script
- `/js unload <script>` - Unload a script
- `/js enable/disable <script>` - Enable/disable scripts
- `/js info <script>` - Show script info
- `/js debug` - Toggle debug mode

## Example Scripts

Ready-to-use examples in [src/main/resources/examples](src/main/resources/examples/):

- Commands: HealCommand, FlyCommand
- Events: WelcomeScript, Example
- Config: ConfigExample
- Database: DatabaseExample, ExtraInventory (HikariCP)
- GUI: GUIExample
- Scheduler: SchedulerExample
- PlaceholderAPI: PlaceholderExample
- Permissions: PermissionExample
- Multi-class: MultiClassExample

## API for Other Plugins

```java
JavaSkriptPlugin plugin = (JavaSkriptPlugin) Bukkit.getPluginManager().getPlugin("JavaSkript");
JavaSkriptAPI api = plugin.getAPI();

// Load a script
api.loadScript(new File("path/to/script.java"));

// Check if loaded
boolean loaded = api.isScriptLoaded("MyScript.java");

// Get all scripts
Map<String, ScriptInstance> scripts = api.getAllScripts();
```

## Contributing

Contributions welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Links

- [Paper Documentation](https://docs.papermc.io/)
- [Paper API Javadocs](https://jd.papermc.io/)
- [Adventure API](https://docs.advntr.dev/)

## License

GNU General Public License v3.0 - see [LICENSE](LICENSE) file.
