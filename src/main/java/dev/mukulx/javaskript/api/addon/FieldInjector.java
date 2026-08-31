package dev.mukulx.javaskript.api.addon;

import dev.mukulx.javaskript.script.ScriptInstance;

/**
 * Provides custom objects for injection into script fields. Addons and external plugins register
 * FieldInjectors to expose their custom APIs to scripts.
 */
@FunctionalInterface
public interface FieldInjector<T> {

  /**
   * Provide an instance to inject into a script field.
   *
   * @param instance The script instance receiving the injection
   * @param fieldType The type of the field declared in the script
   * @param fieldName The name of the field declared in the script
   * @return The object to inject, or null if this injector does not handle it
   */
  T provide(ScriptInstance instance, Class<?> fieldType, String fieldName);
}
