# Multiple Classes in One File

JavaSkript supports multiple classes in a single file, allowing you to organize related code together without creating separate files.

## Overview

You can include:
- **One public class** (required) - The main class that gets registered
- **Multiple package-private classes** - Helper classes, managers, utilities
- **Inner classes** (static or non-static) - Nested within the main class
- **Data classes** - Simple POJOs for storing data

**Important:** Each script gets its own isolated ClassLoader, which means:
- Classes with the same name in different scripts won't conflict
- `MyScript1.java` can have a `Utils` class
- `MyScript2.java` can also have a `Utils` class
- They are completely separate and won't interfere with each other

## Basic Example

```java
// MyScript.java - Everything in one file

@FoliaSupport
public class MyScript implements Listener {
    private Utils utils = new Utils();
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        utils.sendWelcome(event.getPlayer());
    }
}

// Package-private utility class
class Utils {
    void sendWelcome(Player player) {
        player.sendMessage("Welcome!");
    }
}
```

## Rules

### 1. One Public Class Required

Only ONE class can be `public` - this is the main class that JavaSkript registers:

```java
// ✅ Good - one public class
public class MyScript implements Listener {
    // main logic
}

class Helper {
    // helper logic
}

// ❌ Bad - multiple public classes (Java doesn't allow this)
public class MyScript implements Listener { }
public class MyOtherScript implements Listener { } // ERROR
```

### 2. Public Class Name Must Match File Name

```java
// File: MyScript.java
public class MyScript implements Listener {  // ✅ Matches file name
    // ...
}
```

### 3. Package-Private Classes Don't Need 'public'

```java
// No 'public' keyword - these are package-private
class Manager { }
class Utils { }
class Data { }
```

## Patterns

### Pattern 1: Manager Classes

Organize logic into separate manager classes:

```java
@FoliaSupport
public class EconomyScript implements Listener, CommandExecutor {
    private PlayerManager playerManager = new PlayerManager();
    private ShopManager shopManager = new ShopManager();
    private BankManager bankManager = new BankManager();
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerManager.handleJoin(event.getPlayer());
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args[0].equals("shop")) {
            return shopManager.openShop((Player) sender);
        }
        return true;
    }
}

class PlayerManager {
    private Map<UUID, Account> accounts = new HashMap<>();
    
    void handleJoin(Player player) {
        accounts.putIfAbsent(player.getUniqueId(), new Account(player.getName()));
    }
    
    Account getAccount(UUID uuid) {
        return accounts.get(uuid);
    }
}

class ShopManager {
    boolean openShop(Player player) {
        // shop logic
        return true;
    }
}

class BankManager {
    void deposit(UUID player, int amount) {
        // bank logic
    }
}

class Account {
    private String name;
    private int balance;
    
    Account(String name) {
        this.name = name;
        this.balance = 0;
    }
    
    int getBalance() {
        return balance;
    }
    
    void addBalance(int amount) {
        balance += amount;
    }
}
```

### Pattern 2: Static Utility Classes

Use static methods for utilities:

```java
@FoliaSupport
public class MyScript implements Listener {
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Utils.broadcast("Player joined: " + event.getPlayer().getName());
        Utils.log("Join event processed");
    }
}

class Utils {
    static void broadcast(String message) {
        Bukkit.broadcast(Component.text(message));
    }
    
    static void log(String message) {
        System.out.println("[MyScript] " + message);
    }
    
    static String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(millis));
    }
}
```

### Pattern 3: Inner Classes

Use inner classes for tight coupling:

```java
@FoliaSupport
public class MyScript implements Listener {
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var handler = new JoinHandler();
        handler.process(event);
    }
    
    // Non-static inner class (can access outer class fields)
    class JoinHandler {
        void process(PlayerJoinEvent event) {
            // can access MyScript's fields
        }
    }
    
    // Static inner class (independent)
    static class Config {
        static String getMessage(String key) {
            return "message";
        }
    }
}
```

### Pattern 4: Data Classes

Simple classes for storing data:

```java
@FoliaSupport
public class PlayerStatsScript implements Listener {
    private Map<UUID, PlayerStats> stats = new HashMap<>();
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        stats.put(player.getUniqueId(), new PlayerStats(player.getName()));
    }
}

class PlayerStats {
    private String name;
    private int kills;
    private int deaths;
    private long playtime;
    
    PlayerStats(String name) {
        this.name = name;
        this.kills = 0;
        this.deaths = 0;
        this.playtime = 0;
    }
    
    // Getters and setters
    String getName() { return name; }
    int getKills() { return kills; }
    void addKill() { kills++; }
    void addDeath() { deaths++; }
}
```

## Benefits

### ✅ Better Organization
Keep related code together instead of scattered across multiple files.

### ✅ Easier to Manage
One file per feature instead of many small files.

### ✅ Cleaner Scripts Folder
Fewer files to navigate.

### ✅ Standard Java
This is how Java normally works - no special syntax to learn.

### ✅ IDE Support
IDEs understand this pattern perfectly - auto-completion, refactoring, etc. all work.

### ✅ No Class Name Conflicts
Each script has its own isolated ClassLoader, so you can use common names like `Utils`, `Manager`, `Config` without worrying about conflicts with other scripts.

**Example:** Both `EconomyScript.java` and `ShopScript.java` can have their own `Manager` class - they won't conflict because each script is isolated.

## Complete Example

See [MultiClassExample.java](../src/main/resources/examples/MultiClassExample.java) for a complete working example with:
- Main listener/command class
- PlayerManager for managing players
- MessageManager for sending messages
- PlayerData for storing player information

## Common Mistakes

### ❌ Using APIs in Package-Private Class Constructors

