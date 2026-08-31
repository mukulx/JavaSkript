package dev.mukulx.javaskript.event;

import dev.mukulx.javaskript.script.ScriptInstance;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a JavaSkript script is successfully compiled, loaded, and registered. Other plugins
 * can listen to this event to detect script reloads or integrations.
 */
public class ScriptLoadEvent extends ScriptEvent {

  private static final HandlerList HANDLERS = new HandlerList();
  private final ScriptInstance scriptInstance;
  private final Class<?> scriptClass;

  public ScriptLoadEvent(String scriptKey, ScriptInstance scriptInstance, Class<?> scriptClass) {
    super(scriptKey);
    this.scriptInstance = scriptInstance;
    this.scriptClass = scriptClass;
  }

  public ScriptInstance getScriptInstance() {
    return scriptInstance;
  }

  public Class<?> getScriptClass() {
    return scriptClass;
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
