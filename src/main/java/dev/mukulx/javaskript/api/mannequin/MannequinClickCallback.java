package dev.mukulx.javaskript.api.mannequin;

import org.bukkit.entity.Player;

/**
 * Functional callback invoked when a player clicks (right-clicks) a custom mannequin.
 */
@FunctionalInterface
public interface MannequinClickCallback {

  /**
   * Called when a player clicks a mannequin.
   *
   * @param player The clicking player
   * @param mannequin The custom mannequin target
   */
  void onClick(Player player, CustomMannequin mannequin);
}
