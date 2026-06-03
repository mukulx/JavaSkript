// ExtraInventory - Advanced example showing database usage with HikariCP
//
// This script demonstrates:
// - Custom database connection pool (HikariCP)
// - Proper lifecycle management with onDisable()
// - Async inventory loading
// - Folia-compatible entity scheduler usage
// - Resource cleanup on unload
//
// Lifecycle:
// 1. Constructor: Initialize maps
// 2. Constructor: Initialize database (creates tables)
// 3. onDisable(): Save open inventories, close database pool
//
// @dependency com.zaxxer:HikariCP:5.1.0
// @dependency org.xerial:sqlite-jdbc:3.53.1.0

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

@FoliaSupport
public class ExtraInventory implements Listener, CommandExecutor {

  private HikariDataSource dataSource;

  // Track open inventories to prevent duplication
  private final Map<UUID, Boolean> openInventories = new ConcurrentHashMap<>();

  // Track if player is currently loading inventory (prevent race conditions)
  private final Map<UUID, Boolean> loadingInventories = new ConcurrentHashMap<>();

  // Cache for loaded inventories (cleared on save)
  private final Map<UUID, ItemStack[]> inventoryCache = new ConcurrentHashMap<>();

  public ExtraInventory() {
    // Initialize database
    initDatabase();

    Bukkit.getLogger()
        .info("[ExtraInventory] Enabled! Use /extrainv to access your extra storage.");
  }

