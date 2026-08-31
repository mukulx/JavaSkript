# JavaSkript API Documentation

Complete reference for all JavaSkript APIs available to scripts.

## Table of Contents

1. [ScriptScheduler & Tasks](#scriptscheduler)
2. [ScriptConfig & Storage](#scriptconfig)
3. [DatabaseHelper (SQLite)](#databasehelper)
4. [GUI Builder & Menus](#gui-builder)
5. [ItemBuilder & Items Utility](#itembuilder--itemhelper)
6. [CommandHelper (Fluent Commands)](#commandhelper-fluent-commands)
7. [RecipeHelper (Custom Recipes)](#recipehelper-custom-recipes)
8. [PlayerHelper & Players Utility](#playerhelper--players-universal-player-utility)
9. [Sounds & Audio Chords](#audio--visuals)
10. [ChatHelper (Interactive Chat API)](#chathelper-interactive-chat-api)
11. [EventHelper (Lambda Events & Custom Event Bus)](#eventhelper-lambda-events)
12. [CooldownHelper & Cooldowns](#cooldownhelper-rate-limiting--tickers)
13. [HologramHelper & Holograms](#hologramhelper-display-entities)
14. [ActionBarHelper, TitleHelper, & BossBarHelper](#actionbarhelper)
15. [EconomyHelper & Economy Engine](#economyhelper--economy-universal-economy-engine)
16. [PDCHelper (PersistentData / NBT)](#pdchelper-persistentdata--nbt)
17. [DialogHelper](#dialoghelper)
18. [PlaceholderHelper](#placeholderhelper)
19. [Permissions Management](#permissions)
20. [Script Annotations & Lifecycle](#annotations)
21. [External Maven Dependencies](#external-dependencies)
22. [External Plugin & Addon Integration](#external-plugin--addon-integration)
23. [VariableHelper & Variables (Shared State)](#variablehelper--variables-shared-state)

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

Crash-safe, comment-preserving configuration management with schema migrations and O(1) in-memory reads.

### Auto-Injection
```java
private ScriptConfig config; // Automatically injected!
```

### File Location
Config files are stored in: `plugins/JavaSkript/script-data/YourScriptName/`

### Features & Methods

#### 1. Non-Destructive Defaults with Comments (`addDefault`)
Adds missing keys with comments. If the user already customized the value, their existing setting is strictly preserved:
```java
// Header comments at top of file
config.setHeader("MyScript Configuration", "User settings are preserved across updates.");

// Key-level defaults with comments
config.addDefault("enabled", true, "Enable or disable this module");
config.addDefault("cooldown", 30, "Cooldown in seconds between uses");
config.addDefault("rewards.coins", 100, "Coins awarded on completion");
```

#### 2. Automatic Version Migration (`migrate`)
Upgrade configuration schemas across script versions with zero data loss:
```java
config.migrate(2, c -> {
    // Automatically runs when config-version is below 2:
    if (c.contains("old-spawn-point")) {
        c.set("spawn.location", c.get("old-spawn-point"));
        c.remove("old-spawn-point");
    }
    c.addDefault("spawn.sound", "ENTITY_PLAYER_LEVELUP", "Sound played on spawn");
});
```

#### 3. Comments & Headers
Read and write comments dynamically without parsing YAML manually:
```java
config.setComments("rewards.coins", "Coins given to the player", "Set to 0 to disable");
config.setInlineComments("enabled", "Master toggle");
List<String> comments = config.getComments("rewards.coins");
```

#### 4. Batch Updates (`batch`)
Modify multiple values in memory and flush to disk in a single atomic save:
```java
config.batch(cfg -> {
    cfg.set("stats.kills", 10);
    cfg.set("stats.deaths", 2);
    cfg.set("stats.ratio", 5.0);
});
```

#### 5. Section & Key Queries
```java
ConfigurationSection section = config.getSection("rewards");
Set<String> keys = config.getKeys("rewards", false);
boolean exists = config.contains("enabled");
config.remove("deprecated-key");
```

#### 6. Typed Getters & Setters
```java
String title = config.getString("title", "Default Title");
int amount = config.getInt("amount", 1);
double multiplier = config.getDouble("multiplier", 1.0);
boolean debug = config.getBoolean("debug", false);
List<String> list = config.getStringList("allowed-worlds");

config.set("title", "New Title"); // Saves atomically
```

#### 7. Multi-File YAML
```java
FileConfiguration messages = config.getConfig("messages.yml");
config.addDefault("messages.yml", "prefix", "<gold>[Server]</gold> ");
config.saveConfig("messages.yml");
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

#### `ItemBuilder` & `ItemHelper`
Easy, fluent item creation with MiniMessage formatting, specialized meta, and full Bukkit extensibility.

**Script Auto-Injection:**
```java
private ItemHelper items; // Automatically injected!
```

**Static & Instance Factories:**
- `ItemBuilder.of(Material material)`
- `ItemBuilder.of(Material material, int amount)`
- `ItemBuilder.from(ItemStack item)` - clone & edit existing item
- `ItemBuilder.skull(String playerNameOrBase64)` - custom heads & textures
- `items.create(Material.NETHERITE_SWORD)`

```java
ItemStack sword = items.create(Material.NETHERITE_SWORD)
    .name("<gradient:#ff5555:#ffaa00><bold>EXCALIBUR</bold></gradient>")
    .lore(
        "&7An ancient blade pulled from stone.",
        "",
        "&c+25 Attack Damage",
        "&4&lLEGENDARY ARTIFACT"
    )
    .enchant(Enchantment.SHARPNESS, 6)
    .unbreakable()
    .pdc("ability", "lightning_strike")
    .customModelData(1042)
    .glow()
    .build();
```

**Display & Formatting:**
- `name(String name)` - Auto-parses MiniMessage (`<gradient:...>`, `<bold>`, `<rainbow>`) and legacy (`&a`, `&l`)
- `name(Component name)` - Kyori Adventure Component
- `lore(String... lines)` / `lore(List<String> lines)` - Auto-parses MiniMessage & legacy
- `appendLore(String... lines)` - Append extra lines
- `clearLore()` - Clear lore

**Properties & Flags:**
- `amount(int amount)` - Set stack count
- `glow()` / `glow(boolean glow)` - Add/remove shimmer glow
- `enchant(Enchantment ench)` / `enchant(Enchantment ench, int level)` - Add enchantment
- `removeEnchant(Enchantment ench)` / `clearEnchants()`
- `unbreakable()` / `unbreakable(boolean unbreakable)`
- `damage(int damage)` / `durability(int durability)`
- `customModelData(Integer data)`
- `flags(ItemFlag... flags)` / `hideAll()`

**Specialized Item Meta:**
- **Player Skulls:** `skullOwner(OfflinePlayer player)`, `skullOwner(String name)`, `skullTexture(String base64OrUrl)`
- **Leather Armor:** `color(Color color)`, `color(int r, int g, int b)`, `color(int rgbHex)`
- **Potions:** `potionType(PotionType type)`, `potionColor(Color color)`, `addPotionEffect(PotionEffect effect)`

**PersistentDataContainer (PDC):**
- `pdc(String key, String value)`
- `pdc(String key, int value)`
- `pdc(String key, double value)`
- `pdc(String key, boolean value)`
- `pdc(NamespacedKey key, PersistentDataType type, Object value)`
- `hasPdc(String key)`

**Open Meta Mutator (100% Extensible):**
- `meta(Consumer<ItemMeta> consumer)` - Directly mutate raw `ItemMeta`
- `<M extends ItemMeta> meta(Class<M> metaClass, Consumer<M> consumer)` - Mutate specialized meta (`BookMeta`, `BannerMeta`, `FireworkMeta`, `EnchantmentStorageMeta`)

**Give & Clone:**
- `give(Player player)` - Directly give item to player (drops on ground if inventory full)
- `clone()` - Duplicate builder state
- `build()` - Build final `ItemStack`

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
| **Non-op player** | No | Undefined permissions default to `false` |
| **Op player** | Yes | Ops have all permissions by default |
| **Player with permission granted via LuckPerms** | Yes | Permission plugins can grant any permission, even unregistered ones |

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
| Op-only command | No | Undefined permissions are op-only by default |
| Everyone should have access | Yes | Use `PermissionDefault.TRUE` |
| Want it in LuckPerms list | Yes | Makes it visible in `/lp` commands |
| Want permission hierarchy | Yes | Need to define parent/child relationships |
| Just checking permission | No | `hasPermission()` works with unregistered permissions |

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
- Don't register permissions for op-only commands (simpler)
- Register permissions if you want them visible in permission plugins
- Use `PermissionDefault.TRUE` for features everyone should access
- Use `PermissionDefault.OP` for admin features (or don't register at all)
- Always check permissions in commands, even if not registered

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
```

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
actionBar.sendMini(player, "<red>HP</red> <aqua>Health</aqua>");
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
    .prefix("<gold> Power: </gold>")
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

## TitleHelper & `Titles`

Send full-screen title and subtitle messages with fade timings, MiniMessage formatting, and automatic cleanup.

### Auto-Injection
```java
private TitleHelper titles; // Automatically injected!
// Alias: private TitleHelper title;
```

### Methods
```java
// Basic Title & Subtitle (supports MiniMessage and legacy color formatting)
titles.send(player, "<gold><bold>VICTORY!</bold></gold>", "<yellow>You completed the trial</yellow>");

// Title with Custom Fade Timings (in ticks: fadeIn, stay, fadeOut)
titles.send(player, "<gradient:#ff5555:#ffaa00>LEVEL UP</gradient>", "<gray>Reached Level 50</gray>", 10, 60, 20);

// Broadcast Title to All Online Players
titles.broadcast("<red><bold>BOSS SPAWNED</bold></red>", "<gray>Prepare for battle!</gray>");

// Clear Active Title
titles.clear(player);
```

---

## BossBarHelper & `BossBars`

Display custom progress boss bars at the top of player screens with colors, segment overlays, and animated countdowns.

### Auto-Injection
```java
private BossBarHelper bossbars; // Automatically injected!
// Alias: private BossBarHelper bossbar;
```

### Methods
```java
// Show BossBar to a player (text, progress from 0.0f to 1.0f, color, overlay style)
bossbars.show(player, "<gradient:#ff5555:#ffaa00>Dragon Health</gradient>", 0.75f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);

// Update progress and title dynamically
bossbars.setProgress(player, 0.50f);
bossbars.setText(player, "<gradient:#ff5555:#ffaa00>Dragon Health (50%)</gradient>");

// Hide and clean up
bossbars.hide(player);

// Broadcast to all players with timed animation (duration in ticks)
bossbars.broadcastAnimated("<gold>Double Experience Weekend</gold>", 1200L, BossBar.Color.YELLOW);
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
    
    // Good: Declare fields
    private ScriptScheduler scheduler;
    private Map<UUID, Integer> playerData = new HashMap<>();
    
    public MyScript() {
        // Good: Initialize simple data structures
        // Good: Set up variables
        
        // Avoid: Don't use APIs here (not injected yet!)
        // scheduler.runLater(...); // Will be null!
        
        // Avoid: Don't register events/commands (done automatically)
        // Avoid: Don't do heavy operations
    }
    
    // Good: Use APIs in event handlers
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

## DialogHelper

Modern Paper Dialog API for creating minimal native Minecraft dialogs.

### Auto-Injection
```java
private DialogHelper dialog; // Automatically injected!
```

### Methods

#### Quick Dialogs
```java
// Alert notice
dialog.alert(player, "<gold>Alert</gold>", "This is an alert message!");

// Confirmation with callbacks
dialog.confirm(player, "<red>Reset?</red>", "Are you sure?", 
    p -> p.sendMessage("Reset!"), 
    p -> p.sendMessage("Cancelled.")
);

// Form prompt input
dialog.input(player, "Enter Nickname", "Type your new name:", (val, p) -> {
    p.sendMessage("New nickname: " + val);
});
```

#### Fluent Custom Dialogs
```java
dialog.multiAction("<gold>Custom Menu</gold>")
    .body("<gray>Choose an option below:</gray>")
    .columns(2)
    .button("Heal Me", p -> p.setHealth(p.getMaxHealth()))
    .button("Website", DialogActions.openUrl("https://papermc.io"))
    .button("Run Command", DialogActions.runCommand("/spawn"))
    .exitAction("Close")
    .send(player);
```

---

## PDCHelper (PersistentData / NBT)

Minimal PersistentDataContainer manipulation on `ItemStack`, `Entity`, `LivingEntity`, `Player`, `BlockState`, `Chunk`, and `World`.

### Auto-Injection
```java
private PDCHelper pdc; // Automatically injected!
```

### Methods

#### Storing Data on Items
```java
pdc.set(item, "custom_id", "hyperion_blade");
pdc.set(item, "bonus_damage", 25.0);
pdc.set(item, "level_required", 10);
pdc.set(item, "soulbound", player.getUniqueId());
pdc.set(item, "untradeable", true);
```

#### Storing Data via ItemBuilder
```java
ItemStack sword = new ItemBuilder(Material.NETHERITE_SWORD)
    .name("<gold>Hyperion Blade</gold>")
    .pdc("custom_id", "hyperion_blade")
    .pdc("bonus_damage", 25.0)
    .pdc("soulbound", true)
    .build();
```

#### Reading Data from Items
```java
if (pdc.has(item, "custom_id")) {
    String id = pdc.getString(item, "custom_id");
    double bonus = pdc.getDouble(item, "bonus_damage", 0.0);
    int level = pdc.getInt(item, "level_required", 1);
    boolean isSoulbound = pdc.getBoolean(item, "soulbound", false);
    UUID owner = pdc.getUUID(item, "soulbound_uuid");
}
```

#### Storing & Reading Data on Entities / Mobs
```java
// Tagging a mob
pdc.set(zombie, "boss_type", "inferno_titan");
pdc.set(zombie, "drop_multiplier", 3);

// Reading in an event
@EventHandler
public void onBossDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();
    if (pdc.has(entity, "boss_type")) {
        String type = pdc.getString(entity, "boss_type");
        int multiplier = pdc.getInt(entity, "drop_multiplier", 1);
        // Custom loot logic
    }
}
```

---

## HologramHelper (Display Entities)

Modern Paper Display Entity system for creating zero-lag floating text, 3D items, and 3D blocks without armor stands or external plugins.

### Usage: Injection or Static Facade
```java
// Option A: Auto-injected field
private HologramHelper holograms;

// Option B: 1-line static facade anywhere (no injection needed!)
Holograms.text(loc, "<gold><bold>SPAWN POINT</bold></gold>");
Holograms.item(loc, new ItemStack(Material.NETHER_STAR));
Holograms.create(loc, "<yellow>Line 1", "<gold>Line 2").spawn();
```

### Methods

#### 1. Dynamic Auto-Refreshing Leaderboard
```java
Hologram holo = holograms.create(location)
    .line("<gradient:#ff5555:#ffaa00><bold> LEADERBOARD </bold></gradient>")
    .line("<yellow>#1 Mukul - 1,450 Kills</yellow>")
    .line("")
    .line(() -> "<gray>Online: <green>" + Bukkit.getOnlinePlayers().size() + "</green></gray>")
    .billboard(Display.Billboard.CENTER)
    .shadow(true)
    .lineSpacing(0.28)
    .updateInterval(20) // Updates dynamic lines every second
    .spawn();
```

#### 2. Floating 3D Item Showcase
```java
ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);

holograms.create(location)
    .line("<gradient:#aa00aa:#ff55ff><bold>Excalibur</bold></gradient>")
    .item(sword, 1.2)
    .glowing(true)
    .glowColor(Color.PURPLE)
    .spawn();
```

#### 3. Floating 3D Block Showcase
```java
holograms.create(location)
    .line("<aqua><bold>Enchantment Altar</bold></aqua>")
    .block(Material.ENCHANTING_TABLE)
    .scale(0.8)
    .spawn();
```

#### 4. Hologram Management
```java
holo.teleport(newLocation); // Move smoothly
holo.setLine(0, "<green>New Title</green>");
holo.remove(); // Despawn specific hologram
holograms.removeAll(); // Clear all holograms
```

---

## CooldownHelper (Rate Limiting & Tickers)

Thread-safe, memory-leak-free cooldown engine for player abilities, combat tags, commands, and global server rate limits.

### Usage: Injection or Static Facade
```java
// Option A: Auto-injected field
private CooldownHelper cooldowns;

// Option B: 1-line static facade anywhere (no injection needed!)
if (Cooldowns.has(player, "fireball")) {
    double sec = Cooldowns.remainingSeconds(player, "fireball");
    Players.msg(player, "<red>Wait " + sec + "s!");
    return;
}
Cooldowns.set(player, "fireball", Duration.ofSeconds(15));
Cooldowns.showTicker(player, "fireball", "Fireball");
```

### Methods & Features

#### 1. Checking & Starting Cooldowns
```java
// Check if player is on cooldown
if (cooldowns.isOnCooldown(player, "fireball")) {
    long remainingSec = cooldowns.getRemainingSeconds(player, "fireball");
    player.sendMessage("§cFireball on cooldown: " + remainingSec + "s");
    return;
}

// Start 15-second cooldown
cooldowns.set(player, "fireball", Duration.ofSeconds(15));
```

#### 2. Animated Action Bar Progress Tickers
Automatically displays a smooth ticking progress bar on the player's action bar until the cooldown finishes, then confirms readiness!
```java
// Displays: "Fireball [■■■■■■□□□□] 4.2s" -> " Fireball Ready to use!"
cooldowns.startActionBarTicker(player, "fireball", "Fireball");
```

#### 3. Global Server-Wide Rate Limits
```java
// Limit a server command to once every 30 seconds across all players
if (cooldowns.isGlobalOnCooldown("server_poll")) {
    long remaining = cooldowns.getGlobalRemainingSeconds("server_poll");
    sender.sendMessage("§cPoll already active! Wait " + remaining + "s.");
    return;
}
cooldowns.setGlobal("server_poll", Duration.ofSeconds(30));
```

#### 4. Reset & Cleanup
```java
cooldowns.reset(player, "fireball"); // Clear specific ability
cooldowns.resetAll(player);          // Clear all cooldowns for player
cooldowns.resetGlobal("server_poll");// Clear specific global cooldown
cooldowns.purgeExpired();            // Automatically done: purges expired memory
```

---

## EventHelper (Lambda Events)

Register event listeners in **1 line** using lambdas without creating `Listener` classes or writing `@EventHandler`.

### Auto-Injection
```java
private EventHelper events;
```

### 1. Basic 1-Line Listener
```java
events.on(PlayerJoinEvent.class, e -> {
    e.getPlayer().sendMessage("§aWelcome to the server!");
});
```

### 2. Filtered Events
Execute only when a condition passes (saves CPU and indentation):
```java
events.on(BlockBreakEvent.class, e -> e.getBlock().getType() == Material.DIAMOND_ORE, e -> {
    e.getPlayer().sendMessage("§bYou found Diamonds!");
});
```

### 3. Run-Once Listeners (`once`)
Automatically unregisters itself after 1 execution (great for tutorials, quests, or first clicks):
```java
events.once(PlayerInteractEvent.class, e -> {
    e.getPlayer().sendMessage("§eFirst interaction complete!");
});
```

### 4. Advanced Subscriptions (`EventSubscription`)
```java
EventSubscription<EntityDamageByEntityEvent> sub = events.on(EntityDamageByEntityEvent.class, e -> {
    // Combat handler
})
.maxExecutions(20)                 // Auto-unsubscribes after 20 hits
.expireAfter(Duration.ofMinutes(2)) // Auto-unsubscribes after 2 minutes
.onExpire(() -> {
    Bukkit.broadcast(Component.text("Combat period ended."));
});

// Or cancel manually at any time:
sub.unsubscribe();
```

### 6. Inter-Script Custom Events (Pub/Sub Event Bus)

Broadcast custom events to any other script on the server without compile-time class dependencies:

```java
// In Script A (Broadcaster):
events.fire("quest_completed", player, "ancient_ruins", 500);

// In Script B (Listener):
events.onCustom("quest_completed", ctx -> {
    Player player = ctx.getPlayer();
    String quest = ctx.get(1, String.class);
    int coins = ctx.get(2, Integer.class, 0);

    Economy.deposit(player, coins);
    Players.msg(player, "<green>Reward received for " + quest);
});

// Or listen once:
events.onceCustom("first_blood", ctx -> {
    Player killer = ctx.getPlayer();
    Players.broadcast("<red><bold>" + killer.getName() + " got First Blood!</bold></red>");
});
```

All custom event listeners are automatically unregistered when the script unloads or reloads.

---

## PlayerHelper & `Players` (Universal Player Utility)

Streamlines player communication, audio-visual feedback, stats manipulation, and spatial queries.
Usable via auto-injection (`private PlayerHelper players;`) or statically anywhere via `Players`.

### 1. 1-Line Messaging & Broadcasts
```java
// Supports MiniMessage gradients & legacy & codes with default italics stripped:
Players.msg(player, "<gradient:#ff5555:#ffaa00><bold>Victory!</bold></gradient>");
Players.broadcast("<yellow> Mukul joined the realm!");
```

### 2. Audio & Visuals
```java
Players.sound(player, Sound.ENTITY_PLAYER_LEVELUP);
Players.sound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
Players.title(player, "<gold>LEVEL UP", "<yellow>You reached level 10!");
Players.actionBar(player, "<red> +25 Damage Dealt");

// Dedicated 1-line audio fanfares & feedback chords via Sounds facade:
Sounds.success(player);  // High-pitch chime for completed actions
Sounds.fail(player);     // Low bass note for errors / denied actions
Sounds.click(player);    // UI button click
Sounds.levelup(player);  // Grand level up fanfare
Sounds.chime(player);    // Notification bell chime
Sounds.play(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
```

### 3. Stats, Healing & Inventory
```java
Players.heal(player);     // Restores full max health, clears fire & negative potions
Players.feed(player);     // Restores 20 food level & 20 saturation
Players.clearInventory(player);
Players.give(player, sword, shield); // Safely drops leftover items if inventory full
```

### 4. Potion Effects (All Vanilla, 1.21 & Paper 26.2 Effects)
```java
// 1-line shortcuts with seconds and amplifier:
Players.speed(player, 10, 1);           // Speed II for 10s
Players.strength(player, 15, 0);        // Strength I for 15s
Players.invisibility(player, 30);       // Invisibility for 30s
Players.glowing(player, 5);             // Glowing for 5s
Players.fireResistance(player, 60);     // Fire Resistance for 60s
Players.jumpBoost(player, 10, 2);       // Jump Boost III for 10s

// 1.21 Trial Chambers Effects:
Players.windCharged(player, 10);        // Wind Charged
Players.weaving(player, 10);            // Weaving
Players.oozing(player, 10);             // Oozing
Players.infested(player, 10);           // Infested
Players.trialOmen(player, 30, 0);       // Trial Omen
Players.raidOmen(player, 30, 0);        // Raid Omen

// Paper 26.2 Nautilus Effect:
Players.breathOfTheNautilus(player, 20); // Breath of the Nautilus

// Flexible string lookup (supports names, namespaces & user aliases):
Players.addEffect(player, "speed", 10, 1);
Players.addEffect(player, "minecraft:haste", 20, 0);
Players.removeEffect(player, "poison");

// Status checks & cleanup:
if (Players.hasEffect(player, "speed")) { ... }
Players.clearNegativeEffects(player); // Clears all harmful/negative effects
Players.clearEffects(player);         // Clears all active effects
```

### 5. Spatial & Lightning
```java
Players.lightningEffect(player); // Harmless visual lightning (no damage/fire)
Players.lightning(loc);          // Real lightning strike

// Find nearby players within radius:
for (Player target : Players.nearby(player.getLocation(), 10.0)) {
    Players.msg(target, "<red>You were caught in the blast radius!");
}
```

---

## ChatHelper (Interactive Chat API)

A modern, complete chat engine for interactive buttons, confirmation workflows, private prompts, paginated lists, and pixel-perfect centered text banners.

### Auto-Injection
```java
private ChatHelper chat;
```

### 1. Interactive ChatBuilder (Buttons, Hover, & Lambda Actions)
```java
chat.builder()
    .text("<yellow>Special Offer: ")
    // Click to execute command:
    .clickRun("<green><bold>[Claim Kit]</bold></green>", "/kit starter", "<gray>Click to claim kit")
    .space()
    // Click to prefill chatbox:
    .clickSuggest("<aqua>[Message]</aqua>", "/msg mukul ", "<gray>Click to reply")
    .space()
    // Click to copy text:
    .clickCopy("<light_purple>[IP]</light_purple>", "play.myserver.net", "<gray>Copy server IP")
    .space()
    // Click to open URL:
    .clickUrl("<gold>[Discord]</gold>", "https://discord.gg/minecraft", "<gray>Open Discord")
    .space()
    // Click to execute an in-memory Java lambda callback directly!
    .clickAction("<yellow>[ Mystery Box]</yellow>", p -> {
        Players.sound(p, Sound.ENTITY_PLAYER_LEVELUP);
        Players.msg(p, "<green> You triggered a live Java lambda action!");
    }, "<gray>Click for surprise")
    .send(player);
```

### 2. 1-Line Confirmations (`chat.confirm`)
Automates clickable `[CONFIRM]` and `[CANCEL]` buttons with automatic single-use token invalidation and timeouts:
```java
chat.confirm(player)
    .question("<gold>Purchase <yellow>Fly Perk</yellow> for <green>$5,000</green>?")
    .accept("<green><bold>[CONFIRM]</bold></green>", p -> {
        Players.msg(p, "<green>Perk activated!");
    })
    .deny("<red><bold>[CANCEL]</bold></red>", p -> {
        Players.msg(p, "<red>Purchase canceled.");
    })
    .timeout(Duration.ofSeconds(30))
    .send();
```

### 3. Private Chat Input Prompts (`chat.prompt`)
Hides player's next chat message from everyone else and delivers it to a callback:
```java
// General text prompt:
chat.prompt(player, "<yellow>Type your warp name in chat (or 'cancel'):", (p, name) -> {
    Players.msg(p, "<green>Warp '" + name + "' created!");
});

// Validated number prompt (with auto-bounds checking and re-prompting):
chat.promptInteger(player, "<yellow>Enter deposit amount (1 - 1000):", 1, 1000, (p, amount) -> {
    Players.msg(p, "<green>Deposited $" + amount);
});
```

### 4. Paginated Lists with Clickable Page Controls (`chat.pager`)
```java
chat.pager(player, "Server Warps", warpList)
    .pageSize(7)
    .header("<gold> %title% <gray>(Page %page%/%max%)</gray> ")
    .formatter((index, warp) -> "<yellow>#" + index + " <white>" + warp)
    .send(1); // Clickable [◀ Previous] & [Next ▶] buttons automatically work!
```

### 5. Pixel-Perfect Centered Text
```java
chat.sendCentered(player, "<gradient:#ff5555:#ffaa00><bold> MYTHIC EVENT </bold></gradient>");
chat.sendCentered(player, "<gray>Double EXP is now live!</gray>");
```

---

## CommandHelper (Fluent Commands)

Build and register dynamic commands fluently with automatic player/console isolation, permission checks, custom arguments, and subcommands.

### Auto-Injection
```java
private CommandHelper commands; // Automatically injected!
```

### 1. Basic Player Command
```java
commands.create("fly")
    .description("Toggle creative flight")
    .permission("script.fly")
    .executesPlayer(ctx -> {
        Player player = ctx.getPlayer();
        boolean allow = !player.getAllowFlight();
        player.setAllowFlight(allow);
        Players.msg(player, "<green>Flight " + (allow ? "enabled" : "disabled"));
    })
    .register();
```

### 2. Commands with Typed Arguments
```java
commands.create("heal")
    .description("Heal yourself or another player")
    .permission("script.heal")
    .optionalArgument(CommandArgument.player("target"))
    .executesPlayer(ctx -> {
        Player target = ctx.hasArgument("target") 
            ? ctx.getArgument("target", Player.class) 
            : ctx.getPlayer();
        
        Players.heal(target);
        Players.msg(target, "<green>You have been healed!");
        if (target != ctx.getPlayer()) {
            Players.msg(ctx.getPlayer(), "<green>Healed " + target.getName());
        }
    })
    .register();
```

### 3. Subcommands & Tab Completion
```java
commands.create("warp")
    .description("Server warp system")
    .subcommand("set", sub -> sub
        .permission("script.warp.admin")
        .argument(CommandArgument.string("name"))
        .executesPlayer(ctx -> {
            String name = ctx.getArgument("name", String.class);
            Players.msg(ctx.getPlayer(), "<green>Warp '" + name + "' created.");
        })
    )
    .subcommand("tp", sub -> sub
        .argument(CommandArgument.string("name"))
        .executesPlayer(ctx -> {
            String name = ctx.getArgument("name", String.class);
            // Teleport logic
        })
    )
    .register();
```

---

## RecipeHelper (Custom Recipes)

Create custom crafting table recipes, furnace recipes, and smithing upgrades that automatically unregister when the script reloads or unloads.

### Auto-Injection
```java
private RecipeHelper recipes; // Automatically injected!
```

### 1. Shaped Recipe
```java
ItemStack excalibur = Items.create(Material.NETHERITE_SWORD, "<gradient:#ffaa00:#ff5555><bold>EXCALIBUR</bold></gradient>");

recipes.shaped("excalibur", excalibur)
    .shape(
        " N ",
        " N ",
        " S "
    )
    .set('N', Material.NETHERITE_INGOT)
    .set('S', Material.STICK)
    .register();
```

### 2. Shapeless Recipe
```java
ItemStack superApple = Items.create(Material.ENCHANTED_GOLDEN_APPLE);

recipes.shapeless("super_apple", superApple)
    .add(Material.GOLD_BLOCK, 8)
    .add(Material.APPLE, 1)
    .register();
```

### 3. Smelting, Campfire & Stonecutting Recipes
```java
// Furnace recipe: 100 ticks (5 seconds), 1.0 XP
recipes.furnace("quick_iron", new ItemStack(Material.IRON_INGOT), Material.RAW_IRON)
    .cookingTime(100)
    .experience(1.0f)
    .register();

// Blast furnace: 50 ticks (2.5 seconds)
recipes.blasting("quick_blast", new ItemStack(Material.IRON_INGOT), Material.RAW_IRON)
    .cookingTime(50)
    .register();

// Smoker: fast food cooking
recipes.smoking("crispy_steak", new ItemStack(Material.COOKED_BEEF), Material.BEEF)
    .cookingTime(50)
    .register();

// Campfire recipe
recipes.campfire("roasted_marshmallow", new ItemStack(Material.SUGAR), Material.SLIME_BALL)
    .cookingTime(200)
    .register();

// Stonecutter recipe
recipes.stonecutting("carved_andesite", new ItemStack(Material.POLISHED_ANDESITE), Material.ANDESITE)
    .register();
```

---

## EconomyHelper & `Economy` (Universal Economy Engine)

A complete, zero-compromise Economy engine that bridges **Vault** (EssentialsX, CMI, UltraEconomy, etc.), JavaSkript's **Built-in SQLite Persistent Storage**, and **Custom Multi-Currencies** (Gems, Tokens, Credits).

### Auto-Injection & Static Facade
```java
// Option A: Auto-injected in any script
private EconomyHelper economy;

// Option B: Static 1-line facade anywhere
Economy.getBalance(player);
```

### 1. Balance Checks & Formatted Strings
```java
double bal = Economy.getBalance(player);

// Full formatted currency with commas:
String formatted = Economy.format(1500.50);      // "$1,500.50"

// Compact suffix format:
String compact = Economy.formatShort(1500000);   // "$1.5M"
```

### 2. Transactions (Withdraw, Deposit, Set)
```java
// Check balance:
if (Economy.has(player, 250.0)) {
    EconomyResult result = Economy.withdraw(player, 250.0);
    if (result.isSuccess()) {
        Players.msg(player, "<green>Paid " + Economy.format(250.0));
    }
}

// Deposit:
Economy.deposit(player, 500.0);

// Set absolute balance:
Economy.set(player, 1000.0);
```

### 3. Safe Atomic Player Payments (`Economy.transfer`)
Transfers money between two players with transactional rollback protection:
```java
EconomyResult result = Economy.transfer(sender, receiver, 100.0);
if (result.isSuccess()) {
    Players.msg(sender, "<green>Sent $100 to " + receiver.getName());
    Players.msg(receiver, "<green>Received $100 from " + sender.getName());
} else {
    Players.msg(sender, "<red>Payment failed: " + result.getErrorMessage());
}
```

### 4. Leaderboard (`Economy.getTopBalances`)
```java
// Get top 10 richest players on the server (when using built-in engine):
List<Map.Entry<UUID, Double>> top = Economy.getTopBalances(10);
for (Map.Entry<UUID, Double> entry : top) {
    OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
    String line = p.getName() + ": " + Economy.format(entry.getValue());
}
```

### 5. Multi-Currency Support (Gems, Tokens, Credits)

You can create and register secondary currencies alongside the main server economy.

#### A. Creating a Custom Currency Provider
Implement `EconomyProvider` (storing data in memory, SQLite, or player PDC):
```java
public class GemsCurrencyProvider implements EconomyProvider {
    private final Map<UUID, Double> gems = new ConcurrentHashMap<>();

    @Override public String getName() { return "Gems"; }
    @Override public String getCurrencySymbol() { return "GEMS"; }
    @Override public String getCurrencySingular() { return "Gem"; }
    @Override public String getCurrencyPlural() { return "Gems"; }

    @Override public double getBalance(UUID uuid) { return gems.getOrDefault(uuid, 0.0); }

    @Override
    public EconomyResult withdraw(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) return EconomyResult.fail("Insufficient gems", current);
        double next = current - amount;
        gems.put(uuid, next);
        return EconomyResult.success(amount, next);
    }

    @Override
    public EconomyResult deposit(UUID uuid, double amount) {
        double next = getBalance(uuid) + amount;
        gems.put(uuid, next);
        return EconomyResult.success(amount, next);
    }

    @Override
    public EconomyResult set(UUID uuid, double amount) {
        gems.put(uuid, amount);
        return EconomyResult.success(amount, amount);
    }

    @Override public String format(double amount) { return "GEMS" + (int) amount; }
    @Override public String formatShort(double amount) { return format(amount); }
}
```

#### B. Registering the Custom Currency
```java
// Register in your script's onEnable():
economy.registerCurrency("gems", new GemsCurrencyProvider());
```

#### C. Multi-Balance Hybrid Transaction (Coins + Gems)
Purchase an item requiring **both** $5,000 Coins AND 25 Gems with full rollback safety:
```java
double costCoins = 5000.0;
double costGems = 25.0;

EconomyProvider gems = Economy.currency("gems").orElseThrow();

// 1. Verify player can afford both
if (!Economy.has(player, costCoins) || !gems.has(player, costGems)) {
    Players.msg(player, "<red>You need " + Economy.format(costCoins) + " and " + gems.format(costGems) + "!");
    return;
}

// 2. Perform atomic multi-balance withdrawal
EconomyResult resCoins = Economy.withdraw(player, costCoins);
EconomyResult resGems = gems.withdraw(player, costGems);

if (resCoins.isSuccess() && resGems.isSuccess()) {
    Players.msg(player, "<green> Purchased Mythic Relic with Coins & Gems!");
    Players.sound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
    // Give item...
} else {
    // Transaction failed - rollback whichever was charged
    if (resCoins.isSuccess()) Economy.deposit(player, costCoins);
    if (resGems.isSuccess()) gems.deposit(player, costGems);
    Players.msg(player, "<red> Transaction failed. Funds refunded.");
}
```

#### D. Currency Exchange (Convert Coins to Gems)
Convert $10,000 Coins into 10 Gems:
```java
if (Economy.has(player, 10000.0)) {
    Economy.withdraw(player, 10000.0);
    gems.deposit(player, 10.0);
    Players.msg(player, "<green>Converted $10,000 Coins into 10 Gems!");
}
```

---

## External Plugin & Addon Integration

JavaSkript provides a fully open API and addon architecture allowing other Bukkit/Paper plugins to interact with scripts, register custom field injectors, listen to script lifecycle events, and call script methods dynamically.

### 1. Static Access

External plugins can access JavaSkript's API directly through the static `JavaSkript` facade without needing plugin casting:

```java
import dev.mukulx.javaskript.JavaSkript;
import dev.mukulx.javaskript.api.JavaSkriptAPI;

if (JavaSkript.isAvailable()) {
    JavaSkriptAPI api = JavaSkript.getAPI();
    // Access all JavaSkript systems
}
```

### 2. Addon Registration

External plugins can register themselves as formal JavaSkript addons with metadata and lifecycle hooks:

```java
import dev.mukulx.javaskript.JavaSkript;
import dev.mukulx.javaskript.api.addon.JavaSkriptAddon;
import dev.mukulx.javaskript.api.JavaSkriptAPI;

public class MyDiscordBridgeAddon implements JavaSkriptAddon {

    @Override
    public String getName() {
        return "JavaSkript-Discord";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "Developer";
    }

    @Override
    public String getDescription() {
        return "Bridges Discord bot features directly into JavaSkript scripts";
    }

    @Override
    public void onEnable(JavaSkriptAPI api) {
        // Register custom field injectors or services
    }

    @Override
    public void onDisable(JavaSkriptAPI api) {
        // Cleanup resources
    }
}

// Register during plugin onEnable:
JavaSkript.registerAddon(new MyDiscordBridgeAddon());
```

Registered addons are visible in-game via `/js addons`.

### 3. Custom Field Injectors

Addons and external plugins can register custom field injectors. Any `.java` script that declares a field matching the registered class type or field name will automatically receive the injected instance:

```java
// Option A: Register by Class Type
JavaSkript.registerInjector(DiscordService.class, (scriptInstance, fieldType, fieldName) -> {
    return new DiscordService(scriptInstance.getScriptKey());
});

// Option B: Register by Field Name
JavaSkript.registerInjector("discord", (scriptInstance, fieldType, fieldName) -> {
    return new DiscordService(scriptInstance.getScriptKey());
});
```

Scripts can then use the custom addon API with zero boilerplate:

```java
// Inside any script: MyScript.java
public class MyScript implements Listener {

    private DiscordService discord; // Automatically injected by your addon!

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        discord.sendMessage("Player " + event.getPlayer().getName() + " joined!");
    }
}
```

If the injected object implements `java.lang.AutoCloseable`, JavaSkript will automatically call `close()` when the script unloads or reloads!

### 4. Bukkit Lifecycle Events

Other plugins can listen to standard Bukkit events fired across the script lifecycle:

| Event | Description |
|-------|-------------|
| `ScriptPreCompileEvent` | Cancellable event fired before compilation. Addons can inspect or modify source code (`setSourceCode(...)`) or cancel compilation (`setCancelled(true)`). |
| `ScriptLoadEvent` | Fired when a script has successfully compiled, initialized, and registered events/commands. |
| `ScriptUnloadEvent` | Fired immediately before a script is disabled and unloaded. |
| `ScriptReloadEvent` | Fired when an existing script is replaced and reloaded. |

Example listener in your plugin:

```java
import dev.mukulx.javaskript.event.ScriptLoadEvent;
import dev.mukulx.javaskript.event.ScriptUnloadEvent;
import dev.mukulx.javaskript.event.ScriptPreCompileEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class JavaSkriptEventListener implements Listener {

    @EventHandler
    public void onPreCompile(ScriptPreCompileEvent event) {
        String scriptKey = event.getScriptKey();
        // Addons can preprocess source code before ECJ compiles it
        String code = event.getSourceCode();
        if (code.contains("// @custom-tag")) {
            event.setSourceCode(code.replace("// @custom-tag", "// processed"));
        }
    }

    @EventHandler
    public void onScriptLoad(ScriptLoadEvent event) {
        getLogger().info("Script loaded: " + event.getScriptKey());
    }

    @EventHandler
    public void onScriptUnload(ScriptUnloadEvent event) {
        getLogger().info("Script unloading: " + event.getScriptKey());
    }
}
```

### 5. Inter-Plugin Script Invocation (`call`)

External plugins can execute public methods on active scripts without needing class references at compile time:

```java
JavaSkriptAPI api = JavaSkript.getAPI();

// Call public method 'grantReward(Player, int)' in script 'rewards/DailyRewards.java'
try {
    Object result = api.call("rewards/DailyRewards", "grantReward", player, 500);
} catch (Exception e) {
    getLogger().warning("Failed to invoke script method: " + e.getMessage());
}
```

---

## VariableHelper & `Variables` (Shared State)

Thread-safe shared variable storage engine for JavaSkript scripts.

Because each script is isolated in its own ClassLoader, `Variables` provides a global, thread-safe memory store accessible by every script on the server. It also supports disk persistence so data can survive server restarts.

### Auto-Injection & Static Facade
```java
// Option A: Injected helper
private VariableHelper variables;
// Aliases: private VariableHelper vars; / state; / shared;

// Option B: Static facade (usable anywhere, even in utility classes)
Variables.set("jackpot", 5000);
int jackpot = Variables.getInt("jackpot");
```

### 1. In-Memory Variables
Fast, sub-microsecond in-memory key-value cache:
```java
// Set any Java or Bukkit object
variables.set("event_active", true);
variables.set("spawn_point", player.getLocation());

// Get with type safety and fallback default values
boolean active = variables.getBoolean("event_active", false);
Location spawn = variables.get("spawn_point", Location.class);
String motd = variables.getString("server_motd", "Welcome!");
```

### 2. Atomic Counters & Math
Thread-safe atomic updates without locks:
```java
// Atomic increment & decrement
long joins = variables.increment("total_joins", 1);
double prizePool = variables.increment("lottery_pool", 25.50);
long remaining = variables.decrement("lives", 1);
```

### 3. Persistent Variables (Survives Server Restarts)
Saved atomically to `plugins/JavaSkript/script-data/variables.json`:
```java
// Saved to disk asynchronously, survives restarts
variables.setPersistent("global_jackpot", 150000);

// Read persisted value
int jackpot = variables.getPersistentInt("global_jackpot", 10000);

// Remove persisted key
variables.removePersistent("global_jackpot");
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

**Last Updated:** 2026-08-31
