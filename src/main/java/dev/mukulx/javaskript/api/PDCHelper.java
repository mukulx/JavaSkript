package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

/**
 * Modern, fluent PersistentDataContainer (PDC) / NBT Helper for JavaSkript scripts.
 *
 * <p>Enables ultra-fast, zero-boilerplate reading and writing of permanent custom data on:
 *
 * <ul>
 *   <li>{@link ItemStack} (items, weapons, armor, tools)
 *   <li>{@link PersistentDataHolder} (entities, players, tile blocks, chunks, worlds)
 *   <li>{@link ItemMeta}
 * </ul>
 */
public class PDCHelper {

  private final JavaSkriptPlugin plugin;
  private final String defaultNamespace;

  public PDCHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.defaultNamespace = "javaskript";
  }

  public PDCHelper(JavaSkriptPlugin plugin, String defaultNamespace) {
    this.plugin = plugin;
    this.defaultNamespace =
        defaultNamespace != null ? defaultNamespace.toLowerCase() : "javaskript";
  }

  // ==========================================
  // NamespacedKey Factory
  // ==========================================

  /**
   * Creates a NamespacedKey from a key string. If the key contains a colon (e.g. "custom:key"), it
   * parses it directly. Otherwise, it uses the plugin's namespace.
   */
  public NamespacedKey key(String key) {
    if (key == null || key.isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }
    if (key.contains(":")) {
      NamespacedKey parsed = NamespacedKey.fromString(key);
      if (parsed != null) {
        return parsed;
      }
    }
    String normalized = key.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    return new NamespacedKey(plugin, normalized);
  }

  // ==========================================
  // Core Generic Operations
  // ==========================================

  public <T, Z> boolean set(ItemStack item, String key, PersistentDataType<T, Z> type, Z value) {
    if (item == null || item.getType().isAir() || key == null || value == null) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }
    meta.getPersistentDataContainer().set(key(key), type, value);
    return item.setItemMeta(meta);
  }

  public <T, Z> boolean set(
      PersistentDataHolder holder, String key, PersistentDataType<T, Z> type, Z value) {
    if (holder == null || key == null || value == null) {
      return false;
    }
    holder.getPersistentDataContainer().set(key(key), type, value);
    return true;
  }

  public <T, Z> boolean set(ItemMeta meta, String key, PersistentDataType<T, Z> type, Z value) {
    if (meta == null || key == null || value == null) {
      return false;
    }
    meta.getPersistentDataContainer().set(key(key), type, value);
    return true;
  }

  public <T, Z> Z get(ItemStack item, String key, PersistentDataType<T, Z> type) {
    if (item == null || item.getType().isAir() || key == null) {
      return null;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return null;
    }
    return meta.getPersistentDataContainer().get(key(key), type);
  }

  public <T, Z> Z get(PersistentDataHolder holder, String key, PersistentDataType<T, Z> type) {
    if (holder == null || key == null) {
      return null;
    }
    return holder.getPersistentDataContainer().get(key(key), type);
  }

  public <T, Z> Z get(ItemMeta meta, String key, PersistentDataType<T, Z> type) {
    if (meta == null || key == null) {
      return null;
    }
    return meta.getPersistentDataContainer().get(key(key), type);
  }

  public <T, Z> Z getOrDefault(
      ItemStack item, String key, PersistentDataType<T, Z> type, Z defaultValue) {
    Z val = get(item, key, type);
    return val != null ? val : defaultValue;
  }

  public <T, Z> Z getOrDefault(
      PersistentDataHolder holder, String key, PersistentDataType<T, Z> type, Z defaultValue) {
    Z val = get(holder, key, type);
    return val != null ? val : defaultValue;
  }

  public <T, Z> Z getOrDefault(
      ItemMeta meta, String key, PersistentDataType<T, Z> type, Z defaultValue) {
    Z val = get(meta, key, type);
    return val != null ? val : defaultValue;
  }

  // ==========================================
  // Existence & Removal
  // ==========================================

  public boolean has(ItemStack item, String key) {
    if (item == null || item.getType().isAir() || key == null) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }
    return meta.getPersistentDataContainer().has(key(key));
  }

  public boolean has(PersistentDataHolder holder, String key) {
    if (holder == null || key == null) {
      return false;
    }
    return holder.getPersistentDataContainer().has(key(key));
  }

  public boolean has(ItemMeta meta, String key) {
    if (meta == null || key == null) {
      return false;
    }
    return meta.getPersistentDataContainer().has(key(key));
  }

  public boolean remove(ItemStack item, String key) {
    if (item == null || item.getType().isAir() || key == null) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }
    meta.getPersistentDataContainer().remove(key(key));
    return item.setItemMeta(meta);
  }

  public boolean remove(PersistentDataHolder holder, String key) {
    if (holder == null || key == null) {
      return false;
    }
    holder.getPersistentDataContainer().remove(key(key));
    return true;
  }

  public boolean remove(ItemMeta meta, String key) {
    if (meta == null || key == null) {
      return false;
    }
    meta.getPersistentDataContainer().remove(key(key));
    return true;
  }

  public Set<NamespacedKey> getKeys(ItemStack item) {
    if (item == null || item.getType().isAir()) {
      return Collections.emptySet();
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return Collections.emptySet();
    }
    return meta.getPersistentDataContainer().getKeys();
  }

  public Set<NamespacedKey> getKeys(PersistentDataHolder holder) {
    if (holder == null) {
      return Collections.emptySet();
    }
    return holder.getPersistentDataContainer().getKeys();
  }

  public Set<NamespacedKey> getKeys(ItemMeta meta) {
    if (meta == null) {
      return Collections.emptySet();
    }
    return meta.getPersistentDataContainer().getKeys();
  }

  public void clear(ItemStack item) {
    if (item == null || item.getType().isAir()) {
      return;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return;
    }
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    for (NamespacedKey k : new HashSet<>(pdc.getKeys())) {
      pdc.remove(k);
    }
    item.setItemMeta(meta);
  }

  public void clear(PersistentDataHolder holder) {
    if (holder == null) {
      return;
    }
    PersistentDataContainer pdc = holder.getPersistentDataContainer();
    for (NamespacedKey k : new HashSet<>(pdc.getKeys())) {
      pdc.remove(k);
    }
  }

  // ==========================================
  // ItemStack Typed Convenience (Setters)
  // ==========================================

  public boolean set(ItemStack item, String key, String value) {
    return set(item, key, PersistentDataType.STRING, value);
  }

  public boolean set(ItemStack item, String key, int value) {
    return set(item, key, PersistentDataType.INTEGER, value);
  }

  public boolean set(ItemStack item, String key, double value) {
    return set(item, key, PersistentDataType.DOUBLE, value);
  }

  public boolean set(ItemStack item, String key, float value) {
    return set(item, key, PersistentDataType.FLOAT, value);
  }

  public boolean set(ItemStack item, String key, long value) {
    return set(item, key, PersistentDataType.LONG, value);
  }

  public boolean set(ItemStack item, String key, short value) {
    return set(item, key, PersistentDataType.SHORT, value);
  }

  public boolean set(ItemStack item, String key, byte value) {
    return set(item, key, PersistentDataType.BYTE, value);
  }

  public boolean set(ItemStack item, String key, boolean value) {
    return set(item, key, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
  }

  public boolean set(ItemStack item, String key, UUID value) {
    return set(item, key, PersistentDataType.STRING, value != null ? value.toString() : null);
  }

  public boolean set(ItemStack item, String key, List<String> values) {
    if (values == null) return false;
    String joined = String.join("\u0000", values);
    return set(item, key, PersistentDataType.STRING, joined);
  }

  public boolean set(ItemStack item, String key, int[] values) {
    return set(item, key, PersistentDataType.INTEGER_ARRAY, values);
  }

  public boolean set(ItemStack item, String key, byte[] values) {
    return set(item, key, PersistentDataType.BYTE_ARRAY, values);
  }

  public boolean set(ItemStack item, String key, long[] values) {
    return set(item, key, PersistentDataType.LONG_ARRAY, values);
  }

  // ==========================================
  // ItemStack Typed Convenience (Getters)
  // ==========================================

  public String getString(ItemStack item, String key) {
    return get(item, key, PersistentDataType.STRING);
  }

  public String getString(ItemStack item, String key, String defaultValue) {
    return getOrDefault(item, key, PersistentDataType.STRING, defaultValue);
  }

  public Integer getInt(ItemStack item, String key) {
    return get(item, key, PersistentDataType.INTEGER);
  }

  public int getInt(ItemStack item, String key, int defaultValue) {
    Integer val = getInt(item, key);
    return val != null ? val : defaultValue;
  }

  public Double getDouble(ItemStack item, String key) {
    return get(item, key, PersistentDataType.DOUBLE);
  }

  public double getDouble(ItemStack item, String key, double defaultValue) {
    Double val = getDouble(item, key);
    return val != null ? val : defaultValue;
  }

  public Float getFloat(ItemStack item, String key) {
    return get(item, key, PersistentDataType.FLOAT);
  }

  public float getFloat(ItemStack item, String key, float defaultValue) {
    Float val = getFloat(item, key);
    return val != null ? val : defaultValue;
  }

  public Long getLong(ItemStack item, String key) {
    return get(item, key, PersistentDataType.LONG);
  }

  public long getLong(ItemStack item, String key, long defaultValue) {
    Long val = getLong(item, key);
    return val != null ? val : defaultValue;
  }

  public Short getShort(ItemStack item, String key) {
    return get(item, key, PersistentDataType.SHORT);
  }

  public short getShort(ItemStack item, String key, short defaultValue) {
    Short val = getShort(item, key);
    return val != null ? val : defaultValue;
  }

  public Byte getByte(ItemStack item, String key) {
    return get(item, key, PersistentDataType.BYTE);
  }

  public byte getByte(ItemStack item, String key, byte defaultValue) {
    Byte val = getByte(item, key);
    return val != null ? val : defaultValue;
  }

  public Boolean getBoolean(ItemStack item, String key) {
    Byte val = getByte(item, key);
    return val != null ? val == 1 : null;
  }

  public boolean getBoolean(ItemStack item, String key, boolean defaultValue) {
    Byte val = getByte(item, key);
    return val != null ? val == 1 : defaultValue;
  }

  public UUID getUUID(ItemStack item, String key) {
    String str = getString(item, key);
    if (str == null) return null;
    try {
      return UUID.fromString(str);
    } catch (Exception e) {
      return null;
    }
  }

  public UUID getUUID(ItemStack item, String key, UUID defaultValue) {
    UUID val = getUUID(item, key);
    return val != null ? val : defaultValue;
  }

  public List<String> getStringList(ItemStack item, String key) {
    String str = getString(item, key);
    if (str == null || str.isEmpty()) {
      return Collections.emptyList();
    }
    return new ArrayList<>(Arrays.asList(str.split("\u0000")));
  }

  public List<String> getStringList(ItemStack item, String key, List<String> defaultValue) {
    if (!has(item, key)) {
      return defaultValue;
    }
    return getStringList(item, key);
  }

  public int[] getIntArray(ItemStack item, String key) {
    return get(item, key, PersistentDataType.INTEGER_ARRAY);
  }

  public byte[] getByteArray(ItemStack item, String key) {
    return get(item, key, PersistentDataType.BYTE_ARRAY);
  }

  public long[] getLongArray(ItemStack item, String key) {
    return get(item, key, PersistentDataType.LONG_ARRAY);
  }

  // ==========================================
  // PersistentDataHolder Typed Convenience (Setters)
  // (Entities, Players, TileBlocks, Chunks, Worlds)
  // ==========================================

  public boolean set(PersistentDataHolder holder, String key, String value) {
    return set(holder, key, PersistentDataType.STRING, value);
  }

  public boolean set(PersistentDataHolder holder, String key, int value) {
    return set(holder, key, PersistentDataType.INTEGER, value);
  }

  public boolean set(PersistentDataHolder holder, String key, double value) {
    return set(holder, key, PersistentDataType.DOUBLE, value);
  }

  public boolean set(PersistentDataHolder holder, String key, float value) {
    return set(holder, key, PersistentDataType.FLOAT, value);
  }

  public boolean set(PersistentDataHolder holder, String key, long value) {
    return set(holder, key, PersistentDataType.LONG, value);
  }

  public boolean set(PersistentDataHolder holder, String key, short value) {
    return set(holder, key, PersistentDataType.SHORT, value);
  }

  public boolean set(PersistentDataHolder holder, String key, byte value) {
    return set(holder, key, PersistentDataType.BYTE, value);
  }

  public boolean set(PersistentDataHolder holder, String key, boolean value) {
    return set(holder, key, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
  }

  public boolean set(PersistentDataHolder holder, String key, UUID value) {
    return set(holder, key, PersistentDataType.STRING, value != null ? value.toString() : null);
  }

  public boolean set(PersistentDataHolder holder, String key, List<String> values) {
    if (values == null) return false;
    String joined = String.join("\u0000", values);
    return set(holder, key, PersistentDataType.STRING, joined);
  }

  public boolean set(PersistentDataHolder holder, String key, int[] values) {
    return set(holder, key, PersistentDataType.INTEGER_ARRAY, values);
  }

  public boolean set(PersistentDataHolder holder, String key, byte[] values) {
    return set(holder, key, PersistentDataType.BYTE_ARRAY, values);
  }

  public boolean set(PersistentDataHolder holder, String key, long[] values) {
    return set(holder, key, PersistentDataType.LONG_ARRAY, values);
  }

  // ==========================================
  // PersistentDataHolder Typed Convenience (Getters)
  // ==========================================

  public String getString(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.STRING);
  }

  public String getString(PersistentDataHolder holder, String key, String defaultValue) {
    return getOrDefault(holder, key, PersistentDataType.STRING, defaultValue);
  }

  public Integer getInt(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.INTEGER);
  }

  public int getInt(PersistentDataHolder holder, String key, int defaultValue) {
    Integer val = getInt(holder, key);
    return val != null ? val : defaultValue;
  }

  public Double getDouble(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.DOUBLE);
  }

  public double getDouble(PersistentDataHolder holder, String key, double defaultValue) {
    Double val = getDouble(holder, key);
    return val != null ? val : defaultValue;
  }

  public Float getFloat(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.FLOAT);
  }

  public float getFloat(PersistentDataHolder holder, String key, float defaultValue) {
    Float val = getFloat(holder, key);
    return val != null ? val : defaultValue;
  }

  public Long getLong(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.LONG);
  }

  public long getLong(PersistentDataHolder holder, String key, long defaultValue) {
    Long val = getLong(holder, key);
    return val != null ? val : defaultValue;
  }

  public Short getShort(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.SHORT);
  }

  public short getShort(PersistentDataHolder holder, String key, short defaultValue) {
    Short val = getShort(holder, key);
    return val != null ? val : defaultValue;
  }

  public Byte getByte(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.BYTE);
  }

  public byte getByte(PersistentDataHolder holder, String key, byte defaultValue) {
    Byte val = getByte(holder, key);
    return val != null ? val : defaultValue;
  }

  public Boolean getBoolean(PersistentDataHolder holder, String key) {
    Byte val = getByte(holder, key);
    return val != null ? val == 1 : null;
  }

  public boolean getBoolean(PersistentDataHolder holder, String key, boolean defaultValue) {
    Byte val = getByte(holder, key);
    return val != null ? val == 1 : defaultValue;
  }

  public UUID getUUID(PersistentDataHolder holder, String key) {
    String str = getString(holder, key);
    if (str == null) return null;
    try {
      return UUID.fromString(str);
    } catch (Exception e) {
      return null;
    }
  }

  public UUID getUUID(PersistentDataHolder holder, String key, UUID defaultValue) {
    UUID val = getUUID(holder, key);
    return val != null ? val : defaultValue;
  }

  public List<String> getStringList(PersistentDataHolder holder, String key) {
    String str = getString(holder, key);
    if (str == null || str.isEmpty()) {
      return Collections.emptyList();
    }
    return new ArrayList<>(Arrays.asList(str.split("\u0000")));
  }

  public List<String> getStringList(
      PersistentDataHolder holder, String key, List<String> defaultValue) {
    if (!has(holder, key)) {
      return defaultValue;
    }
    return getStringList(holder, key);
  }

  public int[] getIntArray(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.INTEGER_ARRAY);
  }

  public byte[] getByteArray(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.BYTE_ARRAY);
  }

  public long[] getLongArray(PersistentDataHolder holder, String key) {
    return get(holder, key, PersistentDataType.LONG_ARRAY);
  }

  // ==========================================
  // ItemMeta Typed Convenience (Setters & Getters)
  // ==========================================

  public boolean set(ItemMeta meta, String key, String value) {
    return set(meta, key, PersistentDataType.STRING, value);
  }

  public boolean set(ItemMeta meta, String key, int value) {
    return set(meta, key, PersistentDataType.INTEGER, value);
  }

  public boolean set(ItemMeta meta, String key, double value) {
    return set(meta, key, PersistentDataType.DOUBLE, value);
  }

  public boolean set(ItemMeta meta, String key, float value) {
    return set(meta, key, PersistentDataType.FLOAT, value);
  }

  public boolean set(ItemMeta meta, String key, long value) {
    return set(meta, key, PersistentDataType.LONG, value);
  }

  public boolean set(ItemMeta meta, String key, short value) {
    return set(meta, key, PersistentDataType.SHORT, value);
  }

  public boolean set(ItemMeta meta, String key, byte value) {
    return set(meta, key, PersistentDataType.BYTE, value);
  }

  public boolean set(ItemMeta meta, String key, boolean value) {
    return set(meta, key, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
  }

  public boolean set(ItemMeta meta, String key, UUID value) {
    return set(meta, key, PersistentDataType.STRING, value != null ? value.toString() : null);
  }

  public String getString(ItemMeta meta, String key) {
    return get(meta, key, PersistentDataType.STRING);
  }

  public String getString(ItemMeta meta, String key, String defaultValue) {
    return getOrDefault(meta, key, PersistentDataType.STRING, defaultValue);
  }

  public Integer getInt(ItemMeta meta, String key) {
    return get(meta, key, PersistentDataType.INTEGER);
  }

  public int getInt(ItemMeta meta, String key, int defaultValue) {
    Integer val = getInt(meta, key);
    return val != null ? val : defaultValue;
  }

  public Double getDouble(ItemMeta meta, String key) {
    return get(meta, key, PersistentDataType.DOUBLE);
  }

  public double getDouble(ItemMeta meta, String key, double defaultValue) {
    Double val = getDouble(meta, key);
    return val != null ? val : defaultValue;
  }

  public Float getFloat(ItemMeta meta, String key) {
    return get(meta, key, PersistentDataType.FLOAT);
  }

  public float getFloat(ItemMeta meta, String key, float defaultValue) {
    Float val = getFloat(meta, key);
    return val != null ? val : defaultValue;
  }

  public Long getLong(ItemMeta meta, String key) {
    return get(meta, key, PersistentDataType.LONG);
  }

  public long getLong(ItemMeta meta, String key, long defaultValue) {
    Long val = getLong(meta, key);
    return val != null ? val : defaultValue;
  }

  public Short getShort(ItemMeta meta, String key) {
    return get(meta, key, PersistentDataType.SHORT);
  }

  public short getShort(ItemMeta meta, String key, short defaultValue) {
    Short val = getShort(meta, key);
    return val != null ? val : defaultValue;
  }

  public Byte getByte(ItemMeta meta, String key) {
    return get(meta, key, PersistentDataType.BYTE);
  }

  public byte getByte(ItemMeta meta, String key, byte defaultValue) {
    Byte val = getByte(meta, key);
    return val != null ? val : defaultValue;
  }

  public Boolean getBoolean(ItemMeta meta, String key) {
    Byte val = getByte(meta, key);
    return val != null ? val == 1 : null;
  }

  public boolean getBoolean(ItemMeta meta, String key, boolean defaultValue) {
    Byte val = getByte(meta, key);
    return val != null ? val == 1 : defaultValue;
  }

  public UUID getUUID(ItemMeta meta, String key) {
    String str = getString(meta, key);
    if (str == null) return null;
    try {
      return UUID.fromString(str);
    } catch (Exception e) {
      return null;
    }
  }

  public UUID getUUID(ItemMeta meta, String key, UUID defaultValue) {
    UUID val = getUUID(meta, key);
    return val != null ? val : defaultValue;
  }
}
