package dev.mukulx.javaskript.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Base event for all JavaSkript script lifecycle events. */
public abstract class ScriptEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();
  private final String scriptKey;

  public ScriptEvent(String scriptKey) {
    this(scriptKey, false);
  }

  public ScriptEvent(String scriptKey, boolean async) {
    super(async);
    this.scriptKey = scriptKey;
  }

  /**
   * Get the script key or name (e.g. "pvp/CombatLog" or "Welcome").
   *
   * @return The script key
   */
  public String getScriptKey() {
    return scriptKey;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
