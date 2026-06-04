# JavaSkript API Documentation

Complete reference for all JavaSkript APIs available to scripts.

## Table of Contents

1. [ScriptScheduler](#scriptscheduler)
2. [ScriptConfig](#scriptconfig)
3. [DatabaseHelper](#databasehelper)
4. [GUI Builder](#gui-builder)
5. [PlaceholderHelper](#placeholderhelper)
6. [ActionBarHelper](#actionbarhelper)
7. [Annotations](#annotations)

---

## ScriptScheduler

Easy task scheduling without boilerplate code.

### Auto-Injection
```java
private ScriptScheduler scheduler; // Automatically injected!
```

### Methods

#### `runLater(Runnable task, long delayTicks)`
Run a task after a delay.
```java
scheduler.runLater(() -> {
    player.sendMessage("5 seconds later!");
}, 100L); // 100 ticks = 5 seconds
```

#### `runTimer(Runnable task, long delayTicks, long periodTicks)`
Run a task repeatedly.
```java
scheduler.runTimer(() -> {
    Bukkit.broadcast(Component.text("Every 10 seconds!"));
}, 0L, 200L);
```

#### `runAsync(Runnable task)`
Run a task asynchronously (off main thread).
```java
scheduler.runAsync(() -> {
    // Heavy computation here
    // Don't use Bukkit API!
});
```

#### `everySecond(Runnable task)`
Convenience method - runs every second.
```java
scheduler.everySecond(() -> {
    // Runs every second
});
```

#### `everyMinute(Runnable task)`
Runs every minute.

#### `everyHour(Runnable task)`
Runs every hour.

#### `cancelAll()`
Cancel all tasks scheduled by this script.

---

## ScriptConfig

Manage multiple YAML configuration files per script.

### Auto-Injection
```java
private ScriptConfig config; // Automatically injected!
```

### File Location
Config files are stored in: `plugins/JavaSkript/script-data/YourScriptName/`

### Methods

#### `getConfig(String fileName)`
Get or create a config file.
```java
FileConfiguration config = config.getConfig("config.yml");
FileConfiguration messages = config.getConfig("messages.yml");
```

#### `saveConfig(String fileName, FileConfiguration config)`
Save a config file.
```java
config.saveConfig("config.yml", cfg);
```

#### `getString(String fileName, String path, String defaultValue)`
Quick access to string values.
```java
String msg = config.getString("messages.yml", "welcome", "Welcome!");
```

#### `getInt(String fileName, String path, int defaultValue)`
Quick access to integer values.

#### `getBoolean(String fileName, String path, boolean defaultValue)`
Quick access to boolean values.

#### `set(String fileName, String path, Object value)`
Set a value and auto-save.
```java
config.set("config.yml", "enabled", true);
```

#### `getDataFolder()`
Get the script's data folder.
```java
File folder = config.getDataFolder();
```

#### `listConfigs()`
List all config files.
```java
List<String> configs = config.listConfigs();
```

---

## DatabaseHelper

Built-in SQLite database support with connection pooling.

### Auto-Injection
```java
private DatabaseHelper database; // Automatically injected!
// Alternative: private DatabaseHelper db;
```

### Database Location
Database file: `plugins/JavaSkript/script-data/YourScriptName/database.db`

### Methods

#### `connect()`
Connect to the database (auto-called when needed).
```java
database.connect();
```

#### `disconnect()`
Disconnect from the database (auto-called on script unload).

#### `createTable(String tableName, String... columns)`
Create a table if it doesn't exist.
```java
database.createTable("players",
    "uuid TEXT PRIMARY KEY",
    "name TEXT NOT NULL",
    "coins INTEGER DEFAULT 0",
    "last_seen INTEGER"
);
```

#### `insert(String tableName, Map<String, Object> data)`
Insert a row.
```java
Map<String, Object> data = new HashMap<>();
data.put("uuid", player.getUniqueId().toString());
data.put("name", player.getName());
data.put("coins", 100);
database.insert("players", data);
```

#### `update(String tableName, Map<String, Object> data, String where, Object... params)`
Update rows.
```java
Map<String, Object> data = new HashMap<>();
data.put("coins", 500);
database.update("players", data, "uuid = ?", player.getUniqueId().toString());
```

#### `delete(String tableName, String where, Object... params)`
Delete rows.
```java
database.delete("players", "coins < ?", 0);
```

#### `executeQuery(String sql, Object... params)`
Execute a SELECT query.
```java
List<Map<String, Object>> results = database.executeQuery(
    "SELECT * FROM players WHERE name = ?",
    "Steve"
);

for (Map<String, Object> row : results) {
    String name = (String) row.get("name");
    int coins = ((Number) row.get("coins")).intValue();
}
```

#### `executeUpdate(String sql, Object... params)`
Execute INSERT, UPDATE, DELETE, or DDL.
```java
int affected = database.executeUpdate(
    "UPDATE players SET coins = coins + ? WHERE uuid = ?",
    10, uuid
);
```

#### `querySingle(String sql, Object... params)`
Get a single value.
```java
Object count = database.querySingle("SELECT COUNT(*) FROM players");
int playerCount = ((Number) count).intValue();
```

#### `tableExists(String tableName)`
Check if a table exists.
```java
if (!database.tableExists("players")) {
    database.createTable("players", ...);
}
```

---

## GUI Builder

Create interactive inventory GUIs easily.

### GUI Sizes

JavaSkript supports chest GUIs with 1-6 rows:

| Rows | Slots | Use Case |
|------|-------|----------|
| 1 | 9 | Small menus, quick selections |
| 2 | 18 | Compact menus |
| 3 | 27 | Standard menus (most common) |
| 4 | 36 | Medium menus |
| 5 | 45 | Large menus |
| 6 | 54 | Full chest (maximum size) |

**Note:** Bukkit only supports chest-type inventories for custom GUIs. Other inventory types (furnace, brewing stand, etc.) cannot be used for custom menus.

### Classes

#### `GUI`
Main GUI class for creating chest inventories.

```java
import dev.mukulx.javaskript.api.gui.GUI;

// Create a 3-row chest GUI (27 slots)
GUI gui = new GUI("My Menu", 3);

// Or with Component for colored titles
GUI gui = new GUI(Component.text("My Menu").color(NamedTextColor.GOLD), 3);

// Create a 6-row GUI (54 slots - full chest)
GUI largeGui = new GUI("Large Menu", 6);
```

**Constructor:**
- `GUI(String title, int rows)` - Create GUI with string title
- `GUI(Component title, int rows)` - Create GUI with Component title
- `rows` must be between 1 and 6

**Setting Items:**
- `setItem(int slot, ItemStack item)` - Set an item in a slot
- `setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick)` - Set item with click handler

**Filling Methods:**
- `fill(ItemStack item)` - Fill all empty slots
- `fillBorder(ItemStack item)` - Fill border (edges only)
- `fillRow(int row, ItemStack item)` - Fill a specific row (0-5)
- `fillColumn(int column, ItemStack item)` - Fill a specific column (0-8)

**Configuration:**
- `setCancelAllClicks(boolean cancel)` - Cancel all clicks by default (default: true)
- `onClose(Consumer<InventoryCloseEvent> handler)` - Set close handler

**Actions:**
- `open(Player player)` - Open the GUI for a player
- `clear()` - Clear all items and handlers
- `update()` - Update GUI for all viewers
- `getInventory()` - Get the underlying Bukkit inventory

**Slot Numbering:**
```
Row 1:  0  1  2  3  4  5  6  7  8
Row 2:  9 10 11 12 13 14 15 16 17
Row 3: 18 19 20 21 22 23 24 25 26
Row 4: 27 28 29 30 31 32 33 34 35
Row 5: 36 37 38 39 40 41 42 43 44
Row 6: 45 46 47 48 49 50 51 52 53
```

#### `ItemBuilder`
Easy item creation with method chaining.

```java
import dev.mukulx.javaskript.api.gui.ItemBuilder;

var item = new ItemBuilder(Material.DIAMOND)
    .name("Diamond")
    .lore("Line 1", "Line 2")
    .amount(5)
    .glow()
    .build();
```

**Constructor:**
- `ItemBuilder(Material material)` - Create builder with material

**Display:**
- `name(String name)` - Set display name (plain text)
- `name(Component name)` - Set display name (Component)
- `lore(String... lines)` - Set lore (plain text)
- `lore(Component... lines)` - Set lore (Components)
- `lore(List<String> lines)` - Set lore (List)

**Properties:**
- `amount(int amount)` - Set item amount (1-64)
- `glow()` - Make item glow (adds fake enchantment)
- `enchant(Enchantment ench, int level)` - Add enchantment
- `unbreakable(boolean unbreakable)` - Set unbreakable
- `customModelData(int data)` - Set custom model data

**Flags:**
- `flags(ItemFlag... flags)` - Add item flags
- `hideAll()` - Hide all attributes/enchantments/etc

**Build:**
- `build()` - Build the ItemStack

### Example

```java
GUI gui = new GUI("Shop", 3);

var diamondItem = new ItemBuilder(Material.DIAMOND)
    .name(Component.text("Buy Diamonds", NamedTextColor.AQUA))
    .lore("Click to buy 5 diamonds", "Cost: 100 coins")
    .glow()
    .build();

gui.setItem(13, diamondItem, e -> {
    Player player = (Player) e.getWhoClicked();
    player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
    player.sendMessage("Purchased 5 diamonds");
    player.closeInventory();
});

gui.fillBorder(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
    .name(" ")
    .build());

gui.open(player);
```

### Multiple GUIs

You can create multiple GUIs and switch between them:

```java
private void openMainMenu(Player player) {
    GUI gui = new GUI("Main Menu", 3);
    
    var shopButton = new ItemBuilder(Material.EMERALD)
        .name("Shop")
        .build();
    
    gui.setItem(13, shopButton, e -> openShop(player));
    gui.open(player);
}

private void openShop(Player player) {
    GUI gui = new GUI("Shop", 3);
    
    // Add shop items...
    
    var backButton = new ItemBuilder(Material.ARROW)
        .name("Back")
        .build();
    
    gui.setItem(22, backButton, e -> openMainMenu(player));
    gui.open(player);
}
```

---

## PlaceholderHelper

Register custom PlaceholderAPI placeholders.

### Auto-Injection
```java
private PlaceholderHelper placeholders; // Automatically injected!
// Alternative: private PlaceholderHelper papi;
```

### Requirements
PlaceholderAPI plugin must be installed.

### Methods

#### `registerPlaceholder(String identifier, BiFunction<OfflinePlayer, String, String> handler)`
Register a dynamic placeholder.
```java
// Usage: %yourscript_health%
placeholders.registerPlaceholder("health", (player, params) -> {
    if (player == null || !player.isOnline()) return "N/A";
    return String.valueOf(player.getPlayer().getHealth());
});
```

#### `registerPlaceholder(String identifier, String value)`
Register a static placeholder.
```java
// Usage: %yourscript_server%
placeholders.registerPlaceholder("server", "My Server");
```

#### `unregisterPlaceholder(String identifier)`
Unregister a placeholder.

#### `unregisterAll()`
Unregister all placeholders (auto-called on script unload).

#### `parsePlaceholders(OfflinePlayer player, String text)`
Parse placeholders in text.
```java
String parsed = placeholders.parsePlaceholders(player, 
    "Health: %yourscript_health%");
```

#### `isPlaceholderAPIAvailable()`
Check if PlaceholderAPI is installed.
```java
if (placeholders.isPlaceholderAPIAvailable()) {
    // Register placeholders
}
```

---

## Permissions

JavaSkript provides dynamic permission registration, allowing scripts to register permissions at runtime without needing `plugin.yml` entries.

### Basic Permission Checking

The simplest way to check permissions is using Bukkit's built-in permission system:

```java
@Override
public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    // Check if sender has permission
    if (!sender.hasPermission("myscript.use")) {
        sender.sendMessage(Component.text("No permission!").color(NamedTextColor.RED));
        return true;
    }
    
    // Command logic here
    return true;
}
```

**Important:** If you check a permission that was never registered (not in any plugin), Bukkit's default behavior is:
- **Non-ops**: `hasPermission()` returns `false` (no permission)
- **Ops**: `hasPermission()` returns `true` (ops have all permissions by default)

This means if you don't register a permission at all, only ops can use the feature. This is usually the desired behavior for admin commands.

### When to Register Permissions

**You DON'T need to register permissions if:**
- You want only ops to have access (default behavior)
- You're okay with permission plugins not seeing the permission in their lists

**You SHOULD register permissions if:**
- You want non-ops to have access by default (`PermissionDefault.TRUE`)
- You want the permission to show up in permission plugin lists (LuckPerms, etc.)
- You want to create permission hierarchies with wildcards
- You want to document what permissions your script uses

### Setting Up Permissions with LuckPerms

To give players permissions, use a permission plugin like LuckPerms:

**In-game commands:**
```
/lp user <player> permission set myscript.use true
/lp group <group> permission set myscript.admin true
```

**Common permission plugins:**
- **LuckPerms** (recommended) - Modern, feature-rich
- **PermissionsEx** - Legacy but still used
- **GroupManager** - Simple and lightweight

**Note:** If you check a permission that was never registered anywhere, permission plugins can still grant it to players. The permission doesn't need to be registered for permission plugins to work with it.

### What Happens with Undefined Permissions?

If you use `hasPermission("some.permission")` but never register that permission:

| Player Type | Has Permission? | Why? |
|-------------|----------------|------|
| **Non-op player** | ❌ No | Undefined permissions default to `false` |
| **Op player** | ✅ Yes | Ops have all permissions by default |
| **Player with permission granted via LuckPerms** | ✅ Yes | Permission plugins can grant any permission, even unregistered ones |

**Example:**
```java
// This permission is NEVER registered anywhere
if (!sender.hasPermission("mycommand.use")) {
    sender.sendMessage("No permission!");
    return true;
}

// Result:
// - Non-ops: Blocked (no permission)
// - Ops: Allowed (ops have everything)
// - Players with "mycommand.use" in LuckPerms: Allowed
```

**This is usually what you want!** Most commands should be op-only by default.

### Dynamic Permission Registration

JavaSkript allows you to register permissions dynamically using the `DynamicPermissionRegistry`:

```java
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.permission.DynamicPermissionRegistry;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

@FoliaSupport
public class MyScript implements Listener {
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!event.getPlugin().getName().equals("JavaSkript")) return;
        
        // Get the permission registry
        DynamicPermissionRegistry registry = 
            JavaSkriptPlugin.getInstance().getPermissionRegistry();
        
        // Register a permission
        Permission perm = new Permission(
            "myscript.use",
            "Allows using MyScript commands",
            PermissionDefault.TRUE  // Default: everyone has it
        );
        registry.registerPermission(perm);
        
        // Register admin permission
        Permission adminPerm = new Permission(
            "myscript.admin",
            "Allows admin commands",
            PermissionDefault.OP  // Default: only ops have it
        );
        registry.registerPermission(adminPerm);
    }
}
```

### Permission Defaults

| PermissionDefault | Who Gets It |
|-------------------|-------------|
| `TRUE` | Everyone (including non-ops) |
| `FALSE` | Nobody by default (must be granted) |
| `OP` | Only server operators |
| `NOT_OP` | Everyone except operators |

### Permission Hierarchy

You can create permission hierarchies using wildcards:

```java
// Parent permission that grants all child permissions
Permission parent = new Permission(
    "myscript.*",
    "Grants all MyScript permissions",
    PermissionDefault.OP
);

// Child permissions
Permission use = new Permission("myscript.use");
Permission admin = new Permission("myscript.admin");

// Add children to parent
parent.getChildren().put("myscript.use", true);
parent.getChildren().put("myscript.admin", true);

// Register all
registry.registerPermission(parent);
registry.registerPermission(use);
registry.registerPermission(admin);
```

### Checking Multiple Permissions

```java
@Override
public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    // Check for any of multiple permissions
    if (!sender.hasPermission("myscript.use") && 
        !sender.hasPermission("myscript.admin")) {
        sender.sendMessage(Component.text("No permission!").color(NamedTextColor.RED));
        return true;
    }
    
    // Check for specific admin permission
    if (args.length > 0 && args[0].equals("reload")) {
        if (!sender.hasPermission("myscript.admin")) {
            sender.sendMessage(Component.text("Admin only!").color(NamedTextColor.RED));
            return true;
        }
    }
    
    return true;
}
```

### Permission Nodes Best Practices

**Good permission naming:**
```
myscript.use          # Basic usage
myscript.command.fly  # Specific command
myscript.admin        # Admin features
myscript.*            # All permissions
```

**Bad permission naming:**
```
fly                   # Too generic, conflicts possible
MyScript.Use          # Don't use capitals
my-script-use         # Use dots, not dashes
```

### Complete Example

```java
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.permission.DynamicPermissionRegistry;
import dev.mukulx.javaskript.script.FoliaSupport;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.List;

@FoliaSupport
public class FlyCommand implements Listener, CommandExecutor, TabCompleter {
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!event.getPlugin().getName().equals("JavaSkript")) return;
        
        // Register permissions
        DynamicPermissionRegistry registry = 
            JavaSkriptPlugin.getInstance().getPermissionRegistry();
        
        // Basic fly permission
        registry.registerPermission(new Permission(
            "fly.use",
            "Allows toggling flight",
            PermissionDefault.OP
        ));
        
        // Fly for others permission
        registry.registerPermission(new Permission(
            "fly.others",
            "Allows toggling flight for other players",
            PermissionDefault.OP
        ));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check basic permission
        if (!player.hasPermission("fly.use")) {
            player.sendMessage(Component.text("No permission!").color(NamedTextColor.RED));
            return true;
        }
        
        // Toggle for another player
        if (args.length > 0) {
            if (!player.hasPermission("fly.others")) {
                player.sendMessage(Component.text("No permission to toggle flight for others!")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            Player target = org.bukkit.Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return true;
            }
            
            boolean fly = !target.getAllowFlight();
            target.setAllowFlight(fly);
            player.sendMessage(Component.text("Toggled flight for " + target.getName() + ": " + 
                (fly ? "ON" : "OFF")).color(NamedTextColor.GREEN));
            return true;
        }
        
        // Toggle for self
        boolean fly = !player.getAllowFlight();
        player.setAllowFlight(fly);
        player.sendMessage(Component.text("Flight: " + (fly ? "ON" : "OFF"))
            .color(NamedTextColor.GREEN));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("fly.others")) {
            return org.bukkit.Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .toList();
        }
        return List.of();
    }
}
```

### Unregistering Permissions

Permissions are automatically unregistered when scripts are unloaded. You can also manually unregister:

```java
DynamicPermissionRegistry registry = 
    JavaSkriptPlugin.getInstance().getPermissionRegistry();

// Unregister specific permission
registry.unregisterPermission("myscript.use");

// Unregister all permissions registered by this script
registry.unregisterAll();
```

### Checking Permissions in Events

```java
@EventHandler
public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    
    // Check permission before allowing action
    if (!player.hasPermission("myscript.break.special")) {
        if (event.getBlock().getType() == Material.DIAMOND_ORE) {
            event.setCancelled(true);
            player.sendMessage(Component.text("No permission to break diamond ore!")
                .color(NamedTextColor.RED));
        }
    }
}
```

### Default Permissions for All Players

If you want everyone to have a permission by default:

```java
Permission perm = new Permission(
    "myscript.use",
    "Basic usage permission",
    PermissionDefault.TRUE  // Everyone gets this
);
registry.registerPermission(perm);
```

### Op-Only Permissions

For admin/op-only features:

```java
Permission adminPerm = new Permission(
    "myscript.admin",
    "Admin commands",
    PermissionDefault.OP  // Only ops get this
);
registry.registerPermission(adminPerm);
```

### Permission Plugin Setup

**LuckPerms (Recommended):**

1. Install LuckPerms plugin
2. Grant permissions to players:
   ```
   /lp user Steve permission set myscript.use true
   ```
3. Grant permissions to groups:
   ```
   /lp group admin permission set myscript.admin true
   ```
4. View permissions:
   ```
   /lp user Steve permission info
   ```

**Without a Permission Plugin:**

If you don't have a permission plugin, only `PermissionDefault.TRUE` and `PermissionDefault.OP` will work. Players won't be able to get custom permissions unless they're ops.

### Troubleshooting Permissions

**Permission not working:**
1. Check the permission name is correct (case-sensitive)
2. Verify permission is registered (check console on script load)
3. Check player has the permission: `/lp user <player> permission check <permission>`
4. Ensure permission plugin is installed and working

**Permission not registered:**
1. Make sure you register in `PluginEnableEvent` for JavaSkript
2. Check console for errors during script load
3. Verify `DynamicPermissionRegistry` is accessible

**Everyone has permission when they shouldn't:**
1. Check `PermissionDefault` - should be `FALSE` or `OP`, not `TRUE`
2. Check permission plugin configuration
3. Verify no wildcard permissions are granted (`*` or `myscript.*`)

### Quick Reference

**Do I need to register permissions?**

| Scenario | Register? | Why? |
|----------|-----------|------|
| Op-only command | ❌ No | Undefined permissions are op-only by default |
| Everyone should have access | ✅ Yes | Use `PermissionDefault.TRUE` |
| Want it in LuckPerms list | ✅ Yes | Makes it visible in `/lp` commands |
| Want permission hierarchy | ✅ Yes | Need to define parent/child relationships |
| Just checking permission | ❌ No | `hasPermission()` works with unregistered permissions |

**Permission behavior summary:**

```java
// Unregistered permission
if (!sender.hasPermission("undefined.permission")) {
    // Non-ops: BLOCKED
    // Ops: ALLOWED
    // LuckPerms users with permission: ALLOWED
}

// Registered with PermissionDefault.FALSE
if (!sender.hasPermission("registered.false")) {
    // Non-ops: BLOCKED
    // Ops: BLOCKED (unless granted via LuckPerms)
    // LuckPerms users with permission: ALLOWED
}

// Registered with PermissionDefault.TRUE
if (!sender.hasPermission("registered.true")) {
    // Non-ops: ALLOWED
    // Ops: ALLOWED
    // Everyone: ALLOWED (unless explicitly denied)
}

// Registered with PermissionDefault.OP
if (!sender.hasPermission("registered.op")) {
    // Non-ops: BLOCKED
    // Ops: ALLOWED
    // LuckPerms users with permission: ALLOWED
}
```

**Best practices:**
- ✅ Don't register permissions for op-only commands (simpler)
- ✅ Register permissions if you want them visible in permission plugins
- ✅ Use `PermissionDefault.TRUE` for features everyone should access
- ✅ Use `PermissionDefault.OP` for admin features (or don't register at all)
- ✅ Always check permissions in commands, even if not registered

---

## Getting Plugin Instance

Scripts can access the JavaSkript plugin instance to use Bukkit's scheduler and other APIs.

### Method

```java
import dev.mukulx.javaskript.JavaSkriptPlugin;

JavaSkriptPlugin plugin = JavaSkriptPlugin.getInstance();
```

### Common Use Cases

#### Using Bukkit Scheduler
```java
import org.bukkit.Bukkit;
import dev.mukulx.javaskript.JavaSkriptPlugin;

// Run task later
Bukkit.getScheduler().runTaskLater(
    JavaSkriptPlugin.getInstance(),
    () -> {
        player.sendMessage("Hello!");
    },
    20L
);

// Run async task
Bukkit.getScheduler().runTaskAsynchronously(
    JavaSkriptPlugin.getInstance(),
    () -> {
        // Heavy computation here
    }
);
```

#### Getting Data Folder
```java
import java.io.File;
import dev.mukulx.javaskript.JavaSkriptPlugin;

File dataFolder = JavaSkriptPlugin.getInstance().getDataFolder();
File myFolder = new File(dataFolder, "mydata");
if (!myFolder.exists()) {
    myFolder.mkdirs();
}
```

#### Logging
```java
import org.bukkit.Bukkit;

// Simple logging
Bukkit.getLogger().info("[MyScript] Script loaded!");
Bukkit.getLogger().warning("[MyScript] Warning message");
Bukkit.getLogger().severe("[MyScript] Error message");
```

### Important Notes

- **Don't use in constructors**: The plugin instance is available, but avoid heavy operations in constructors
- **Use ScriptScheduler when possible**: It's easier and auto-cleans up tasks
- **Folia compatibility**: If using Bukkit scheduler directly, your script won't work on Folia. Use `@PaperOnly` annotation or ScriptScheduler instead

---

## Plugin Configuration

JavaSkript has a `config.yml` file in `plugins/JavaSkript/` with the following options:

### File Watcher Settings

```yaml
file-watcher:
  # Enable or disable automatic script reloading
  # If disabled, you must use /js reload to reload scripts manually
  enabled: true
  
  # Delay in milliseconds before reloading a script after it's modified
  # This prevents multiple reloads when saving files
  reload-delay: 500
```

### Script Settings

```yaml
scripts:
  # Automatically load all scripts on server startup
  auto-load: true
  
  # Show detailed compilation errors in console
  verbose-errors: true
  
  # Allow scripts to create folders anywhere in the JavaSkript directory
  # When false (recommended), scripts must use the script-data folder
  # When true, scripts can create folders anywhere (not recommended)
  allow-unrestricted-folders: false
```

**Important:** Scripts should always store data in `plugins/JavaSkript/script-data/YourScriptName/` to keep things organized.

### Dependency Settings

```yaml
dependencies:
  # Cache directory for downloaded Maven dependencies
  # Relative to the plugin folder
  cache-folder: "libs"
  
  # Maven repository URL for downloading script dependencies
  repository: "https://repo1.maven.org/maven2/"
```

---

## ActionBarHelper

Comprehensive ActionBar API with gradients, animations, and MiniMessage support.

### Auto-Injection
```java
private ActionBarHelper actionBar; // Automatically injected!
```

### Simple Messages

#### `send(Player player, String text)`
Send a plain text action bar.
```java
actionBar.send(player, "Hello!");
```

#### `sendMini(Player player, String miniMessageText)`
Send with MiniMessage formatting.
```java
actionBar.sendMini(player, "<gradient:gold:yellow><bold>Fancy Text!</bold></gradient>");
actionBar.sendMini(player, "<red>❤</red> <aqua>Health</aqua>");
```

#### `send(Player player, String text, Duration duration)`
Send with auto-clear after duration.
```java
actionBar.send(player, "This disappears in 5 seconds", Duration.ofSeconds(5));
```

### Gradients

#### `gradient(String text)`
Create gradient text builder.
```java
actionBar.gradient("Beautiful Text")
    .colors(NamedTextColor.AQUA, NamedTextColor.LIGHT_PURPLE)
    .bold()
    .send(player);

actionBar.gradient("Rainbow!")
    .colors(NamedTextColor.RED, NamedTextColor.BLUE)
    .italic()
    .underlined()
    .send(player, Duration.ofSeconds(10));
```

### Progress Bars

#### `progressBar()`
Create a customizable progress bar.
```java
actionBar.progressBar()
    .current(75)
    .max(100)
    .length(20)
    .prefix("<gold>⚡ Power: </gold>")
    .showPercentage(true)
    .filledColor(NamedTextColor.YELLOW)
    .emptyColor(NamedTextColor.DARK_GRAY)
    .send(player);
```

### Persistent Messages

#### `sendPersistent(Player player, String text)`
Send a message that stays until manually cleared (refreshes every second).
```java
actionBar.sendPersistentMini(player, "<gradient:green:aqua>Persistent Message</gradient>");
// Stays until cleared
actionBar.clear(player);
```

### Animations

#### `sendAnimated(Player player, List<String> frames, long interval)`
Animate through multiple frames.
```java
List<String> frames = Arrays.asList(
    "<gray>Loading<white>.",
    "<gray>Loading<white>..",
    "<gray>Loading<white>..."
);
actionBar.sendAnimated(player, frames, 10L);

// With auto-stop
actionBar.sendAnimated(player, frames, 10L, Duration.ofSeconds(5));
```

### Quick Utilities

Pre-made components for common patterns.

#### Health Bar
```java
Component health = ActionBarHelper.Quick.healthBar(player.getHealth(), player.getMaxHealth());
actionBar.send(player, health);

// Custom length
Component health = ActionBarHelper.Quick.healthBar(20.0, 20.0, 15);
```

#### XP Bar
```java
Component xp = ActionBarHelper.Quick.xpBar(450, 1000);
actionBar.send(player, xp);
```

#### Cooldown Bar
```java
Component cooldown = ActionBarHelper.Quick.cooldownBar(5000, 10000); // 5s remaining of 10s
actionBar.send(player, cooldown);
```

#### Loading Animation
```java
List<String> loading = ActionBarHelper.Quick.loadingAnimation();
actionBar.sendAnimated(player, loading, 10L, Duration.ofSeconds(5));
```

#### Spinner Animation
```java
List<String> spinner = ActionBarHelper.Quick.spinnerAnimation();
actionBar.sendAnimated(player, spinner, 2L, Duration.ofSeconds(5));
```

#### Rainbow Animation
```java
List<String> rainbow = ActionBarHelper.Quick.rainbowAnimation("Rainbow Text!");
actionBar.sendAnimated(player, rainbow, 5L, Duration.ofSeconds(10));
```

### Broadcasting

#### `broadcast(String text)`
Send to all online players.
```java
actionBar.broadcast("Server message!");
actionBar.broadcastMini("<gradient:red:yellow>Important!</gradient>");
```

### Clearing

#### `clear(Player player)`
Clear the player's action bar and cancel any active tasks.
```java
actionBar.clear(player);
```

---

## Annotations

### `@FoliaSupport`

Mark your script as Folia-compatible.

```java
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class MyScript implements Listener {
    // This script works on both Paper and Folia
}
```

**When to use:**
- Your script uses ScriptScheduler (not Bukkit.getScheduler())
- Your script doesn't use location-dependent operations
- Your script is designed for Folia's regional threading

### `@PaperOnly`

Mark your script as Paper-only (won't load on Folia).

```java
import dev.mukulx.javaskript.script.PaperOnly;

@PaperOnly
public class MyScript implements Listener {
    // This script only works on Paper
}
```

**When to use:**
- Your script uses `Bukkit.getScheduler()` directly
- Your script uses Paper-specific APIs
- Your script can't be adapted for Folia

**Parameters:**
- `reason` - Explanation of why the script is Paper-only

### `@ScriptDependency`

Specify script dependencies (load order).

```java
import dev.mukulx.javaskript.script.ScriptDependency;

@ScriptDependency({"DatabaseHelper.java", "ConfigManager.java"})
public class MyScript implements Listener {
    // This script loads after its dependencies
}
```

**Parameters:**
- `value` - Array of script file names this script depends on

---

## Manual API Access

If auto-injection doesn't work, you can create APIs manually:

```java
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.*;

public class MyScript {
    private ScriptScheduler scheduler;
    private ScriptConfig config;
    private DatabaseHelper database;
    
    public MyScript() {
        var plugin = JavaSkriptPlugin.getInstance();
        this.scheduler = new ScriptScheduler(plugin);
        this.config = new ScriptConfig(plugin, "MyScript.java");
        this.database = new DatabaseHelper(plugin, "MyScript.java");
    }
}
```

**Note:** Auto-injection is preferred. Only use manual creation if you need custom initialization.

---

## Script Lifecycle

Understanding how scripts are loaded and unloaded:

### Loading Process

1. **Compilation** - Script is compiled to bytecode
2. **Class Loading** - Class is loaded with dependencies
3. **Instance Creation** - Constructor is called (keep it lightweight!)
4. **API Injection** - APIs are injected into fields
5. **Registration** - Events and commands are registered

### What Happens Automatically

- **Listener Registration**: If your class implements `Listener`, events are auto-registered
- **Command Registration**: If your class implements `CommandExecutor`, command is auto-registered
  - Command name is derived from class name (e.g., `FlyCommand` → `/fly`)
- **API Injection**: Fields named `scheduler`, `config`, `database`, `db`, `placeholders`, or `papi` are auto-injected
- **Cleanup**: On unload, all tasks, events, commands, and database connections are cleaned up

### Constructor Guidelines

```java
public class MyScript implements Listener {
    
    // ✅ GOOD - Declare fields
    private ScriptScheduler scheduler;
    private Map<UUID, Integer> playerData = new HashMap<>();
    
    public MyScript() {
        // ✅ GOOD - Initialize simple data structures
        // ✅ GOOD - Set up variables
        
        // ❌ BAD - Don't use APIs here (not injected yet!)
        // scheduler.runLater(...); // Will be null!
        
        // ❌ BAD - Don't register events/commands (done automatically)
        // ❌ BAD - Don't do heavy operations
    }
    
    // ✅ GOOD - Use APIs in event handlers
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduler.runLater(() -> {
            // This works!
        }, 20L);
    }
}
```

---

## External Dependencies

Scripts can use external Maven dependencies using the `@dependency` comment.

### Syntax

```java
// @dependency groupId:artifactId:version

// Example: Using Gson
// @dependency com.google.code.gson:gson:2.10.1

import com.google.code.gson.Gson;

public class MyScript {
    private Gson gson = new Gson();
    
    // Use gson...
}
```

### Multiple Dependencies

```java
// @dependency com.google.code.gson:gson:2.10.1
// @dependency com.zaxxer:HikariCP:5.1.0
// @dependency org.xerial:sqlite-jdbc:3.53.1.0

import com.google.code.gson.Gson;
import com.zaxxer.hikari.HikariDataSource;
```

### How It Works

1. JavaSkript parses `@dependency` comments
2. Downloads JARs from Maven Central
3. Caches them in `plugins/JavaSkript/libs/`
4. Adds them to the script's classpath
5. Resolves transitive dependencies automatically

### Notes

- Dependencies are downloaded on first load (may take a few seconds)
- Cached dependencies are reused across reloads
- Use exact versions for reproducibility
- Check Maven Central for available versions

---

## Manual API Access

If auto-injection doesn't work, you can create APIs manually:

```java
import dev.mukulx.javaskript.JavaSkriptPlugin;

public class MyScript {
    private ScriptScheduler scheduler;
    private ScriptConfig config;
    
    public MyScript() {
        var plugin = JavaSkriptPlugin.getInstance();
        this.scheduler = new ScriptScheduler(plugin);
        this.config = new ScriptConfig(plugin, "MyScript.java");
    }
}
```

---

## Best Practices

1. **Always check for null** - Especially in event handlers
2. **Use auto-injection** - Declare fields and let JavaSkript inject them
3. **Clean up resources** - APIs auto-cleanup on script unload
4. **Use async for heavy tasks** - Keep the main thread responsive
5. **Test your scripts** - Use `/js reload` to test changes
6. **Check examples** - Look at example scripts in `plugins/JavaSkript/scripts/`

---

## Getting Help

- Check example scripts in `plugins/JavaSkript/scripts/`
- Read `EXAMPLES.md` for more examples
- Check `TUTORIAL.md` for step-by-step guides
- Report issues on GitHub

---

**Last Updated:** 2026-05-27