  public void onDisable() {
    // Save all open inventories before shutdown
    for (UUID uuid : openInventories.keySet()) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null && player.isOnline()) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv.getHolder() == null) { // Custom inventory
          saveInventoryAsync(uuid, inv.getContents());
        }
      }
    }

    // Close database connection pool
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      Bukkit.getLogger().info("[ExtraInventory] Database connection pool closed.");
    }

    Bukkit.getLogger().info("[ExtraInventory] Disabled!");
  }

  private void initDatabase() {
    try {
      // Create data folder in script-data directory
      File scriptDataFolder =
          new File(
              dev.mukulx.javaskript.JavaSkriptPlugin.getInstance().getDataFolder(), "script-data");
      if (!scriptDataFolder.exists()) {
        scriptDataFolder.mkdirs();
      }

      File dataFolder = new File(scriptDataFolder, "ExtraInventory");
      if (!dataFolder.exists()) {
        dataFolder.mkdirs();
      }

      // Configure HikariCP
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl("jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/inventories.db");
      config.setMaximumPoolSize(10);
      config.setMinimumIdle(2);
      config.setConnectionTimeout(30000);
      config.setIdleTimeout(600000);
      config.setMaxLifetime(1800000);
      config.setConnectionTestQuery("SELECT 1");

      // SQLite specific settings
      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

      dataSource = new HikariDataSource(config);

      // Create table
      try (Connection conn = dataSource.getConnection()) {
        String createTable =
            """
                    CREATE TABLE IF NOT EXISTS extra_inventories (
                        uuid TEXT PRIMARY KEY,
                        inventory BLOB NOT NULL,
                        last_modified INTEGER NOT NULL
                    )
                    """;
        conn.createStatement().execute(createTable);

        // Create index for faster lookups
        String createIndex =
            "CREATE INDEX IF NOT EXISTS idx_last_modified ON extra_inventories(last_modified)";
        conn.createStatement().execute(createIndex);
      }

      Bukkit.getLogger().info("[ExtraInventory] Database initialized successfully!");

    } catch (Exception e) {
      Bukkit.getLogger()
          .severe("[ExtraInventory] Failed to initialize database: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          Component.text("Only players can use this command!").color(NamedTextColor.RED));
      return true;
    }

    if (!player.hasPermission("extrainv.use")) {
      player.sendMessage(
          Component.text("You don't have permission to use this command!")
              .color(NamedTextColor.RED));
      return true;
    }

    // Check if player already has inventory open
    if (openInventories.containsKey(player.getUniqueId())) {
      player.sendMessage(
          Component.text("You already have your extra inventory open!").color(NamedTextColor.RED));
      return true;
    }

    // Check if inventory is currently being loaded
    if (loadingInventories.getOrDefault(player.getUniqueId(), false)) {
      player.sendMessage(
          Component.text("Please wait, loading your inventory...").color(NamedTextColor.YELLOW));
      return true;
    }

    // Open inventory
    openExtraInventory(player);
    return true;
  }

  private void openExtraInventory(Player player) {
    UUID uuid = player.getUniqueId();

    // Mark as loading
    loadingInventories.put(uuid, true);

    player.sendMessage(Component.text("Loading your inventory...").color(NamedTextColor.YELLOW));

    // Load inventory asynchronously using CompletableFuture
    java.util.concurrent.CompletableFuture.supplyAsync(
            () -> {
              try {
                return loadInventory(uuid);
              } catch (Exception e) {
                Bukkit.getLogger()
                    .severe("[ExtraInventory] Error loading inventory: " + e.getMessage());
                e.printStackTrace();
                return null;
              }
            })
        .thenAcceptAsync(
            contents -> {
              try {
                // Double-check player is still online
                if (!player.isOnline()) {
                  loadingInventories.remove(uuid);
                  return;
                }

                // Create inventory
                Inventory inv =
                    Bukkit.createInventory(
                        null, 54, Component.text("Extra Inventory").color(NamedTextColor.GOLD));

                // Set contents
                if (contents != null) {
                  inv.setContents(contents);
                }

                // Open inventory
                player.openInventory(inv);

                // Mark as open
                openInventories.put(uuid, true);
                loadingInventories.remove(uuid);

                player.sendMessage(
                    Component.text("Opened your extra inventory!").color(NamedTextColor.GREEN));
              } catch (Exception e) {
                Bukkit.getLogger()
                    .severe("[ExtraInventory] Error opening inventory: " + e.getMessage());
                e.printStackTrace();
                loadingInventories.remove(uuid);
                player.sendMessage(
                    Component.text("Failed to open inventory!").color(NamedTextColor.RED));
              }
            },
            runnable ->
                player
                    .getScheduler()
                    .execute(
                        dev.mukulx.javaskript.JavaSkriptPlugin.getInstance(), runnable, null, 1L))
        .exceptionally(
            throwable -> {
              Bukkit.getLogger().severe("[ExtraInventory] Async error: " + throwable.getMessage());
              throwable.printStackTrace();
              loadingInventories.remove(uuid);
              player.sendMessage(
                  Component.text("Failed to load inventory!").color(NamedTextColor.RED));
              return null;
            });
  }

  private ItemStack[] loadInventory(UUID uuid) {
    // Check cache first
    if (inventoryCache.containsKey(uuid)) {
      return inventoryCache.get(uuid);
    }

    // Check if datasource is closed (can happen during reload)
    if (dataSource == null || dataSource.isClosed()) {
      Bukkit.getLogger().warning("[ExtraInventory] DataSource is closed, cannot load inventory");
      return null;
    }

    try (Connection conn = dataSource.getConnection()) {
      String query = "SELECT inventory FROM extra_inventories WHERE uuid = ?";
      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, uuid.toString());

        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            byte[] data = rs.getBytes("inventory");
            ItemStack[] contents = deserializeInventory(data);

            // Cache the loaded inventory
            inventoryCache.put(uuid, contents);

            return contents;
          }
        }
      }
    } catch (Exception e) {
      Bukkit.getLogger()
          .severe("[ExtraInventory] Failed to load inventory for " + uuid + ": " + e.getMessage());
      e.printStackTrace();
    }

    return null;
  }

  private void saveInventoryAsync(UUID uuid, ItemStack[] contents) {
    // Update cache
    inventoryCache.put(uuid, contents);

    // Check if datasource is closed (can happen during reload)
    if (dataSource == null || dataSource.isClosed()) {
      Bukkit.getLogger().warning("[ExtraInventory] DataSource is closed, cannot save inventory");
      return;
    }

    // Save to database asynchronously using CompletableFuture
    java.util.concurrent.CompletableFuture.runAsync(
        () -> {
          try (Connection conn = dataSource.getConnection()) {
            String query =
                """
                        INSERT INTO extra_inventories (uuid, inventory, last_modified)
                        VALUES (?, ?, ?)
                        ON CONFLICT(uuid) DO UPDATE SET
                            inventory = excluded.inventory,
                            last_modified = excluded.last_modified
                        """;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
              stmt.setString(1, uuid.toString());
              stmt.setBytes(2, serializeInventory(contents));
              stmt.setLong(3, System.currentTimeMillis());
              stmt.executeUpdate();
            }

          } catch (Exception e) {
            Bukkit.getLogger()
                .severe(
                    "[ExtraInventory] Failed to save inventory for "
                        + uuid
                        + ": "
                        + e.getMessage());
            e.printStackTrace();
          }
        });
  }

  private byte[] serializeInventory(ItemStack[] items) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

    // Write the size of the inventory
    dataOutput.writeInt(items.length);

    // Write every item
    for (ItemStack item : items) {
      dataOutput.writeObject(item);
    }

    dataOutput.close();
    return outputStream.toByteArray();
  }

  private ItemStack[] deserializeInventory(byte[] data) throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
    BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

    // Read the size
    int size = dataInput.readInt();
    ItemStack[] items = new ItemStack[size];

    // Read every item
    for (int i = 0; i < size; i++) {
      items[i] = (ItemStack) dataInput.readObject();
    }

    dataInput.close();
    return items;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player)) {
      return;
    }

    UUID uuid = player.getUniqueId();

    // Check if this is an extra inventory
    if (!openInventories.containsKey(uuid)) {
      return;
    }

    Inventory inv = event.getInventory();

    // Verify it's a custom inventory (not a player/chest/etc)
    if (inv.getHolder() != null) {
      return;
    }

    // Save inventory
    saveInventoryAsync(uuid, inv.getContents());

    // Remove from open inventories
    openInventories.remove(uuid);

    player.sendMessage(Component.text("Extra inventory saved!").color(NamedTextColor.GREEN));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    UUID uuid = player.getUniqueId();

    // Only track clicks in extra inventories
    if (!openInventories.containsKey(uuid)) {
      return;
    }

    // Allow all normal interactions
    // The inventory will be saved on close
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    UUID uuid = player.getUniqueId();

    // Only track drags in extra inventories
    if (!openInventories.containsKey(uuid)) {
      return;
    }

    // Allow all normal interactions
    // The inventory will be saved on close
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    // If player has extra inventory open, save it
    if (openInventories.containsKey(uuid)) {
      Inventory inv = player.getOpenInventory().getTopInventory();
      if (inv.getHolder() == null) {
        saveInventoryAsync(uuid, inv.getContents());
      }
      openInventories.remove(uuid);
    }

    // Clear loading state
    loadingInventories.remove(uuid);

    // Clear cache after a delay to ensure save completes (using CompletableFuture)
    java.util.concurrent.CompletableFuture.delayedExecutor(2, java.util.concurrent.TimeUnit.SECONDS)
        .execute(
            () -> {
              inventoryCache.remove(uuid);
            });
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    // Clear any stale states (shouldn't happen, but safety first)
    openInventories.remove(uuid);
    loadingInventories.remove(uuid);

    // Pre-load inventory into cache for faster access (using CompletableFuture with delay)
    java.util.concurrent.CompletableFuture.delayedExecutor(1, java.util.concurrent.TimeUnit.SECONDS)
        .execute(
            () -> {
              loadInventory(uuid);
            });
  }
}
