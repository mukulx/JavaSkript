# JavaSkript Tutorial - Complete Guide

This comprehensive tutorial will teach you everything you need to know about creating scripts with JavaSkript.

## Table of Contents

1. [Introduction](#introduction)
2. [Your First Script](#your-first-script)
3. [Understanding Script Structure](#understanding-script-structure)
4. [Event Listeners](#event-listeners)
5. [Custom Commands](#custom-commands)
6. [Working with Players](#working-with-players)
7. [Working with Worlds](#working-with-worlds)
8. [Scheduled Tasks](#scheduled-tasks)
9. [Data Persistence](#data-persistence)
10. [Error Handling](#error-handling)
11. [Best Practices](#best-practices)

---

## Introduction

JavaSkript allows you to write Java code in simple `.java` files that are automatically compiled and loaded by the plugin. Unlike traditional plugin development, you don't need to:

- Set up a development environment
- Compile your code manually
- Restart the server to test changes

Just write your Java code, save it, and run `/js reload`!

---

## Your First Script

Let's create a simple script that sends a message when a player joins.

### Step 1: Create the File

Navigate to `plugins/JavaSkript/scripts/` and create a file called `myfirst.java`.

### Step 2: Write the Code

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MyFirstScript implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Always check for null!
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        player.sendMessage(
            Component.text("Hello from JavaSkript!")
                .color(NamedTextColor.GOLD)
        );
    }
}
```

### Step 3: Load the Script

In-game, run:
```
/js reload myfirst
```

Or reload all scripts:
```
/js reload
```

### Step 4: Test It

Join the server and you should see "Hello from JavaSkript!" in gold text!

---

## Understanding Script Structure

Every JavaSkript file must follow this structure:

```java
// 1. Imports - Import the classes you need
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

// 2. Public Class - Must be public and have a descriptive name
public class MyScript implements Listener {

    // 3. Methods - Your event handlers or command logic
    @EventHandler
    public void onSomeEvent(SomeEvent event) {
        // Always check for null
        if (event == null) return;
        
        // Your code here
    }
}
```

### Key Rules:

1. **Class must be public** - `public class MyScript`
2. **Implement appropriate interfaces**:
   - `Listener` for event handling
   - `CommandExecutor` for commands
   - `TabCompleter` for tab completion
3. **Always check for null** - Prevents crashes
4. **Use descriptive names** - Makes debugging easier

---

## Event Listeners

Event listeners allow your script to react to things happening in the game.

### Basic Event Listener

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;

public class EventExample implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        System.out.println(player.getName() + " joined!");
    }
}
```

### Multiple Events in One Script

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.*;
import org.bukkit.event.Listener;

public class MultiEventScript implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event == null) return;
        System.out.println("Player joined");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event == null) return;
        System.out.println("Player quit");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event == null) return;
        System.out.println("Player chatted");
    }
}
```

### Event Priority

Control when your event handler runs:

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;

public class PriorityExample implements Listener {

    // Runs first
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinFirst(PlayerJoinEvent event) {
        if (event == null) return;
        System.out.println("I run first!");
    }

    // Runs last
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinLast(PlayerJoinEvent event) {
        if (event == null) return;
        System.out.println("I run last!");
    }
}
```

### Cancelling Events

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.Listener;

public class CancelExample implements Listener {

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event == null) return;
        
        // Prevent players from dropping items
        event.setCancelled(true);
        
        if (event.getPlayer() != null) {
            event.getPlayer().sendMessage("You cannot drop items!");
        }
    }
}
```

### Common Events

| Event | When it fires |
|-------|---------------|
| `PlayerJoinEvent` | Player joins server |
| `PlayerQuitEvent` | Player leaves server |
| `PlayerMoveEvent` | Player moves |
| `PlayerInteractEvent` | Player right/left clicks |
| `AsyncPlayerChatEvent` | Player sends chat message |
| `PlayerDeathEvent` | Player dies |
| `BlockBreakEvent` | Block is broken |
| `BlockPlaceEvent` | Block is placed |
| `EntityDamageEvent` | Entity takes damage |
| `InventoryClickEvent` | Player clicks in inventory |

---

## Custom Commands

Commands let players interact with your script.

### Basic Command

First, add the command to `plugin.yml`:

```yaml
commands:
  mycommand:
    description: My custom command
    usage: /mycommand
```

Then create the script:

```java
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.List;

public class MyCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this!").color(NamedTextColor.RED));
            return true;
        }
        
        player.sendMessage(Component.text("You used my command!").color(NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }
}
```

### Command with Arguments

```java
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.List;

public class GiveCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players!");
            return true;
        }
        
        // Check arguments
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /give <item>").color(NamedTextColor.RED));
            return true;
        }
        
        String itemName = args[0].toUpperCase();
        
        try {
            var material = org.bukkit.Material.valueOf(itemName);
            var item = new org.bukkit.inventory.ItemStack(material);
            player.getInventory().addItem(item);
            player.sendMessage(Component.text("Given " + itemName).color(NamedTextColor.GREEN));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid item!").color(NamedTextColor.RED));
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("DIAMOND", "GOLD_INGOT", "IRON_INGOT", "EMERALD");
        }
        return List.of();
    }
}
```

### Permission Checking

```java
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // Check permission
    if (!sender.hasPermission("myplugin.admin")) {
        sender.sendMessage(Component.text("No permission!").color(NamedTextColor.RED));
        return true;
    }
    
    // Command logic here
    return true;
}
```

---

## Working with Players

### Getting Player Information

```java
import org.bukkit.entity.Player;

