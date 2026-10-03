package dev.mukulx.javaskript.api.advancement;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.AdvancementHelper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

/** Event listener that routes Paper advancement completions to active scripts and handlers. */
public class AdvancementListener implements Listener {

  private final JavaSkriptPlugin plugin;

  public AdvancementListener(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
    if (event == null || event.getPlayer() == null || event.getAdvancement() == null) {
      return;
    }
    AdvancementHelper.dispatchDone(event.getPlayer(), event.getAdvancement());
  }
}
