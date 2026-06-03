package dev.mukulx.javaskript.api.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/** Manages all GUIs for scripts */
public class GUIManager implements Listener {

  private static final Map<UUID, GUI> openGUIs = new HashMap<>();

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
}
