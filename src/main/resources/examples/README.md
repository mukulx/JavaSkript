# JavaSkript Example Scripts

This folder contains example scripts demonstrating various JavaSkript features.

## Quick Start

Copy any example to `plugins/JavaSkript/scripts/` on your server to use it.

## Available Examples

### Basic Examples

- **WelcomeScript.java** - Simple welcome message on player join
- **HealCommand.java** - Basic command that heals the player
- **FlyCommand.java** - Toggle flight with permission checking

### API Examples

- **ConfigExample.java** - Multiple config files (config.yml, messages.yml)
- **DatabaseExample.java** - SQLite database with player tracking
- **SchedulerExample.java** - Delayed and repeating tasks
- **GUIExample.java** - Interactive inventory GUIs (small, large, shop examples)
- **PlaceholderExample.java** - Custom PlaceholderAPI placeholders
- **PermissionExample.java** - Dynamic permission registration

### Advanced Examples

- **MultiClassExample.java** - Multiple classes in one file (managers, utilities, data classes)
- **ExtraInventory.java** - Complete inventory storage system with HikariCP + SQLite

### Compatibility Examples

- **FoliaCompatibleExample.java** - Script marked with @FoliaSupport
- **PaperOnlyExample.java** - Script marked with @PaperOnly

## How to Use

1. Choose an example script
2. Copy it to `plugins/JavaSkript/scripts/` on your server
3. The script loads automatically (or use `/js reload`)
4. Test the functionality in-game

## Learning Path

**Beginners:**
1. Start with `WelcomeScript.java` - Simple event listener
2. Try `HealCommand.java` - Basic command
3. Explore `FlyCommand.java` - Command with permissions

**Intermediate:**
4. Learn `ConfigExample.java` - Config management
5. Try `DatabaseExample.java` - Data persistence
6. Explore `SchedulerExample.java` - Task scheduling
7. Study `GUIExample.java` - Interactive menus with multiple GUI types

**Advanced:**
8. Analyze `MultiClassExample.java` - Multiple classes in one file
9. Study `ExtraInventory.java` - Production-ready inventory system with HikariCP

## Features Demonstrated

| Feature | Examples |
|---------|----------|
| Event Listeners | WelcomeScript, DatabaseExample, FoliaCompatibleExample, ExtraInventory |
| Commands | HealCommand, FlyCommand, ConfigExample, GUIExample, ExtraInventory |
| Tab Completion | FlyCommand, GUIExample, all command examples |
| Permissions | FlyCommand, PermissionExample, ExtraInventory |
| Config Files | ConfigExample |
| Database (SQLite) | DatabaseExample, ExtraInventory |
| Database (HikariCP) | ExtraInventory |
| Scheduler | SchedulerExample |
| GUI Builder | GUIExample (small, large, shop examples), ExtraInventory |
| PlaceholderAPI | PlaceholderExample |
| Multiple Classes | MultiClassExample (managers, utilities, data classes) |
| Maven Dependencies | ExtraInventory (HikariCP) |
| Dupe Prevention | ExtraInventory |
| Folia Support | All examples (marked with @FoliaSupport) |

## Notes

- All examples include null safety checks
- All examples are Folia-compatible (except PaperOnlyExample)
- Commands are registered automatically (no plugin.yml needed)
- APIs are auto-injected into scripts

## Documentation

For detailed documentation, see:
- [Quick Start Guide](../../docs/QUICKSTART.md) - Get started in 5 minutes
- [Tutorial](../../docs/TUTORIAL.md) - Step-by-step guide
- [API Documentation](../../docs/API.md) - Complete API reference
- [Examples Guide](../../docs/EXAMPLES.md) - More examples and patterns
- [Dependencies Guide](../../docs/DEPENDENCIES.md) - Using Maven libraries

## Support

- Check console for errors
- Use `/js info <script>` to see script status
- Use `/js reload <script>` to reload a specific script
- Read [Troubleshooting Guide](../../docs/TROUBLESHOOTING.md)

---

**Happy scripting!**
