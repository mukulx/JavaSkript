# JavaSkript Examples

This document contains examples for various use cases.

**Ready-to-use example scripts** are available in the [src/main/resources/examples](../src/main/resources/examples/) folder. Copy any example to `plugins/JavaSkript/scripts/` on your server to use it.

## Table of Contents

1. [Basic Event Listeners](#basic-event-listeners)
2. [Custom Commands](#custom-commands)
3. [Anti-Cheat & Protection](#anti-cheat--protection)
4. [Player Management](#player-management)
5. [World Management](#world-management)
6. [Economy & Rewards](#economy--rewards)
7. [Advanced Techniques](#advanced-techniques)

---

## Basic Event Listeners

### Welcome & Goodbye Messages

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class WelcomeMessages implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        
        // Custom join message
        event.joinMessage(
            Component.text("✦ ", NamedTextColor.GOLD)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" has joined the server!", NamedTextColor.GOLD))
        );
        
        // Send welcome message to player
        player.sendMessage(Component.empty());
        player.sendMessage(
            Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)
        );
        player.sendMessage(
            Component.text("  Welcome to the Server!", NamedTextColor.GOLD, TextDecoration.BOLD)
        );
        player.sendMessage(
            Component.text("  Online Players: " + event.getPlayer().getServer().getOnlinePlayers().size(), 
                NamedTextColor.YELLOW)
        );
        player.sendMessage(
            Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)
        );
        player.sendMessage(Component.empty());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        
        event.quitMessage(
            Component.text("✦ ", NamedTextColor.RED)
                .append(Component.text(player.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" has left the server!", NamedTextColor.RED))
        );
    }
}
```

### First Join Detection

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class FirstJoinScript implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        
        // Check if first join
        if (!player.hasPlayedBefore()) {
            // Broadcast to all players
            Bukkit.broadcast(
                Component.text("Welcome ", NamedTextColor.GOLD)
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" to the server for the first time!", NamedTextColor.GOLD))
            );
            
            // Give starter items
            player.getInventory().addItem(
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE_SWORD),
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.BREAD, 16)
            );
            
            // Teleport to spawn
            var spawn = player.getWorld().getSpawnLocation();
            player.teleport(spawn);
        }
    }
}
```

---

## Custom Commands

### Heal Command

```java
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import java.util.List;
import java.util.ArrayList;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class HealCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Heal another player
        if (args.length > 0) {
            if (!sender.hasPermission("heal.others")) {
                sender.sendMessage(Component.text("No permission!").color(NamedTextColor.RED));
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return true;
            }
            
            healPlayer(target);
            sender.sendMessage(
                Component.text("Healed " + target.getName() + "!").color(NamedTextColor.GREEN)
            );
            target.sendMessage(
                Component.text("You have been healed by " + sender.getName() + "!")
                    .color(NamedTextColor.GREEN)
            );
            return true;
        }
        
        // Heal self
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this!").color(NamedTextColor.RED));
            return true;
        }
        
        healPlayer(player);
        player.sendMessage(Component.text("You have been healed!").color(NamedTextColor.GREEN));
        return true;
    }
    
    private void healPlayer(Player player) {
        if (player == null) return;
        
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("heal.others")) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return players;
        }
        return List.of();
    }
}
```

### Teleport Command

```java
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.List;
import java.util.ArrayList;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class TeleportCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this!").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /tp <player>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }
        
        if (target.equals(player)) {
            player.sendMessage(Component.text("You cannot teleport to yourself!").color(NamedTextColor.RED));
            return true;
        }
        
        player.teleport(target.getLocation());
        player.sendMessage(
            Component.text("Teleported to " + target.getName() + "!").color(NamedTextColor.GREEN)
        );
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (!p.equals(sender)) {
                    players.add(p.getName());
                }
            });
            return players;
        }
        return List.of();
    }
}
```

---

## Anti-Cheat & Protection

### Anti-Dupe Protection

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class AntiDupeScript implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event == null || event.getWhoClicked() == null) return;
        
        ItemStack item = event.getCurrentItem();
        checkItem(event, item);
        
        ItemStack cursor = event.getCursor();
        checkItem(event, cursor);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event == null || event.getWhoClicked() == null) return;
        
        for (ItemStack item : event.getNewItems().values()) {
            if (checkItem(event, item)) {
                break;
            }
        }
    }
    
    private boolean checkItem(InventoryEvent event, ItemStack item) {
        if (item == null) return false;
        
        // Check for impossible stack sizes
        if (item.getAmount() > item.getMaxStackSize()) {
            event.setCancelled(true);
            event.getView().close();
            
            event.getView().getPlayer().sendMessage(
                Component.text("⚠ Suspicious item detected and removed!")
                    .color(NamedTextColor.RED)
            );
            
            System.out.println("[AntiDupe] Blocked suspicious item for " + 
                event.getView().getPlayer().getName() + 
                " - Amount: " + item.getAmount() + 
                " - Max: " + item.getMaxStackSize());
            
            return true;
        }
        
        return false;
    }
}
```

### Anti-Spam Chat Protection

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class AntiSpamScript implements Listener {

    private final Map<UUID, Long> lastMessage = new HashMap<>();
    private final Map<UUID, String> previousMessage = new HashMap<>();
    private static final long COOLDOWN_MS = 1000; // 1 second

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        var uuid = player.getUniqueId();
        var message = event.getMessage();
        
        // Bypass for ops
        if (player.isOp()) return;
        
        long now = System.currentTimeMillis();
        
        // Check cooldown
        if (lastMessage.containsKey(uuid)) {
            long timeSince = now - lastMessage.get(uuid);
            if (timeSince < COOLDOWN_MS) {
                event.setCancelled(true);
                player.sendMessage(
                    Component.text("⚠ Please wait before sending another message!")
                        .color(NamedTextColor.RED)
                );
                return;
            }
        }
        
        // Check duplicate message
        if (previousMessage.containsKey(uuid)) {
            if (message.equals(previousMessage.get(uuid))) {
                event.setCancelled(true);
                player.sendMessage(
                    Component.text("⚠ Please don't send the same message twice!")
                        .color(NamedTextColor.RED)
                );
                return;
            }
        }
        
        lastMessage.put(uuid, now);
        previousMessage.put(uuid, message);
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var uuid = event.getPlayer().getUniqueId();
        lastMessage.remove(uuid);
        previousMessage.remove(uuid);
    }
}
```