public void playerInfo(Player player) {
    if (player == null) return;
    
    // Basic info
    String name = player.getName();
    var uuid = player.getUniqueId();
    var location = player.getLocation();
    
    // Health and food
    double health = player.getHealth();
    double maxHealth = player.getMaxHealth();
    int foodLevel = player.getFoodLevel();
    
    // Game mode
    var gameMode = player.getGameMode();
    
    // Experience
    int level = player.getLevel();
    float exp = player.getExp();
    
    // Permissions
    boolean isOp = player.isOp();
    boolean hasPerm = player.hasPermission("some.permission");
}
```

### Modifying Players

```java
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public void modifyPlayer(Player player) {
    if (player == null) return;
    
    // Health and food
    player.setHealth(20.0);
    player.setFoodLevel(20);
    player.setSaturation(20f);
    
    // Game mode
    player.setGameMode(GameMode.CREATIVE);
    
    // Flight
    player.setAllowFlight(true);
    player.setFlying(true);
    
    // Experience
    player.setLevel(100);
    player.setExp(0.5f);
    
    // Potion effects
    player.addPotionEffect(new PotionEffect(
        PotionEffectType.SPEED,
        200, // Duration in ticks (10 seconds)
        1    // Amplifier (Speed II)
    ));
    
    // Teleport
    var spawn = player.getWorld().getSpawnLocation();
    player.teleport(spawn);
}
```

### Sending Messages

```java
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import java.time.Duration;

public void sendMessages(Player player) {
    if (player == null) return;
    
    // Simple message
    player.sendMessage("Hello!");
    
    // Colored message
    player.sendMessage(
        Component.text("Hello!").color(NamedTextColor.GOLD)
    );
    
    // Formatted message
    player.sendMessage(
        Component.text("Important!", NamedTextColor.RED, TextDecoration.BOLD)
    );
    
    // Combined message
    player.sendMessage(
        Component.text("Welcome ", NamedTextColor.GOLD)
            .append(Component.text(player.getName(), NamedTextColor.YELLOW))
            .append(Component.text("!", NamedTextColor.GOLD))
    );
    
    // Action bar
    player.sendActionBar(
        Component.text("Action bar message").color(NamedTextColor.GREEN)
    );
    
    // Title
    player.showTitle(Title.title(
        Component.text("Big Title", NamedTextColor.GOLD, TextDecoration.BOLD),
        Component.text("Subtitle", NamedTextColor.YELLOW),
        Title.Times.times(
            Duration.ofMillis(500),  // Fade in
            Duration.ofSeconds(3),   // Stay
            Duration.ofMillis(500)   // Fade out
        )
    ));
}
```

---

## Working with Worlds

### World Information

```java
import org.bukkit.World;
import org.bukkit.Bukkit;

public void worldInfo() {
    // Get all worlds
    for (World world : Bukkit.getWorlds()) {
        if (world == null) continue;
        
        String name = world.getName();
        var environment = world.getEnvironment(); // NORMAL, NETHER, THE_END
        long time = world.getTime();
        boolean pvp = world.getPVP();
        
        System.out.println("World: " + name);
    }
    
    // Get specific world
    World world = Bukkit.getWorld("world");
    if (world != null) {
        // Use world
    }
}
```

### Modifying Worlds

```java
import org.bukkit.World;
import org.bukkit.Difficulty;

