package dev.mukulx.javaskript.util;

import org.bukkit.Bukkit;

/** Utility class for server detection and compatibility checks. */
public class ServerUtil {

  // Memoized state flag to avoid reflection lookups on every single tick
  private static Boolean isFolia = null;

  /**
   * Check if the server is running Folia.
   *
   * @return true if running on Folia, false otherwise
   */
  public static boolean isFolia() {
    if (isFolia == null) {
      try {
        // Look up class signature for Folia's multi-threaded region manager
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
        isFolia = true;
      } catch (ClassNotFoundException e) {
        isFolia = false;
      }
    }
    return isFolia;
  }

  /**
   * Check if the server is running Paper (not Folia).
   *
   * @return true if running on Paper, false if Folia
   */
  public static boolean isPaper() {
    return !isFolia();
  }

  /**
   * Get the server type as a string.
   *
   * @return "Folia" or "Paper"
   */
  public static String getServerType() {
    return isFolia() ? "Folia" : "Paper";
  }

  /**
   * Get the server version.
   *
   * @return server version string
   */
  public static String getServerVersion() {
    return Bukkit.getVersion(); // Grabs implementation metadata from the server instance
  }
}
