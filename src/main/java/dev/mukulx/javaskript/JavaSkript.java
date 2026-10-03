package dev.mukulx.javaskript;

import dev.mukulx.javaskript.api.JavaSkriptAPI;
import dev.mukulx.javaskript.api.addon.FieldInjector;
import dev.mukulx.javaskript.api.addon.JavaSkriptAddon;

/**
 * Static gateway to the JavaSkript API. Other plugins and addons can use this class to easily
 * interact with JavaSkript without boilerplate.
 */
public final class JavaSkript {

  private JavaSkript() {}

  /**
   * Get the JavaSkript public API.
   *
   * @return The JavaSkriptAPI instance
   * @throws IllegalStateException If JavaSkript is not installed or enabled
   */
  public static JavaSkriptAPI getAPI() {
    JavaSkriptPlugin plugin = JavaSkriptPlugin.getInstance();
    if (plugin == null || plugin.getAPI() == null) {
      throw new IllegalStateException("JavaSkript is not loaded or enabled yet!");
    }
    return plugin.getAPI();
  }

  /**
   * Check if JavaSkript is loaded and enabled on the server.
   *
   * @return true if JavaSkript is active
   */
  public static boolean isAvailable() {
    JavaSkriptPlugin plugin = JavaSkriptPlugin.getInstance();
    return plugin != null && plugin.isEnabled();
  }

  /**
   * Register an addon extending JavaSkript.
   *
   * @param addon The addon to register
   */
  public static void registerAddon(JavaSkriptAddon addon) {
    getAPI().registerAddon(addon);
  }

  /**
   * Register a custom field injector by class type for scripts.
   *
   * @param type The type of the field to match
   * @param injector The provider
   */
  public static <T> void registerInjector(Class<T> type, FieldInjector<T> injector) {
    getAPI().registerInjector(type, injector);
  }

  /**
   * Register a custom field injector by field name for scripts.
   *
   * @param fieldName The name of the field to match
   * @param injector The provider
   */
  public static void registerInjector(String fieldName, FieldInjector<?> injector) {
    getAPI().registerInjector(fieldName, injector);
  }

  /**
   * Get the VariableHelper managing shared in-memory and persistent script variables.
   *
   * @return VariableHelper instance
   */
  public static dev.mukulx.javaskript.api.variable.VariableHelper getVariables() {
    return getAPI().getVariableHelper();
  }

  /**
   * Get the inter-script Pub/Sub EventBus.
   *
   * @return ScriptEventBus instance
   */
  public static dev.mukulx.javaskript.api.event.ScriptEventBus getEventBus() {
    return getAPI().getEventBus();
  }

  /**
   * Get the HttpHelper for asynchronous HTTP requests and Discord webhooks.
   *
   * @return HttpHelper instance
   */
  public static dev.mukulx.javaskript.api.http.HttpHelper getHttp() {
    return getAPI().getHttpHelper();
  }

  /**
   * Get the MannequinHelper for creating and controlling Paper mannequins.
   *
   * @return MannequinHelper instance
   */
  public static dev.mukulx.javaskript.api.MannequinHelper getMannequins() {
    return getAPI().getMannequinHelper();
  }
}
