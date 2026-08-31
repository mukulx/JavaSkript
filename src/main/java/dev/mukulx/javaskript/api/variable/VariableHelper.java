package dev.mukulx.javaskript.api.variable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe shared variable storage engine for JavaSkript scripts.
 *
 * <p>Provides in-memory key-value caching accessible across all isolated script ClassLoaders, as
 * well as optional disk-persisted variables that survive server restarts.
 */
public class VariableHelper {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final JavaSkriptPlugin plugin;
  private final File persistentFile;
  private final Map<String, Object> memoryStore = new ConcurrentHashMap<>();
  private final Map<String, Object> persistentStore = new ConcurrentHashMap<>();
  private final AtomicBoolean dirty = new AtomicBoolean(false);

  public VariableHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    File dataFolder = new File(plugin.getDataFolder(), "script-data");
    if (!dataFolder.exists()) {
      dataFolder.mkdirs();
    }
    this.persistentFile = new File(dataFolder, "variables.json");
    loadPersistent();
  }

  // ==========================================
  // In-Memory Shared Variables
  // ==========================================

  public void set(String key, Object value) {
    if (key == null) return;
    if (value == null) {
      memoryStore.remove(key);
    } else {
      memoryStore.put(key, value);
    }
  }

  public Object get(String key) {
    if (key == null) return null;
    return memoryStore.get(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key, Class<T> type) {
    Object val = get(key);
    if (val != null && type.isInstance(val)) {
      return (T) val;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key, T defaultValue) {
    Object val = get(key);
    if (val == null) return defaultValue;
    if (defaultValue != null && defaultValue.getClass().isInstance(val)) {
      return (T) val;
    }
    return (T) val;
  }

  public String getString(String key, String defaultValue) {
    Object val = get(key);
    return val != null ? String.valueOf(val) : defaultValue;
  }

  public String getString(String key) {
    return getString(key, null);
  }

  public int getInt(String key, int defaultValue) {
    Object val = get(key);
    if (val instanceof Number num) {
      return num.intValue();
    }
    if (val instanceof String str) {
      try {
        return Integer.parseInt(str);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  public int getInt(String key) {
    return getInt(key, 0);
  }

  public long getLong(String key, long defaultValue) {
    Object val = get(key);
    if (val instanceof Number num) {
      return num.longValue();
    }
    if (val instanceof String str) {
      try {
        return Long.parseLong(str);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  public long getLong(String key) {
    return getLong(key, 0L);
  }

  public double getDouble(String key, double defaultValue) {
    Object val = get(key);
    if (val instanceof Number num) {
      return num.doubleValue();
    }
    if (val instanceof String str) {
      try {
        return Double.parseDouble(str);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  public double getDouble(String key) {
    return getDouble(key, 0.0);
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    Object val = get(key);
    if (val instanceof Boolean b) {
      return b;
    }
    if (val instanceof String str) {
      return Boolean.parseBoolean(str);
    }
    return defaultValue;
  }

  public boolean getBoolean(String key) {
    return getBoolean(key, false);
  }

  public long increment(String key, long amount) {
    if (key == null) return 0;
    synchronized (memoryStore) {
      long current = getLong(key, 0L);
      long updated = current + amount;
      memoryStore.put(key, updated);
      return updated;
    }
  }

  public double increment(String key, double amount) {
    if (key == null) return 0.0;
    synchronized (memoryStore) {
      double current = getDouble(key, 0.0);
      double updated = current + amount;
      memoryStore.put(key, updated);
      return updated;
    }
  }

  public long decrement(String key, long amount) {
    return increment(key, -amount);
  }

  public double decrement(String key, double amount) {
    return increment(key, -amount);
  }

  public boolean has(String key) {
    return key != null && memoryStore.containsKey(key);
  }

  public Object remove(String key) {
    if (key == null) return null;
    return memoryStore.remove(key);
  }

  public void clear() {
    memoryStore.clear();
  }

  public Set<String> keys() {
    return Collections.unmodifiableSet(memoryStore.keySet());
  }

  public Map<String, Object> asMap() {
    return Collections.unmodifiableMap(memoryStore);
  }

  // ==========================================
  // Persistent Shared Variables (Survive Restarts)
  // ==========================================

  public void setPersistent(String key, Object value) {
    if (key == null) return;
    if (value == null) {
      persistentStore.remove(key);
    } else {
      persistentStore.put(key, value);
    }
    savePersistentAsync();
  }

  public Object getPersistent(String key) {
    if (key == null) return null;
    return persistentStore.get(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T getPersistent(String key, Class<T> type) {
    Object val = getPersistent(key);
    if (val != null && type.isInstance(val)) {
      return (T) val;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <T> T getPersistent(String key, T defaultValue) {
    Object val = getPersistent(key);
    if (val == null) return defaultValue;
    if (defaultValue != null && defaultValue.getClass().isInstance(val)) {
      return (T) val;
    }
    return (T) val;
  }

  public String getPersistentString(String key, String defaultValue) {
    Object val = getPersistent(key);
    return val != null ? String.valueOf(val) : defaultValue;
  }

  public int getPersistentInt(String key, int defaultValue) {
    Object val = getPersistent(key);
    if (val instanceof Number num) {
      return num.intValue();
    }
    if (val instanceof String str) {
      try {
        return Integer.parseInt(str);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  public double getPersistentDouble(String key, double defaultValue) {
    Object val = getPersistent(key);
    if (val instanceof Number num) {
      return num.doubleValue();
    }
    if (val instanceof String str) {
      try {
        return Double.parseDouble(str);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultValue;
  }

  public boolean hasPersistent(String key) {
    return key != null && persistentStore.containsKey(key);
  }

  public Object removePersistent(String key) {
    if (key == null) return null;
    Object old = persistentStore.remove(key);
    if (old != null) {
      savePersistentAsync();
    }
    return old;
  }

  public Set<String> persistentKeys() {
    return Collections.unmodifiableSet(persistentStore.keySet());
  }

  private void savePersistentAsync() {
    dirty.set(true);
    java.util.concurrent.CompletableFuture.runAsync(
        () -> {
          if (dirty.compareAndSet(true, false)) {
            savePersistent();
          }
        });
  }

  public synchronized void savePersistent() {
    try {
      File tmpFile = new File(persistentFile.getParentFile(), persistentFile.getName() + ".tmp");
      try (FileWriter writer = new FileWriter(tmpFile)) {
        GSON.toJson(persistentStore, writer);
      }

      try {
        Files.move(
            tmpFile.toPath(),
            persistentFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception fallback) {
        Files.move(tmpFile.toPath(), persistentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception e) {
      if (plugin != null) {
        plugin
            .getLogger()
            .warning("[Variables] Failed to save persistent variables: " + e.getMessage());
      }
    }
  }

  public synchronized void loadPersistent() {
    if (!persistentFile.exists()) {
      return;
    }
    try (FileReader reader = new FileReader(persistentFile)) {
      Type type = new TypeToken<Map<String, Object>>() {}.getType();
      Map<String, Object> loaded = GSON.fromJson(reader, type);
      if (loaded != null) {
        persistentStore.clear();
        persistentStore.putAll(loaded);
      }
    } catch (Exception e) {
      if (plugin != null) {
        plugin
            .getLogger()
            .warning("[Variables] Failed to load persistent variables: " + e.getMessage());
      }
    }
  }

  public void shutdown() {
    if (dirty.get()) {
      savePersistent();
    }
  }
}