```java
public class MyScript implements Listener {
    private ScriptScheduler scheduler;  // Auto-injected
    private Manager manager = new Manager();
}

class Manager {
    Manager() {
        // ❌ Can't access MyScript's scheduler here
        // scheduler is not accessible from this class
    }
}
```

**Solution:** Pass dependencies explicitly:

```java
public class MyScript implements Listener {
    private ScriptScheduler scheduler;
    private Manager manager;
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!event.getPlugin().getName().equals("JavaSkript")) return;
        
        // ✅ Create manager after APIs are injected
        this.manager = new Manager(scheduler);
    }
}

class Manager {
    private ScriptScheduler scheduler;
    
    Manager(ScriptScheduler scheduler) {
        this.scheduler = scheduler;
    }
}
```

### ❌ Trying to Register Package-Private Classes

```java
public class MyScript implements Listener { }

// ❌ This won't be registered as a listener
class MyOtherListener implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // This won't be called
    }
}
```

**Solution:** Only the public class is registered. Put all event handlers in the public class:

```java
public class MyScript implements Listener {
    private Handler handler = new Handler();
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handler.process(event);  // ✅ Delegate to helper
    }
}

class Handler {
    void process(PlayerJoinEvent event) {
        // Process the event
    }
}
```

## Tips

1. **Keep the public class simple** - Use it as a coordinator
2. **Put complex logic in helper classes** - Easier to test and maintain
3. **Use descriptive names** - `PlayerManager`, `ShopManager`, not `Manager1`, `Manager2`
4. **Group related classes** - All shop-related classes in one file
5. **Don't overdo it** - If a file gets too large (>500 lines), consider splitting

## Quick Reference

### File Structure Template

```java
// MyScript.java

import dev.mukulx.javaskript.script.FoliaSupport;
import org.bukkit.event.*;
import org.bukkit.command.*;

// Main public class (required)
@FoliaSupport
public class MyScript implements Listener, CommandExecutor {
    private Manager manager = new Manager();
    
    @EventHandler
    public void onEvent(SomeEvent event) {
        manager.handle(event);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return manager.executeCommand(sender, args);
    }
}

// Helper class (package-private)
class Manager {
    void handle(SomeEvent event) {
        // logic
    }
    
    boolean executeCommand(CommandSender sender, String[] args) {
        // command logic
        return true;
    }
}

// Utility class (package-private, static methods)
class Utils {
    static void broadcast(String msg) {
        Bukkit.broadcast(Component.text(msg));
    }
}

// Data class (package-private)
class Data {
    private String value;
    
    Data(String value) {
        this.value = value;
    }
    
    String getValue() {
        return value;
    }
}
```

### Class Access Rules

| Class Type | Keyword | Accessible From | Use Case |
|------------|---------|-----------------|----------|
| Public | `public class` | Everywhere | Main script class (one per file) |
| Package-private | `class` | Same file only | Managers, utilities, data classes |
| Inner (non-static) | `class` inside public class | Outer class | Tightly coupled logic |
| Inner (static) | `static class` inside public class | Outer class | Independent utilities |

### What Gets Registered

| Class Type | Registered as Listener? | Registered as Command? |
|------------|------------------------|------------------------|
| Public class | ✅ Yes (if implements Listener) | ✅ Yes (if implements CommandExecutor) |
| Package-private class | ❌ No | ❌ No |
| Inner class | ❌ No | ❌ No |

**Only the public class is registered!** Other classes are helpers.

## Troubleshooting

### Error: "Could not find public class"

**Problem:** No public class in the file.

**Solution:** Ensure you have exactly one public class:

```java
// ✅ Good
public class MyScript implements Listener { }

// ❌ Bad - no public class
class MyScript implements Listener { }
```

### Error: "Main class not found"

**Problem:** Public class name doesn't match file name.

**Solution:** Match the names:

```java
// File: MyScript.java
public class MyScript implements Listener { }  // ✅ Names match
```

### Error: "Failed to define class"

**Problem:** Syntax error in one of the classes.

**Solution:** Check console for compilation errors. Fix syntax in all classes.

### Warning: "Compiled class file not found"

**Problem:** Class name in code doesn't match expected name.

**Solution:** Check for typos in class names.

### Events Not Firing from Helper Class

**Problem:** Trying to use @EventHandler in package-private class.

**Solution:** Only the public class can have event handlers. Delegate from public class:

```java
// ✅ Good
public class MyScript implements Listener {
    private Handler handler = new Handler();
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handler.process(event);  // Delegate to helper
    }
}

class Handler {
    void process(PlayerJoinEvent event) {
        // Process here
    }
}
```

### NullPointerException in Helper Class

**Problem:** Trying to access injected APIs from helper class.

**Solution:** Pass APIs explicitly:

```java
public class MyScript implements Listener {
    private ScriptScheduler scheduler;
    private Manager manager;
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!event.getPlugin().getName().equals("JavaSkript")) return;
        this.manager = new Manager(scheduler);  // Pass scheduler
    }
}

class Manager {
    private ScriptScheduler scheduler;
    
    Manager(ScriptScheduler scheduler) {
        this.scheduler = scheduler;
    }
}
```

### Can I use the same class name in different scripts?

**Yes!** Each script has its own isolated ClassLoader, so class names won't conflict.

**Example:**
```java
// File: EconomyScript.java
public class EconomyScript implements Listener { }
class Utils { }  // ✅ OK

// File: ShopScript.java  
public class ShopScript implements Listener { }
class Utils { }  // ✅ Also OK - different ClassLoader
```

Both scripts can have a `Utils` class without any conflicts. JavaSkript automatically isolates each script's classes.

## See Also

- [MultiClassExample.java](../src/main/resources/examples/MultiClassExample.java) - Complete example
- [API Documentation](API.md) - API reference
- [Tutorial](TUTORIAL.md) - Step-by-step guide
