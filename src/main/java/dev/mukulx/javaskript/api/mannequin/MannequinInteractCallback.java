package dev.mukulx.javaskript.api.mannequin;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Functional callback invoked when a player interacts with a custom mannequin with a specific hand.
 */
@FunctionalInterface
public interface MannequinInteractCallback {

  /**
   * Called when a player interacts with a mannequin.
   *
   * @param player The interacting player
   * @param mannequin The custom mannequin target
   * @param hand The hand used (HAND or OFF_HAND)
   */
  void onInteract(Player player, CustomMannequin mannequin, EquipmentSlot hand);
}
