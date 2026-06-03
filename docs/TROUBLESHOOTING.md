# Troubleshooting Guide

## Common Issues and Solutions

### "ERROR: Trying to use API in constructor before it's injected!"

**Problem:** You're using `scheduler`, `config`, `database`, or `placeholders` in your script's constructor.

**Why:** APIs are injected AFTER the constructor runs, so they're null in the constructor.

**Solution:** Use APIs in event handlers or methods, not constructors.

**Bad:**
```java
public class MyScript implements Listener {
    private ScriptScheduler scheduler;
    
    public MyScript() {
        scheduler.runLater(() -> {  // ❌ scheduler is null here!
            // code
        }, 100L);
    }
}
```

**Good:**
```java
@FoliaSupport
public class MyScript implements Listener {
    private ScriptScheduler scheduler;
    
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!event.getPlugin().getName().equals("JavaSkript")) return;
        
        scheduler.runLater(() -> {  // ✅ scheduler is injected now!
            // code
        }, 100L);
    }
}
```

---

### "UnsupportedOperationException" on Folia

**Problem:** Using `Bukkit.getScheduler()` on Folia server.

**Why:** Folia doesn't support the old Bukkit scheduler.

**Solution:** Use JavaSkript's `ScriptScheduler` instead (auto-injected and works on both Paper and Folia).

**Bad:**
```java
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    // code
}, 100L);
```

**Good:**
```java
private ScriptScheduler scheduler;  // Auto-injected

scheduler.runLater(() -> {
    // code
}, 100L);
```

---

### Script Not Loading

**Check:**
1. Console for compilation errors
2. Class is `public`
3. Imports are correct
4. No syntax errors
5. If using multiple classes, ensure one is public

**Run:** `/js reload <scriptname>` to see specific errors

---

### Multiple Classes Not Working

**Problem:** Helper classes not accessible or not loading.

**Solution:** 
1. Ensure only ONE class is `public`
2. Other classes should be package-private (no `public` keyword)
3. Public class name must match file name
4. Check console for "Found X class(es)" message

See [Multiple Classes Guide](MULTIPLE_CLASSES.md) for details.

---

### Command Not Working

**Check:**
1. Class name ends with "Command" (e.g., `HealCommand` → `/heal`)
2. Implements `CommandExecutor`
3. Console shows "Dynamically registered command: /yourcommand"

---

### APIs Not Injected

**Check:**
1. Field names are exact: `scheduler`, `config`, `database`, `placeholders`
2. Fields are `private` (not `public` or `static`)
3. Not trying to use them in constructor

---

### Old Script Version Running

**Problem:** Server is using old compiled version of script.

**Solution:**
1. Delete the script file from `plugins/JavaSkript/scripts/`
2. Restart server (regenerates from resources)

OR

3. Run `/js disable <script>` to disable it
4. Edit the script file
5. Run `/js enable <script>` to reload it

---

### Folia Compatibility Warnings

**On Folia:** Scripts without `@FoliaSupport` show warnings.

**Solution:** Add annotation to your script:

```java
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class MyScript implements Listener {
    // Your code
}
```

**If Paper-only:** Mark it:

```java
import dev.mukulx.javaskript.script.PaperOnly;

@PaperOnly
public class MyScript implements Listener {
    // Your code
}
```

---

### Script Disabled Accidentally

**Check:** `/js list` shows disabled scripts in red with ✗

**Solution:** `/js enable <script>`

---

### Hot Reload Not Working

**Check:**
1. File watcher is running (console shows "File watcher started")
2. Saving the file (not just editing)
3. File is in `plugins/JavaSkript/scripts/` folder

**Manual reload:** `/js reload <script>`

---

### Database Errors

**Check:**
1. Not using `database` in constructor
2. Table created before using it
3. SQL syntax is correct

**Example:**
```java
@EventHandler
public void onPluginEnable(PluginEnableEvent event) {
    if (!event.getPlugin().getName().equals("JavaSkript")) return;
    
    database.createTable("players",
        "uuid TEXT PRIMARY KEY",
        "name TEXT"
    );
}
```

---

### Permission Not Working

**Check:**
1. Permission registered in `PluginEnableEvent`
2. Permission name is correct
3. Player has the permission (check with LuckPerms/etc)

---

## Getting Help

1. Check console for error messages
2. Read the error message carefully (they explain the problem!)
3. Check example scripts in `plugins/JavaSkript/scripts/`
4. Read documentation in `docs/` folder
5. Report issues on GitHub with:
   - Full error message
   - Script code
   - Server version (Paper/Folia)
   - JavaSkript version