---

## Player Management

### AFK Detection

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.*;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.mukulx.javaskript.script.PaperOnly;

@PaperOnly
public class AFKDetector implements Listener {

    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final long AFK_TIME_MS = 300000; // 5 minutes

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Start AFK checker (only once)
        if (lastActivity.size() == 1) {
            startAFKChecker();
        }
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) return;
        lastActivity.remove(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        // Only update if actually moved (not just head rotation)
        if (event.getFrom().distance(event.getTo()) > 0) {
            lastActivity.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event == null || event.getPlayer() == null) return;
        lastActivity.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }
    
    private void startAFKChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player == null) return;
                    
                    Long last = lastActivity.get(player.getUniqueId());
                    if (last != null && (now - last) >= AFK_TIME_MS) {
                        Bukkit.broadcast(
                            Component.text(player.getName() + " is now AFK")
                                .color(NamedTextColor.GRAY)
                        );
                        
                        // Reset to avoid spam
                        lastActivity.put(player.getUniqueId(), now);
                    }
                });
            }
        }.runTaskTimer(
            Bukkit.getPluginManager().getPlugin("JavaSkript"), 
            6000L, // Start after 5 minutes
            6000L  // Check every 5 minutes
        );
    }
}
```

---

## Advanced Techniques

### Custom Event System

```java
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.Player;

// Define custom event
public class PlayerLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    
    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }
    
    public Player getPlayer() { return player; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }
    
    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    
    public static HandlerList getHandlerList() { return HANDLERS; }
}
```

### Persistent Data Storage

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.Listener;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;
import dev.mukulx.javaskript.script.FoliaSupport;

@FoliaSupport
public class PlayerDataScript implements Listener {

    private static final NamespacedKey COINS_KEY = 
        new NamespacedKey(Bukkit.getPluginManager().getPlugin("JavaSkript"), "coins");

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        var pdc = player.getPersistentDataContainer();
        
        // Load coins or set default
        int coins = pdc.getOrDefault(COINS_KEY, PersistentDataType.INTEGER, 0);
        
        player.sendMessage("You have " + coins + " coins!");
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) return;
        
        var player = event.getPlayer();
        var pdc = player.getPersistentDataContainer();
        
        // Save coins (example: add 10 per session)
        int coins = pdc.getOrDefault(COINS_KEY, PersistentDataType.INTEGER, 0);
        pdc.set(COINS_KEY, PersistentDataType.INTEGER, coins + 10);
    }
}
```

---

For more ready-to-use examples, check the [src/main/resources/examples](../src/main/resources/examples/) folder:

- **WelcomeScript.java** - Simple welcome message
- **HealCommand.java** - Basic heal command
- **FlyCommand.java** - Flight toggle with permissions
- **ConfigExample.java** - Multiple config files
- **DatabaseExample.java** - SQLite database usage
- **SchedulerExample.java** - Task scheduling
- **GUIExample.java** - Interactive GUIs
- **PlaceholderExample.java** - Custom placeholders
- **PermissionExample.java** - Dynamic permissions
- **MultiClassExample.java** - Multiple classes in one file

Copy any example to `plugins/JavaSkript/scripts/` to use it on your server!
