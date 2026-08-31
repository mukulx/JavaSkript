package dev.mukulx.javaskript.api.addon;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptInstance;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for external plugins and addons. Manages registered addons and custom field injectors.
 */
public class AddonRegistry {

  private final JavaSkriptPlugin plugin;
  private final Map<String, JavaSkriptAddon> addons = new ConcurrentHashMap<>();
  private final Map<Class<?>, FieldInjector<?>> typeInjectors = new ConcurrentHashMap<>();
  private final Map<String, FieldInjector<?>> nameInjectors = new ConcurrentHashMap<>();
  private final List<FieldInjector<?>> universalInjectors = new CopyOnWriteArrayList<>();

  public AddonRegistry(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Register an addon.
   *
   * @param addon The addon to register
   */
  public void registerAddon(JavaSkriptAddon addon) {
    if (addon == null || addon.getName() == null) return;
    String key = addon.getName().toLowerCase();
    addons.put(key, addon);
    try {
      addon.onEnable(plugin.getAPI());
      plugin
          .getLogger()
          .info("[Addon] Registered addon: " + addon.getName() + " v" + addon.getVersion());
    } catch (Exception e) {
      plugin
          .getLogger()
          .severe("[Addon] Failed to initialize addon " + addon.getName() + ": " + e.getMessage());
    }
  }

  /**
   * Unregister an addon.
   *
   * @param addon The addon to unregister
   */
  public void unregisterAddon(JavaSkriptAddon addon) {
    if (addon == null || addon.getName() == null) return;
    String key = addon.getName().toLowerCase();
    if (addons.remove(key) != null) {
      try {
        addon.onDisable(plugin.getAPI());
      } catch (Exception ignored) {
      }
    }
  }

  /**
   * Get all registered addons.
   *
   * @return Unmodifiable collection of addons
   */
  public Collection<JavaSkriptAddon> getAddons() {
    return Collections.unmodifiableCollection(addons.values());
  }

  /**
   * Register a field injector by class type.
   *
   * @param type The class type to match
   * @param injector The provider
   */
  public <T> void registerInjector(Class<T> type, FieldInjector<T> injector) {
    if (type != null && injector != null) {
      typeInjectors.put(type, injector);
    }
  }

  /**
   * Register a field injector by field name (case-insensitive).
   *
   * @param fieldName The name of the field to match
   * @param injector The provider
   */
  public void registerInjector(String fieldName, FieldInjector<?> injector) {
    if (fieldName != null && injector != null) {
      nameInjectors.put(fieldName.toLowerCase(), injector);
    }
  }

  /**
   * Register a universal fallback injector that inspects all fields.
   *
   * @param injector The provider
   */
  public void registerUniversalInjector(FieldInjector<?> injector) {
    if (injector != null) {
      universalInjectors.add(injector);
    }
  }

  /**
   * Resolve a custom injection for a script field.
   *
   * @param instance The script instance
   * @param fieldType The declared field type
   * @param fieldName The declared field name
   * @return Injected object, or null if no injector matches
   */
  public Object resolveInjection(ScriptInstance instance, Class<?> fieldType, String fieldName) {
    // 1. Direct type match or subclass match
    FieldInjector<?> direct = typeInjectors.get(fieldType);
    if (direct != null) {
      try {
        Object val = direct.provide(instance, fieldType, fieldName);
        if (val != null) return val;
      } catch (Exception ignored) {
      }
    }

    for (Map.Entry<Class<?>, FieldInjector<?>> entry : typeInjectors.entrySet()) {
      if (entry.getKey().isAssignableFrom(fieldType)) {
        try {
          Object val = entry.getValue().provide(instance, fieldType, fieldName);
          if (val != null) return val;
        } catch (Exception ignored) {
        }
      }
    }

    // 2. Direct name match
    FieldInjector<?> nameInj = nameInjectors.get(fieldName.toLowerCase());
    if (nameInj != null) {
      try {
        Object val = nameInj.provide(instance, fieldType, fieldName);
        if (val != null) return val;
      } catch (Exception ignored) {
      }
    }

    // 3. Universal injectors
    for (FieldInjector<?> uInj : universalInjectors) {
      try {
        Object val = uInj.provide(instance, fieldType, fieldName);
        if (val != null) return val;
      } catch (Exception ignored) {
      }
    }

    return null;
  }

  /** Shutdown and unregister all addons. */
  public void shutdown() {
    for (JavaSkriptAddon addon : addons.values()) {
      try {
        addon.onDisable(plugin.getAPI());
      } catch (Exception ignored) {
      }
    }
    addons.clear();
    typeInjectors.clear();
    nameInjectors.clear();
    universalInjectors.clear();
  }
}
