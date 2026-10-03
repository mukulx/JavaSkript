package dev.mukulx.javaskript.api.mannequin;

import org.bukkit.entity.Player;

/**
 * Functional callback invoked when a player attacks (left-clicks) a custom mannequin.
 */
@FunctionalInterface
public interface MannequinAttackCallback {

  /**
   * Called when a player attacks a mannequin.
   *
   * @param player The attacking player
   * @param mannequin The custom mannequin target
   */
  void onAttack(Player player, CustomMannequin mannequin);
}
