package dev.mukulx.javaskript.api.gui;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Easy GUI builder for scripts Supports all inventory types with click handlers */
public class GUI {

  private final Inventory inventory;
  private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers;
  private Consumer<InventoryCloseEvent> closeHandler;
  private boolean cancelAllClicks = true;

  /**
   * Create a chest GUI
   *
   * @param title The GUI title
   * @param rows Number of rows (1-6)
   */
  public GUI(Component title, int rows) {
    if (rows < 1 || rows > 6) {
      throw new IllegalArgumentException("Rows must be between 1 and 6");
    }
    this.inventory =
        Bukkit.createInventory(null, rows * 9, title != null ? title : Component.empty());
    this.clickHandlers = new ConcurrentHashMap<>();
  }

  /**
   * Create a chest GUI with String title
   *
   * @param title The GUI title
   * @param rows Number of rows (1-6)
   */
  public GUI(String title, int rows) {
    this(dev.mukulx.javaskript.util.TextUtil.parse(title), rows);
  }

  /**
   * Set an item in a specific slot
   *
   * @param slot The slot (0-53 for 6 rows)
   * @param item The item to set
   * @return This GUI for chaining
   */
  public GUI setItem(int slot, ItemStack item) {
    if (slot >= 0 && slot < inventory.getSize()) {
      inventory.setItem(slot, item);
    }
    return this;
  }

  /**
   * Set an item with a click handler
   *
   * @param slot The slot
   * @param item The item
   * @param onClick Click handler
   * @return This GUI for chaining
   */
  public GUI setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
    setItem(slot, item);
    clickHandlers.put(slot, onClick);
    return this;
  }

  /**
   * Fill the GUI with an item
   *
   * @param item The item to fill with
   * @return This GUI for chaining
   */
  public GUI fill(ItemStack item) {
    for (int i = 0; i < inventory.getSize(); i++) {
      if (inventory.getItem(i) == null) {
        inventory.setItem(i, item);
      }
    }
    return this;
  }

  /**
   * Fill the border with an item
   *
   * @param item The item to fill with
   * @return This GUI for chaining
   */
  public GUI fillBorder(ItemStack item) {
    int size = inventory.getSize();
    int rows = size / 9;

    // Top and bottom rows
    for (int i = 0; i < 9; i++) {
      inventory.setItem(i, item);
      inventory.setItem(size - 9 + i, item);
    }

    // Left and right columns
    for (int i = 1; i < rows - 1; i++) {
      inventory.setItem(i * 9, item);
      inventory.setItem(i * 9 + 8, item);
    }

    return this;
  }

  /**
   * Fill a row with an item
   *
   * @param row The row (0-5)
   * @param item The item
   * @return This GUI for chaining
   */
  public GUI fillRow(int row, ItemStack item) {
    int start = row * 9;
    for (int i = 0; i < 9; i++) {
      inventory.setItem(start + i, item);
    }
    return this;
  }

  /**
   * Fill a column with an item
   *
   * @param column The column (0-8)
   * @param item The item
   * @return This GUI for chaining
   */
  public GUI fillColumn(int column, ItemStack item) {
    int rows = inventory.getSize() / 9;
    for (int i = 0; i < rows; i++) {
      inventory.setItem(i * 9 + column, item);
    }
    return this;
  }

  /**
   * Set whether to cancel all clicks by default
   *
   * @param cancel true to cancel all clicks
   * @return This GUI for chaining
   */
  public GUI setCancelAllClicks(boolean cancel) {
    this.cancelAllClicks = cancel;
    return this;
  }

  /**
   * Set a close handler
   *
   * @param handler The close handler
   * @return This GUI for chaining
   */
  public GUI onClose(Consumer<InventoryCloseEvent> handler) {
    this.closeHandler = handler;
    return this;
  }

  /**
   * Open the GUI for a player
   *
   * @param player The player
   */
  public void open(Player player) {
    if (player != null) {
      GUIManager.registerGUI(player, this);
      player.openInventory(inventory);
    }
  }

  /**
   * Handle a click event
   *
   * @param event The click event
   */
  public void handleClick(InventoryClickEvent event) {
    // Harden every click path: shift-click, number keys, offhand swap, double-click collect
    // and drags can all move items when only the top slot is guarded.
    if (cancelAllClicks) {
      event.setCancelled(true);
    }
    if (event.getClickedInventory() == null) {
      return;
    }

    boolean isTopInventory = event.getClickedInventory().equals(inventory);
    // Number-key swaps, offhand swaps and collects act on the top inventory even when the
    // clicked slot is in the bottom inventory, so they are already cancelled above.

    if (isTopInventory) {
      int slot = event.getSlot();
      if (slot >= 0 && slot < inventory.getSize()) {
        Consumer<InventoryClickEvent> handler = clickHandlers.get(slot);
        if (handler != null) {
          try {
            handler.accept(event);
          } catch (Throwable t) {
            Bukkit.getLogger()
                .warning(
                    "[GUI] Error executing click handler in slot " + slot + ": " + t.getMessage());
          }
        }
      }
    }
  }

  /**
   * Handle a close event
   *
   * @param event The close event
   */
  public void handleClose(InventoryCloseEvent event) {
    if (closeHandler != null) {
      try {
        closeHandler.accept(event);
      } catch (Throwable t) {
        Bukkit.getLogger().warning("[GUI] Error executing close handler: " + t.getMessage());
      }
    }
  }

  /**
   * Get the inventory
   *
   * @return The inventory
   */
  public Inventory getInventory() {
    return inventory;
  }

  /**
   * Clear the GUI
   *
   * @return This GUI for chaining
   */
  public GUI clear() {
    inventory.clear();
    clickHandlers.clear();
    return this;
  }

  /** Update the GUI for all viewers */
  public void update() {
    inventory
        .getViewers()
        .forEach(
            viewer -> {
              if (viewer instanceof Player player) {
                player.updateInventory();
              }
            });
  }
}
