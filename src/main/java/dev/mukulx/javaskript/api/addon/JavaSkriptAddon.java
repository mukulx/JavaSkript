package dev.mukulx.javaskript.api.addon;

import dev.mukulx.javaskript.api.JavaSkriptAPI;

/** Interface implemented by external plugins or addons extending JavaSkript. */
public interface JavaSkriptAddon {

  /**
   * The display name of this addon.
   *
   * @return Addon name (e.g. "JavaSkript-Discord", "WorldGuard-Bridge")
   */
  String getName();

  /**
   * The version string of this addon.
   *
   * @return Version string (e.g. "1.0.0")
   */
  String getVersion();

  /**
   * The author(s) of this addon.
   *
   * @return Author name
   */
  default String getAuthor() {
    return "Unknown";
  }

  /**
   * A short description of this addon.
   *
   * @return Description text
   */
  default String getDescription() {
    return "";
  }

  /**
   * Called when the addon is registered with JavaSkript.
   *
   * @param api The JavaSkript public API instance
   */
  default void onEnable(JavaSkriptAPI api) {}

  /**
   * Called when the addon is unregistered or JavaSkript is shutting down.
   *
   * @param api The JavaSkript public API instance
   */
  default void onDisable(JavaSkriptAPI api) {}
}
