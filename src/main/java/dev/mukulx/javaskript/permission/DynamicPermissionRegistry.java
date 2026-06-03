package dev.mukulx.javaskript.permission;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

/** Dynamically registers permissions without needing plugin.yml entries */
public class DynamicPermissionRegistry {

  private final JavaSkriptPlugin plugin;
  private final PluginManager pluginManager;
  private final Map<String, Permission> registeredPermissions;

  public DynamicPermissionRegistry(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.pluginManager = plugin.getServer().getPluginManager();
    this.registeredPermissions = new HashMap<>();
  }

  /**
   * Register a permission dynamically
   *
   * @param permission The permission string (e.g., "myplugin.admin")
   * @param description Description of the permission
   * @param defaultValue Default permission level (TRUE, FALSE, OP, NOT_OP)
   * @return true if successful
   */
  public boolean registerPermission(
      String permission, String description, PermissionDefault defaultValue) {
    if (permission == null || permission.isEmpty()) {
      plugin.getLogger().warning("Cannot register permission with null or empty name");
      return false;
    }

    try {
      // Check if permission already exists
      Permission existingPerm = pluginManager.getPermission(permission);
      if (existingPerm != null) {
        // Update existing permission
        existingPerm.setDescription(description != null ? description : "");
        existingPerm.setDefault(defaultValue != null ? defaultValue : PermissionDefault.OP);
        plugin.getLogger().info("Updated existing permission: " + permission);
        return true;
      }

      // Create new permission
      Permission perm =
          new Permission(
              permission,
              description != null ? description : "",
              defaultValue != null ? defaultValue : PermissionDefault.OP);

      // Register with plugin manager
      pluginManager.addPermission(perm);
      registeredPermissions.put(permission, perm);

      plugin
          .getLogger()
          .info("Registered permission: " + permission + " (default: " + perm.getDefault() + ")");

      // Recalculate permissions for online players
      recalculatePermissions();

      return true;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error registering permission: " + permission, e);
      return false;
    }
  }

  /**
   * Register a permission with default OP level
   *
   * @param permission The permission string
   * @param description Description of the permission
   * @return true if successful
   */
  public boolean registerPermission(String permission, String description) {
    return registerPermission(permission, description, PermissionDefault.OP);
  }

  /**
   * Register a permission with default OP level and no description
   *
   * @param permission The permission string
   * @return true if successful
   */
  public boolean registerPermission(String permission) {
    return registerPermission(permission, "", PermissionDefault.OP);
  }

  /**
   * Register multiple permissions at once
   *
   * @param permissions Map of permission name to description
   * @param defaultValue Default permission level for all
   * @return Number of successfully registered permissions
   */
  public int registerPermissions(Map<String, String> permissions, PermissionDefault defaultValue) {
    if (permissions == null || permissions.isEmpty()) {
      return 0;
    }

    int count = 0;
    for (Map.Entry<String, String> entry : permissions.entrySet()) {
      if (registerPermission(entry.getKey(), entry.getValue(), defaultValue)) {
        count++;
      }
    }

    return count;
  }

  /**
   * Unregister a permission
   *
   * @param permission The permission to unregister
   * @return true if successful
   */
  public boolean unregisterPermission(String permission) {
    if (permission == null || permission.isEmpty()) {
      return false;
    }

    try {
      Permission perm = pluginManager.getPermission(permission);
      if (perm != null) {
        pluginManager.removePermission(perm);
        registeredPermissions.remove(permission);

        plugin.getLogger().info("Unregistered permission: " + permission);

        // Recalculate permissions for online players
        recalculatePermissions();

        return true;
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "Error unregistering permission: " + permission, e);
    }

    return false;
  }

  /** Unregister all permissions registered by this registry */
  public void unregisterAll() {
    List<String> permissions = new ArrayList<>(registeredPermissions.keySet());
    for (String permission : permissions) {
      unregisterPermission(permission);
    }
  }

  /**
   * Check if a permission is registered
   *
   * @param permission The permission to check
   * @return true if registered
   */
  public boolean isPermissionRegistered(String permission) {
    if (permission == null || permission.isEmpty()) {
      return false;
    }
    return registeredPermissions.containsKey(permission);
  }

  /**
   * Get all registered permissions
   *
   * @return Map of permission names to Permission objects
   */
  public Map<String, Permission> getRegisteredPermissions() {
    return Collections.unmodifiableMap(registeredPermissions);
  }

  /**
   * Register a permission with children (parent permission)
   *
   * @param permission The parent permission
   * @param description Description
   * @param defaultValue Default level
   * @param children Map of child permissions to inherit (true = inherit, false = negate)
   * @return true if successful
   */
  public boolean registerPermissionWithChildren(
      String permission,
      String description,
      PermissionDefault defaultValue,
      Map<String, Boolean> children) {
    if (permission == null || permission.isEmpty()) {
      return false;
    }

    try {
      // Create permission with children
      Permission perm =
          new Permission(
              permission,
              description != null ? description : "",
              defaultValue != null ? defaultValue : PermissionDefault.OP,
              children != null ? children : new HashMap<>());

      // Unregister if exists
      Permission existing = pluginManager.getPermission(permission);
      if (existing != null) {
        pluginManager.removePermission(existing);
      }

      // Register
      pluginManager.addPermission(perm);
      registeredPermissions.put(permission, perm);

      plugin.getLogger().info("Registered permission with children: " + permission);

      // Recalculate permissions
      recalculatePermissions();

      return true;

    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "Error registering permission with children: " + permission, e);
      return false;
    }
  }

  /**
   * Create a wildcard permission (e.g., "myplugin.*")
   *
   * @param basePermission The base permission (e.g., "myplugin")
   * @param childPermissions List of child permissions to include
   * @return true if successful
   */
  public boolean registerWildcardPermission(String basePermission, List<String> childPermissions) {
    if (basePermission == null || basePermission.isEmpty()) {
      return false;
    }

    String wildcardPerm = basePermission + ".*";
    Map<String, Boolean> children = new HashMap<>();

    if (childPermissions != null) {
      for (String child : childPermissions) {
        children.put(child, true);
      }
    }

    return registerPermissionWithChildren(
        wildcardPerm,
        "Grants all " + basePermission + " permissions",
        PermissionDefault.OP,
        children);
  }

  /** Recalculate permissions for all online players */
  private void recalculatePermissions() {
    try {
      plugin
          .getServer()
          .getScheduler()
          .runTask(
              plugin,
              () -> {
                plugin
                    .getServer()
                    .getOnlinePlayers()
                    .forEach(
                        player -> {
                          if (player != null) {
                            player.recalculatePermissions();
                          }
                        });
              });
    } catch (Exception e) {
      // Ignore - not critical
    }
  }
}
