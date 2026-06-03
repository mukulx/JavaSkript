# Script Lifecycle

This guide explains how JavaSkript manages script loading, unloading, and cleanup. Understanding the lifecycle helps you write clean, efficient scripts that properly manage resources.

## Table of Contents

- [Lifecycle Overview](#lifecycle-overview)
- [Lifecycle Methods](#lifecycle-methods)
- [Best Practices](#best-practices)
- [Resource Management](#resource-management)
- [Common Patterns](#common-patterns)
- [Examples](#examples)

---

## Lifecycle Overview

JavaSkript scripts go through several stages:

1. **Loading**: Script is compiled and instantiated
2. **Initialization**: Constructor runs, APIs are injected
3. **Enable**: `onEnable()` is called (if present)
4. **Running**: Script handles events and commands
5. **Disable**: `onDisable()` is called (if present)
6. **Unloading**: Resources are cleaned up

```
┌─────────────┐
│   Loading   │ ──> Compile .java file
└──────┬──────┘
       │
       v
┌─────────────┐
│ Constructor │ ──> Create instance (DON'T use APIs here!)
└──────┬──────┘
       │
       v
┌─────────────┐
│ API Inject  │ ──> scheduler, config, database, placeholders
└──────┬──────┘
       │
       v
┌─────────────┐
│  onEnable() │ ──> Initialize your resources (OPTIONAL)
└──────┬──────┘
       │
       v
┌─────────────┐
│   Running   │ ──> Handle events, commands, etc.
└──────┬──────┘
       │
       v
┌─────────────┐
│ onDisable() │ ──> Clean up resources (OPTIONAL)
└──────┬──────┘
       │
       v
┌─────────────┐
│  Unloading  │ ──> Automatic cleanup
└─────────────┘
```

---

## Lifecycle Methods

### Constructor

**When it runs**: Immediately after class is instantiated  
**Purpose**: Basic initialization only  
**Important**: APIs are NOT available yet!

```java
public MyScript() {
    // ✓ GOOD: Initialize simple fields
    this.playerData = new HashMap<>();
    this.enabled = false;
    
    // ✗ BAD: Don't use APIs (they're null!)
    // scheduler.runLater(() -> {}, 20L); // NullPointerException!
    
    // ✗ BAD: Don't do heavy work
    // loadAllDataFromDatabase(); // Too early!
}
```

### onEnable() (Optional)

**When it runs**: After APIs are injected, before events are registered  
**Purpose**: Initialize resources, start tasks, load data  
**Important**: This is where you should set up your script

```java
public void onEnable() {
    // ✓ GOOD: Now APIs are available
    scheduler.everyMinute(() -> {
        Bukkit.broadcast(Component.text("Tick!"));
    });
    
    // ✓ GOOD: Load configuration
    boolean enabled = config.getBoolean("config.yml", "enabled", true);
    
    // ✓ GOOD: Initialize database
    database.createTable("players", 
        "uuid TEXT PRIMARY KEY",
        "name TEXT NOT NULL"
    );
    
    // ✓ GOOD: Start background tasks
    loadDataAsync();
    
    Bukkit.getLogger().info("[MyScript] Enabled!");
}
```

### onDisable() (Optional)

**When it runs**: Before script is unloaded  
**Purpose**: Clean up resources, save data, close connections  
**Important**: This is your last chance to clean up!

```java
public void onDisable() {
    // ✓ GOOD: Save important data
    saveAllPlayerData();
    
    // ✓ GOOD: Close connections
    if (hikariDataSource != null) {
        hikariDataSource.close();
    }
    
    // ✓ GOOD: Clear caches
    playerCache.clear();
    
    // ✓ GOOD: Cancel custom tasks
    if (customTask != null) {
        customTask.cancel();
    }
    
    Bukkit.getLogger().info("[MyScript] Disabled!");
}
```

**Note**: You don't need to manually:
- Unregister event listeners (automatic)
- Unregister commands (automatic)
- Cancel scheduler tasks (automatic)
- Close database connections (automatic)
- Unregister placeholders (automatic)

---

## Best Practices

### 1. Use onEnable() for Initialization

**Bad**:
```java
public class MyScript implements Listener {
    private Map<UUID, PlayerData> players = new HashMap<>();
    
    public MyScript() {
        // Loading data in constructor - too early!
        loadAllPlayers();
        
        // Starting tasks in constructor - APIs not ready!
        scheduler.everyMinute(() -> saveData()); // NullPointerException!
    }
}
```

**Good**:
```java
public class MyScript implements Listener {
    private Map<UUID, PlayerData> players = new HashMap<>();
    
    public MyScript() {
        // Just initialize the map
        this.players = new HashMap<>();
    }
    
    public void onEnable() {
        // Now load data - APIs are ready
        loadAllPlayers();
        
        // Start tasks - scheduler is available
        scheduler.everyMinute(() -> saveData());
    }
}
```

### 2. Use onDisable() for Cleanup

**Bad**:
```java
public class DatabaseScript implements Listener {
    private HikariDataSource dataSource;
    
    // No cleanup - connection stays open!
}
```

**Good**:
```java
public class DatabaseScript implements Listener {
    private HikariDataSource dataSource;
    
    public void onDisable() {
        // Properly close connection
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Bukkit.getLogger().info("Database connection closed");
        }
    }
}
```

### 3. Handle Reload Gracefully

Scripts can be reloaded with `/js reload <script>`. Make sure your script handles this:

```java
@FoliaSupport
public class ReloadableScript implements Listener {
    private static Map<UUID, Integer> points = new HashMap<>();
    private boolean initialized = false;
    
    public void onEnable() {
        // Prevent double initialization on reload
        if (initialized) {
            Bukkit.getLogger().warning("Script already initialized!");
            return;
        }
        
        // Load saved data
        loadPoints();
        initialized = true;
    }
    
    public void onDisable() {
        // Save data before unload
        savePoints();
        initialized = false;
    }
}
```

### 4. Use Proper Resource Management

```java
@FoliaSupport
public class ResourceScript implements Listener {
    private HikariDataSource dataSource;
    private ExecutorService executor;
    private Map<UUID, PlayerData> cache;
    
    public void onEnable() {
        // Initialize resources
        this.dataSource = createDataSource();
        this.executor = Executors.newFixedThreadPool(4);
        this.cache = new ConcurrentHashMap<>();
        
        Bukkit.getLogger().info("Resources initialized");
    }
    
    public void onDisable() {
        // Clean up in reverse order
        
        // 1. Save cached data
        if (cache != null) {
            cache.values().forEach(this::savePlayerData);
            cache.clear();
        }
        
        // 2. Shutdown executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
        
        // 3. Close database
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        
        Bukkit.getLogger().info("Resources cleaned up");
    }
}
```

---

## Resource Management

### Automatic Cleanup

JavaSkript automatically cleans up:

| Resource | Automatic Cleanup | Manual Cleanup Needed |
|----------|-------------------|----------------------|
| Event Listeners | ✓ Yes | No |
| Commands | ✓ Yes | No |
| Scheduler Tasks | ✓ Yes | No |
| Database (DatabaseHelper) | ✓ Yes | No |
| Placeholders | ✓ Yes | No |
| Config Files | ✓ Yes | No |
| Custom Threads | ✗ No | **Yes** |
| Custom Executors | ✗ No | **Yes** |
| File Handles | ✗ No | **Yes** |
| Network Connections | ✗ No | **Yes** |
| Custom Database Pools | ✗ No | **Yes** |

### Manual Cleanup Required

If you create these resources, you MUST clean them up in `onDisable()`:

```java
public class CustomResourceScript implements Listener {
    // Custom thread pool
    private ExecutorService executor;
    
    // Custom database pool
    private HikariDataSource customDataSource;
    
    // File handles
    private BufferedWriter logWriter;
    
    // Network connections
    private Socket connection;
    
    public void onEnable() {
        executor = Executors.newFixedThreadPool(4);
        customDataSource = new HikariDataSource(config);
        logWriter = new BufferedWriter(new FileWriter("log.txt"));
        connection = new Socket("example.com", 8080);
    }
    
    public void onDisable() {
        // Clean up executor
        if (executor != null) {
            executor.shutdown();
        }
        
        // Close database pool
        if (customDataSource != null) {
            customDataSource.close();
        }
        
        // Close file writer
        if (logWriter != null) {
            try {
                logWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // Close socket
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

---

## Common Patterns

### Pattern 1: Lazy Initialization

```java
@FoliaSupport
public class LazyScript implements Listener {
    private Map<UUID, PlayerData> cache;
    
    private Map<UUID, PlayerData> getCache() {
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
        }
        return cache;
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        getCache().put(event.getPlayer().getUniqueId(), new PlayerData());
    }
}
```

### Pattern 2: Singleton Pattern

```java
@FoliaSupport
public class SingletonScript implements Listener {
    private static SingletonScript instance;
    
    public SingletonScript() {
        instance = this;
    }
    
    public static SingletonScript getInstance() {
        return instance;
    }
    
    public void onDisable() {
        instance = null;
    }
}
```

### Pattern 3: State Management

```java
@FoliaSupport
public class StatefulScript implements Listener {
    private enum State {
        DISABLED, LOADING, READY, ERROR
    }
    
    private State state = State.DISABLED;
    
    public void onEnable() {
        state = State.LOADING;
        
        try {
            loadResources();
            state = State.READY;
        } catch (Exception e) {
            state = State.ERROR;
            Bukkit.getLogger().severe("Failed to load: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (state != State.READY) {
            event.getPlayer().sendMessage("Script is not ready!");
            return;
        }
        
        // Handle event
    }
    
    public void onDisable() {
        state = State.DISABLED;
    }
}
```

---

## Examples

### Example 1: Simple Script

```java
import dev.mukulx.javaskript.script.FoliaSupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@FoliaSupport
public class SimpleScript implements Listener {
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("Welcome!");
    }
}
```

No lifecycle methods needed - this script has no resources to manage.

### Example 2: Script with Resources

```java
import dev.mukulx.javaskript.script.FoliaSupport;
import dev.mukulx.javaskript.api.ScriptScheduler;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import java.util.concurrent.atomic.AtomicInteger;

@FoliaSupport
public class ResourceScript implements Listener {
    private ScriptScheduler scheduler;
    private AtomicInteger counter = new AtomicInteger(0);
    
    public void onEnable() {
        // Start a repeating task
        scheduler.everySecond(() -> {
            int count = counter.incrementAndGet();
            Bukkit.getLogger().info("Counter: " + count);
        });
        
        Bukkit.getLogger().info("ResourceScript enabled!");
    }
    
    public void onDisable() {
        // Save the counter value
        Bukkit.getLogger().info("Final counter value: " + counter.get());
        
        // Note: scheduler.cancelAll() is called automatically
        Bukkit.getLogger().info("ResourceScript disabled!");
    }
}
```

### Example 3: Database Script

```java
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.FoliaSupport;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import java.io.File;
import java.sql.Connection;

// @dependency com.zaxxer:HikariCP:5.1.0
// @dependency org.xerial:sqlite-jdbc:3.53.1.0

@FoliaSupport
public class DatabaseScript implements Listener {
    private HikariDataSource dataSource;
    
    public void onEnable() {
        // Initialize database
        try {
            File dataFolder = new File(
                JavaSkriptPlugin.getInstance().getDataFolder(),
                "script-data/DatabaseScript"
            );
            dataFolder.mkdirs();
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dataFolder + "/data.db");
            config.setMaximumPoolSize(10);
            
            dataSource = new HikariDataSource(config);
            
            // Create table
            try (Connection conn = dataSource.getConnection()) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, name TEXT)"
                );
            }
            
            Bukkit.getLogger().info("Database initialized!");
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (dataSource == null) return;
        
        // Save player to database
        try (Connection conn = dataSource.getConnection()) {
            var stmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, name) VALUES (?, ?)"
            );
            stmt.setString(1, event.getPlayer().getUniqueId().toString());
            stmt.setString(2, event.getPlayer().getName());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void onDisable() {
        // Close database connection pool
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Bukkit.getLogger().info("Database connection closed!");
        }
    }
}
```

---

## Troubleshooting

### Script doesn't reload properly

**Problem**: State persists between reloads  
**Solution**: Clear state in `onDisable()` and reinitialize in `onEnable()`

```java
public void onDisable() {
    // Clear all state
    playerData.clear();
    cache.clear();
    initialized = false;
}

public void onEnable() {
    // Reinitialize
    loadData();
    initialized = true;
}
```

### NullPointerException on APIs

**Problem**: Using APIs in constructor  
**Solution**: Move API usage to `onEnable()` or event handlers

```java
// BAD
public MyScript() {
    scheduler.runLater(() -> {}, 20L); // NPE!
}

// GOOD
public void onEnable() {
    scheduler.runLater(() -> {}, 20L); // Works!
}
```

### Resources not cleaned up

**Problem**: Memory leaks, open connections  
**Solution**: Implement `onDisable()` and clean up manually

```java
public void onDisable() {
    if (customResource != null) {
        customResource.close();
    }
}
```

---

## See Also

- [Quick Start Guide](QUICKSTART.md)
- [API Documentation](API.md)
- [Examples](../src/main/resources/examples/)
- [Folia Compatibility](FOLIA.md)
