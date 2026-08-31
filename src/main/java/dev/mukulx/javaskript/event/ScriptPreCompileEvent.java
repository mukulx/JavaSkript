package dev.mukulx.javaskript.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired immediately before a script's source code is passed into the Eclipse ECJ compiler. Addons
 * and plugins can inspect, preprocess, or modify the source code, or cancel compilation.
 */
public class ScriptPreCompileEvent extends ScriptEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private String sourceCode;
  private boolean cancelled = false;

  public ScriptPreCompileEvent(String scriptKey, String sourceCode) {
    super(scriptKey);
    this.sourceCode = sourceCode;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  public void setSourceCode(String sourceCode) {
    if (sourceCode != null) {
      this.sourceCode = sourceCode;
    }
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
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
