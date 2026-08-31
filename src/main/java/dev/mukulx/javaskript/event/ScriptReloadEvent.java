package dev.mukulx.javaskript.event;

import dev.mukulx.javaskript.script.ScriptInstance;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a script has been reloaded (after the old instance is unloaded and the new instance
 * has been initialized).
 */
public class ScriptReloadEvent extends ScriptEvent {

  private static final HandlerList HANDLERS = new HandlerList();
  private final ScriptInstance newInstance;

  public ScriptReloadEvent(String scriptKey, ScriptInstance newInstance) {
    super(scriptKey);
    this.newInstance = newInstance;
  }

  public ScriptInstance getScriptInstance() {
    return newInstance;
  }

  public Object getInstance() {
    return newInstance != null ? newInstance.getInstance() : null;
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
