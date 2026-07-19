package dev.mukulx.javaskript.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {

  private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/DYqeyxbi/version";
  private final JavaSkriptPlugin plugin;
  private String latestVersion = null;

  public UpdateChecker(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
  }

  public void checkAsync() {
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::check);
  }

  private void check() {
    try {
      URL url = new URL(MODRINTH_API);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);
      connection.setRequestProperty(
          "User-Agent", "JavaSkript/" + plugin.getDescription().getVersion());

      int responseCode = connection.getResponseCode();
      if (responseCode != 200) {
        plugin.debug("Failed to check for updates: HTTP " + responseCode);
        return;
      }

      BufferedReader reader =
          new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
      reader.close();

      JsonArray versions = JsonParser.parseString(response.toString()).getAsJsonArray();
      if (versions.size() == 0) {
        plugin.debug("No versions found on Modrinth");
        return;
      }

      latestVersion = versions.get(0).getAsJsonObject().get("version_number").getAsString();
      String currentVersion = plugin.getDescription().getVersion();

      if (!latestVersion.equals(currentVersion)) {
        plugin
            .getServer()
            .getScheduler()
            .runTask(
                plugin,
                () -> {
                  plugin.getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                  plugin.getLogger().warning("Update available!");
                  plugin.getLogger().warning("Current: " + currentVersion);
                  plugin.getLogger().warning("Latest: " + latestVersion);
                  plugin.getLogger().warning("Download: https://modrinth.com/plugin/javaskript");
                  plugin.getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                });
      } else {
        plugin.debug("Plugin is up to date");
      }

    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "Failed to check for updates", e);
    }
  }

  public String getLatestVersion() {
    return latestVersion;
  }
}
