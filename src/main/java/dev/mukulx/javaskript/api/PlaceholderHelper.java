package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.bukkit.OfflinePlayer;

/** Easy PlaceholderAPI integration for scripts Allows scripts to register custom placeholders */
public class PlaceholderHelper {

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private final Map<String, BiFunction<OfflinePlayer, String, String>> placeholders;
  private boolean papiAvailable = false;
  private Object expansion = null;

  public PlaceholderHelper(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName = scriptName.replace(".java", "");
    this.placeholders = new HashMap<>();

    // Check if PlaceholderAPI is available
    try {
      Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      papiAvailable = true;
    } catch (ClassNotFoundException e) {
      // PlaceholderAPI not installed
    }
  }

  /**
   * Register a placeholder
   *
   * @param identifier The placeholder identifier (e.g., "myscript_value")
   * @param handler Function that returns the placeholder value
   * @return true if registered successfully
   */
  public boolean registerPlaceholder(
      String identifier, BiFunction<OfflinePlayer, String, String> handler) {
    if (!papiAvailable) {
      plugin
          .getLogger()
          .warning(
              "["
                  + scriptName
                  + "] PlaceholderAPI not found! Cannot register placeholder: "
                  + identifier);
      return false;
    }

    if (identifier == null || identifier.isEmpty() || handler == null) {
      return false;
    }

    placeholders.put(identifier.toLowerCase(), handler);

    // Register expansion if not already registered
    if (expansion == null) {
      registerExpansion();
    }

    plugin
        .getLogger()
        .info(
            "[" + scriptName + "] Registered placeholder: %" + scriptName + "_" + identifier + "%");
    return true;
  }

  /**
   * Register a simple placeholder (no player context)
   *
   * @param identifier The placeholder identifier
   * @param value The static value
   * @return true if registered successfully
   */
  public boolean registerPlaceholder(String identifier, String value) {
    return registerPlaceholder(identifier, (player, params) -> value);
  }

  /**
   * Unregister a placeholder
   *
   * @param identifier The placeholder identifier
   * @return true if unregistered successfully
   */
  public boolean unregisterPlaceholder(String identifier) {
    if (identifier == null || identifier.isEmpty()) {
      return false;
    }

    return placeholders.remove(identifier.toLowerCase()) != null;
  }

  /** Unregister all placeholders */
  public void unregisterAll() {
    placeholders.clear();

    if (expansion != null && papiAvailable) {
      try {
        expansion.getClass().getMethod("unregister").invoke(expansion);
      } catch (Exception e) {
        plugin
            .getLogger()
            .log(
                Level.WARNING,
                "[" + scriptName + "] Failed to unregister PlaceholderAPI expansion",
                e);
      }
      expansion = null;
    }
  }

  /**
   * Get a placeholder value
   *
   * @param identifier The placeholder identifier
   * @param player The player
   * @param params Additional parameters
   * @return The placeholder value or null
   */
  public String getPlaceholder(String identifier, OfflinePlayer player, String params) {
    BiFunction<OfflinePlayer, String, String> handler = placeholders.get(identifier.toLowerCase());
    if (handler != null) {
      try {
        return handler.apply(player, params);
      } catch (Exception e) {
        plugin
            .getLogger()
            .log(
                Level.WARNING,
                "[" + scriptName + "] Error in placeholder handler: " + identifier,
                e);
      }
    }
    return null;
  }

  /**
   * Check if PlaceholderAPI is available
   *
   * @return true if available
   */
  public boolean isPlaceholderAPIAvailable() {
    return papiAvailable;
  }

  /**
   * Parse placeholders in a string (if PlaceholderAPI is available)
   *
   * @param player The player
   * @param text The text with placeholders
   * @return Parsed text
   */
  public String parsePlaceholders(OfflinePlayer player, String text) {
    if (!papiAvailable || text == null) {
      return text;
    }

    try {
      Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      return (String)
          papiClass
              .getMethod("setPlaceholders", OfflinePlayer.class, String.class)
              .invoke(null, player, text);
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "[" + scriptName + "] Failed to parse placeholders", e);
      return text;
    }
  }

  private void registerExpansion() {
    try {
      Class<?> expansionClass =
          Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");

      // Create anonymous expansion class
      expansion =
          java.lang.reflect.Proxy.newProxyInstance(
              getClass().getClassLoader(),
              new Class[] {expansionClass},
              (proxy, method, args) -> {
                String methodName = method.getName();

                return switch (methodName) {
                  case "getIdentifier" -> scriptName.toLowerCase();
                  case "getAuthor" -> "JavaSkript";
                  case "getVersion" -> "1.0.0";
                  case "persist" -> true;
                  case "onPlaceholderRequest" -> {
                    if (args != null && args.length >= 2) {
                      OfflinePlayer player = (OfflinePlayer) args[0];
                      String params = (String) args[1];
                      yield getPlaceholder(params, player, params);
                    }
                    yield null;
                  }
                  default -> null;
                };
              });

      // Register the expansion
      expansionClass.getMethod("register").invoke(expansion);

    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "[" + scriptName + "] Failed to register PlaceholderAPI expansion", e);
    }
  }
}