public void modifyWorld(World world) {
    if (world == null) return;
    
    // Time
    world.setTime(1000); // Morning
    world.setTime(13000); // Night
    
    // Weather
    world.setStorm(true);
    world.setThundering(true);
    world.setWeatherDuration(6000); // 5 minutes
    
    // Difficulty
    world.setDifficulty(Difficulty.HARD);
    
    // Game rules
    world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
    world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
    
    // Save world
    world.save();
}
```

---

## Scheduled Tasks

Run code repeatedly or after a delay.

### Delayed Task

```java
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public void delayedTask() {
    new BukkitRunnable() {
        @Override
        public void run() {
            System.out.println("This runs after 5 seconds!");
        }
    }.runTaskLater(
        Bukkit.getPluginManager().getPlugin("JavaSkript"),
        100L // 5 seconds (20 ticks = 1 second)
    );
}
```

### Repeating Task

```java
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public void repeatingTask() {
    new BukkitRunnable() {
        int count = 0;
        
        @Override
        public void run() {
            count++;
            System.out.println("Count: " + count);
            
            // Stop after 10 times
            if (count >= 10) {
                this.cancel();
            }
        }
    }.runTaskTimer(
        Bukkit.getPluginManager().getPlugin("JavaSkript"),
        0L,    // Start immediately
        20L    // Repeat every second
    );
}
```

---

## Data Persistence

Save data that persists across server restarts.

### Using Persistent Data Container

```java
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;

public class DataExample {
    
    private static final NamespacedKey COINS_KEY = 
        new NamespacedKey(Bukkit.getPluginManager().getPlugin("JavaSkript"), "coins");
    
    public void saveCoins(Player player, int coins) {
        if (player == null) return;
        
        var pdc = player.getPersistentDataContainer();
        pdc.set(COINS_KEY, PersistentDataType.INTEGER, coins);
    }
    
    public int getCoins(Player player) {
        if (player == null) return 0;
        
        var pdc = player.getPersistentDataContainer();
        return pdc.getOrDefault(COINS_KEY, PersistentDataType.INTEGER, 0);
    }
}
```

---

## Error Handling

Always handle errors gracefully to prevent crashes.

### Try-Catch Blocks

```java
@EventHandler
public void onEvent(SomeEvent event) {
    try {
        // Code that might fail
        riskyOperation();
    } catch (Exception e) {
        // Log the error
        System.err.println("Error in script: " + e.getMessage());
        e.printStackTrace();
        
        // Notify player if applicable
        if (event.getPlayer() != null) {
            event.getPlayer().sendMessage("An error occurred!");
        }
    }
}
```

### Null Checks

```java
// ✅ Good - Always check for null
if (player != null && player.getInventory() != null) {
    player.getInventory().addItem(item);
}

// ❌ Bad - Can cause NullPointerException
player.getInventory().addItem(item);
```

---

## Best Practices

### 1. Always Check for Null

```java
@EventHandler
public void onEvent(PlayerEvent event) {
    // ✅ Always do this
    if (event == null || event.getPlayer() == null) return;
    
    var player = event.getPlayer();
    // Now safe to use player
}
```

### 2. Use Modern Java Syntax

```java
// ✅ Use var for local variables
var player = event.getPlayer();
var location = player.getLocation();

// ✅ Use instanceof with pattern matching
if (sender instanceof Player player) {
    player.sendMessage("Hello!");
}

// ✅ Use List.of() for immutable lists
return List.of("option1", "option2", "option3");
```

### 3. Descriptive Names

```java
// ✅ Good names
public class WelcomeMessageScript implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // ...
    }
}

// ❌ Bad names
public class Script1 implements Listener {
    @EventHandler
    public void a(PlayerJoinEvent e) {
        // ...
    }
}
```

### 4. Comment Your Code

```java
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    if (event == null || event.getPlayer() == null) return;
    
    var player = event.getPlayer();
    
    // Give starter items to new players
    if (!player.hasPlayedBefore()) {
        giveStarterItems(player);
    }
    
    // Teleport to spawn
    teleportToSpawn(player);
}
```

### 5. Keep Scripts Focused

Each script should do one thing well. Don't create massive scripts that do everything.

✅ Good:
- `WelcomeMessages.java` - Handles join/quit messages
- `AntiSpam.java` - Prevents chat spam
- `StarterKit.java` - Gives items to new players

❌ Bad:
- `Everything.java` - Does all of the above

---

## Next Steps

Now that you understand the basics, check out:

- [EXAMPLES.md](EXAMPLES.md) - More complex examples
- [README.md](README.md) - Full documentation
- [Bukkit API Docs](https://hub.spigotmc.org/javadocs/bukkit/) - Complete API reference

Happy scripting! 🎉
