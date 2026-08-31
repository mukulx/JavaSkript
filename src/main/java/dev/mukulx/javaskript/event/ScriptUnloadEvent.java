package dev.mukulx.javaskript.event;

import dev.mukulx.javaskript.script.ScriptInstance;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired immediately before a script is torn down, unloaded, or disabled. External plugins and
 * addons can clean up their custom hooks here.
 */
public class ScriptUnloadEvent extends ScriptEvent {

  private static final HandlerList HANDLERS = new HandlerList();
  private final ScriptInstance scriptInstance;

  public ScriptUnloadEvent(String scriptKey, ScriptInstance scriptInstance) {
    super(scriptKey);
    this.scriptInstance = scriptInstance;
  }

  public ScriptInstance getScriptInstance() {
    return scriptInstance;
  }

  public Object getInstance() {
    return scriptInstance != null ? scriptInstance.getInstance() : null;
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
