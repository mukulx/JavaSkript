package dev.mukulx.javaskript.api.event;

import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Player;

/**
 * Context passed to listeners of custom inter-script events. Encapsulates the event name,
 * arguments, and optional cancellation state.
 */
public class CustomEventContext {

  private final String name;
  private final Object[] args;
  private boolean cancelled = false;

  public CustomEventContext(String name, Object[] args) {
    this.name = name;
    this.args = args != null ? args : new Object[0];
  }

  public String getName() {
    return name;
  }

  public Object[] getArgs() {
    return args;
  }

  public int count() {
    return args.length;
  }

  public boolean has(int index) {
    return index >= 0 && index < args.length && args[index] != null;
  }

  public Object get(int index) {
    if (index < 0 || index >= args.length) return null;
    return args[index];
  }

  @SuppressWarnings("unchecked")
  public <T> T get(int index, Class<T> type) {
    Object val = get(index);
    if (val != null && type.isInstance(val)) {
      return (T) val;
    }
    return null;
  }

  public <T> T get(int index, Class<T> type, T defaultValue) {
    T val = get(index, type);
    return val != null ? val : defaultValue;
  }

  /**
   * Helper to find the first argument that is a Bukkit Player.
   *
   * @return The Player, or null if none was passed
   */
  public Player getPlayer() {
    for (Object arg : args) {
      if (arg instanceof Player p) {
        return p;
      }
    }
    return null;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  public List<Object> asList() {
    return Arrays.asList(args);
  }

  @Override
  public String toString() {
    return "CustomEventContext{name='"
        + name
        + "', args="
        + Arrays.toString(args)
        + ", cancelled="
        + cancelled
        + "}";
  }
}
