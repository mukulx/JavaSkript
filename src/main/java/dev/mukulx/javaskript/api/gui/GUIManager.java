package dev.mukulx.javaskript.api.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Manages all GUIs for scripts */
public class GUIManager implements Listener {

  private static final Map<UUID, GUI> openGUIs = new ConcurrentHashMap<>();

  /** Register a GUI for a player */
  public static void registerGUI(Player player, GUI gui) {
    if (player != null && gui != null) {
      openGUIs.put(player.getUniqueId(), gui);
    }
  }

  /** Unregister a GUI for a player */
  public static void unregisterGUI(Player player) {
    if (player != null) {
      openGUIs.remove(player.getUniqueId());
    }
  }

  /** Get the GUI a player has open */
  public static GUI getGUI(Player player) {
    if (player == null) {
      return null;
    }
    return openGUIs.get(player.getUniqueId());
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    GUI gui = openGUIs.get(player.getUniqueId());
    if (gui != null && event.getInventory().equals(gui.getInventory())) {
      gui.handleClick(event);
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    GUI gui = openGUIs.get(player.getUniqueId());
    // Dragging across the menu would bypass click guards and dupe items.
    if (gui != null && event.getInventory().equals(gui.getInventory())) {
      event.setCancelled(true);
    }
  }

  /** Close all open menus and drop the registry (called on disable to avoid ghost menus). */
  public static void closeAll() {
    for (Map.Entry<UUID, GUI> entry : openGUIs.entrySet()) {
      try {
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
        if (player != null && player.isOnline()) {
          player.closeInventory();
        }
      } catch (Throwable ignored) {
        // Keep closing the rest
      }
    }
    openGUIs.clear();
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player)) {
      return;
    }

    GUI gui = openGUIs.get(player.getUniqueId());
    if (gui != null && event.getInventory().equals(gui.getInventory())) {
      gui.handleClose(event);
      unregisterGUI(player);
    }
  }

  @EventHandler
  public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
    unregisterGUI(event.getPlayer());
  }
}
