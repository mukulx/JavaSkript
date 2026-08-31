package dev.mukulx.javaskript.api.variable;

import java.util.Map;
import java.util.Set;

/**
 * Static facade for JavaSkript shared variables. Allows reading, writing, and atomically updating
 * shared state from any script or class.
 */
public final class Variables {

  private static VariableHelper instance;

  private Variables() {}

  public static void setInstance(VariableHelper helper) {
    instance = helper;
  }

  private static VariableHelper get() {
    if (instance == null) {
      throw new IllegalStateException("Variables facade has not been initialized yet!");
    }
    return instance;
  }

  public static void set(String key, Object value) {
    get().set(key, value);
  }

  public static Object get(String key) {
    return get().get(key);
  }

  public static <T> T get(String key, Class<T> type) {
    return get().get(key, type);
  }

  public static <T> T get(String key, T defaultValue) {
    return get().get(key, defaultValue);
  }

  public static String getString(String key, String defaultValue) {
    return get().getString(key, defaultValue);
  }

  public static String getString(String key) {
    return get().getString(key);
  }

  public static int getInt(String key, int defaultValue) {
    return get().getInt(key, defaultValue);
  }

  public static int getInt(String key) {
    return get().getInt(key);
  }

  public static long getLong(String key, long defaultValue) {
    return get().getLong(key, defaultValue);
  }

  public static long getLong(String key) {
    return get().getLong(key);
  }

  public static double getDouble(String key, double defaultValue) {
    return get().getDouble(key, defaultValue);
  }

  public static double getDouble(String key) {
    return getDouble(key, 0.0);
  }

  public static boolean getBoolean(String key, boolean defaultValue) {
    return get().getBoolean(key, defaultValue);
  }

  public static boolean getBoolean(String key) {
    return get().getBoolean(key);
  }

  public static long increment(String key, long amount) {
    return get().increment(key, amount);
  }

  public static double increment(String key, double amount) {
    return get().increment(key, amount);
  }

  public static long decrement(String key, long amount) {
    return get().decrement(key, amount);
  }

  public static double decrement(String key, double amount) {
    return get().decrement(key, amount);
  }

  public static boolean has(String key) {
    return get().has(key);
  }

  public static Object remove(String key) {
    return get().remove(key);
  }

  public static void clear() {
    get().clear();
  }

  public static Set<String> keys() {
    return get().keys();
  }

  public static Map<String, Object> asMap() {
    return get().asMap();
  }

  // Persistent
  public static void setPersistent(String key, Object value) {
    get().setPersistent(key, value);
  }

  public static Object getPersistent(String key) {
    return get().getPersistent(key);
  }

  public static <T> T getPersistent(String key, Class<T> type) {
    return get().getPersistent(key, type);
  }

  public static <T> T getPersistent(String key, T defaultValue) {
    return get().getPersistent(key, defaultValue);
  }

  public static String getPersistentString(String key, String defaultValue) {
    return get().getPersistentString(key, defaultValue);
  }

  public static int getPersistentInt(String key, int defaultValue) {
    return get().getPersistentInt(key, defaultValue);
  }

  public static double getPersistentDouble(String key, double defaultValue) {
    return get().getPersistentDouble(key, defaultValue);
  }

  public static boolean hasPersistent(String key) {
    return get().hasPersistent(key);
  }

  public static Object removePersistent(String key) {
    return get().removePersistent(key);
  }
}
