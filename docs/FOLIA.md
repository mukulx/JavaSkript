# Folia Compatibility Guide

JavaSkript supports both Paper and Folia servers. However, some scripts may not work correctly on Folia due to API differences.

## What is Folia?

Folia is a Paper fork that implements multi-threaded regions for better performance. However, this changes how some APIs work, and not all Paper APIs are available on Folia.

## Marking Script Compatibility

JavaSkript provides annotations to mark your scripts' compatibility:

### @FoliaSupport

Mark your script as Folia-compatible:

```java
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class MyScript implements Listener {
    // Your Folia-compatible code
}
```

**Benefits:**
- No warnings on Folia servers
- Indicates to users that the script works on Folia
- Shows in `/js info <script>` command

### @PaperOnly

Mark your script as Paper-only (won't work on Folia):

```java
import dev.mukulx.javaskript.script.PaperOnly;

@PaperOnly
public class MyScript implements Listener {
    // Your Paper-only code
}
```

**Benefits:**
- Script won't load on Folia servers
- Clear error message explaining why
- Prevents runtime errors

## Common Folia Incompatibilities

### 1. Bukkit.getScheduler()

**Problem:** Direct scheduler access doesn't work on Folia.

**Paper-only code:**
```java
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    // Code
}, 100L);
```

**Folia-compatible solution:**
```java
// Use JavaSkript's ScriptScheduler (auto-injected)
private ScriptScheduler scheduler;

scheduler.runLater(() -> {
    // Code
}, 100L);
```

### 2. Global World Access

**Problem:** Accessing worlds without entity context doesn't work on Folia.

**Paper-only code:**
```java
World world = Bukkit.getWorld("world");
world.setTime(1000);
```

**Folia-compatible solution:**
```java
// Use entity context
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    World world = event.getPlayer().getWorld();
    world.setTime(1000);
}
```

### 3. Synchronous Chunk Loading

**Problem:** Synchronous chunk operations block on Folia.

**Paper-only code:**
```java
Chunk chunk = world.getChunkAt(x, z);
```

**Folia-compatible solution:**
```java
// Use async chunk loading
world.getChunkAtAsync(x, z).thenAccept(chunk -> {
    // Use chunk here
});
```

### 4. Cross-World Teleportation

**Problem:** Teleporting between worlds requires special handling on Folia.

**Paper-only code:**
```java
player.teleport(otherWorld.getSpawnLocation());
```

**Folia-compatible solution:**
```java
// Use async teleport
player.teleportAsync(otherWorld.getSpawnLocation());
```

## Best Practices for Folia Compatibility

### 1. Use JavaSkript APIs

JavaSkript's built-in APIs are designed to work on both Paper and Folia:

- **ScriptScheduler** - Works on both Paper and Folia
- **ScriptConfig** - Works on both Paper and Folia
- **DatabaseHelper** - Works on both Paper and Folia
- **GUI Builder** - Works on both Paper and Folia

### 2. Use Entity Context

Always work within entity context when possible:

```java
@EventHandler
public void onInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    World world = player.getWorld(); // Use player's world
    Location loc = player.getLocation(); // Use player's location
}
```

### 3. Use Async Methods

Prefer async methods over sync:

```java
// Good - async
player.teleportAsync(location);
world.getChunkAtAsync(x, z);

// Bad - sync (may not work on Folia)
player.teleport(location);
world.getChunkAt(x, z);
```

### 4. Avoid Global State

Don't rely on global server state:

```java
// Bad - global state
Bukkit.getOnlinePlayers().forEach(p -> {
    // This may not work correctly on Folia
});

// Good - event-driven
@EventHandler
public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    // Work with this specific player
}
```

## Testing Your Scripts

### On Paper

1. Start your Paper server
2. Load your script with `/js load <script>`
3. Test all functionality
4. Check console for errors

### On Folia

1. Start your Folia server
2. Load your script with `/js load <script>`
3. Watch for compatibility warnings
4. Test all functionality
5. Check console for errors

## Checking Compatibility

Use `/js info <script>` to see compatibility status:

```
Script Information:
  Name: MyScript.java
  Class: MyScript
  File: /path/to/MyScript.java
  Status: Enabled
  Compatibility: Folia-compatible
  Implements: Listener
```

Possible compatibility statuses:
- **Folia-compatible** - Has @FoliaSupport annotation
- **Paper-only (reason)** - Has @PaperOnly annotation
- **Unknown (not marked)** - No annotation (shows warning on Folia)

## Server Detection

You can detect the server type in your scripts:

```java
import dev.mukulx.javaskript.util.ServerUtil;

if (ServerUtil.isFolia()) {
    // Running on Folia
    player.sendMessage("Running on Folia!");
} else {
    // Running on Paper
    player.sendMessage("Running on Paper!");
}

// Get server type as string
String serverType = ServerUtil.getServerType(); // "Folia" or "Paper"
```

## Migration Guide

### Converting Paper-only Scripts to Folia-compatible

1. **Replace Bukkit.getScheduler()** with ScriptScheduler
2. **Use entity context** for world access
3. **Use async methods** for chunk loading and teleportation
4. **Test on Folia** server
5. **Add @FoliaSupport** annotation

### Example Migration

**Before (Paper-only):**
```java
public class MyScript implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("Welcome!");
        }, 100L);
    }
}
```

**After (Folia-compatible):**
```java
import dev.mukulx.javaskript.script.FoliaSupport;
import dev.mukulx.javaskript.api.ScriptScheduler;

@FoliaSupport
public class MyScript implements Listener {
    private ScriptScheduler scheduler; // Auto-injected
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        scheduler.runLater(() -> {
            player.sendMessage("Welcome!");
        }, 100L);
    }
}
```

## Common Errors on Folia

### "UnsupportedOperationException: Not supported on Folia"

This means you're using a Paper-only API. Check the stack trace and replace with Folia-compatible alternatives.

### "IllegalStateException: Cannot access world without entity context"

You're trying to access a world globally. Use entity context instead.

### "Scheduler access from wrong thread"

You're using Bukkit.getScheduler() on Folia. Use ScriptScheduler instead.

## Resources

- [Folia Documentation](https://docs.papermc.io/folia)
- [Paper API Docs](https://jd.papermc.io/)
- JavaSkript Examples: `plugins/JavaSkript/scripts/`
  - `FoliaCompatibleExample.java` - Folia-compatible script
  - `PaperOnlyExample.java` - Paper-only script

## Need Help?

- Check example scripts in `plugins/JavaSkript/scripts/`
- Read the error messages carefully
- Test on both Paper and Folia
- Report issues on GitHub

---

**Last Updated:** 2026-05-27
