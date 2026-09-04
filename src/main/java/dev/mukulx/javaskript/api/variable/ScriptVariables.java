package dev.mukulx.javaskript.api.variable;

import java.util.LinkedHashSet;
import java.util.Set;

/** Namespaced view of shared variables owned by one script. */
public final class ScriptVariables {
  private final VariableHelper delegate;
  private final String prefix;

  public ScriptVariables(VariableHelper delegate, String scriptKey) {
    this.delegate = delegate;
    this.prefix = scriptKey + ":";
  }

  public void set(String key, Object value) {
    delegate.set(key(key), value);
  }

  public Object get(String key) {
    return delegate.get(key(key));
  }

  public <T> T get(String key, Class<T> type) {
    return delegate.get(key(key), type);
  }

  public <T> T get(String key, T defaultValue) {
    return delegate.get(key(key), defaultValue);
  }

  public boolean has(String key) {
    return delegate.has(key(key));
  }

  public Object remove(String key) {
    return delegate.remove(key(key));
  }

  public void setPersistent(String key, Object value) {
    delegate.setPersistent(key(key), value);
  }

  public Object getPersistent(String key) {
    return delegate.getPersistent(key(key));
  }

  public boolean hasPersistent(String key) {
    return delegate.hasPersistent(key(key));
  }

  public Object removePersistent(String key) {
    return delegate.removePersistent(key(key));
  }

  public Set<String> keys() {
    return unprefix(delegate.keys());
  }

  public Set<String> persistentKeys() {
    return unprefix(delegate.persistentKeys());
  }

  private String key(String key) {
    if (key == null || key.isBlank())
      throw new IllegalArgumentException("Variable key cannot be blank");
    return prefix + key;
  }

  private Set<String> unprefix(Set<String> keys) {
    Set<String> result = new LinkedHashSet<>();
    for (String key : keys) if (key.startsWith(prefix)) result.add(key.substring(prefix.length()));
    return result;
  }
}
