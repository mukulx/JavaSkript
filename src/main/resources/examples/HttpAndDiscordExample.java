package examples;

import dev.mukulx.javaskript.api.http.Http;
import dev.mukulx.javaskript.api.http.HttpHelper;
import dev.mukulx.javaskript.api.player.Players;
import java.awt.Color;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Demonstrates asynchronous HTTP requests and Discord Webhooks in JavaSkript.
 *
 * <p>Supports sending 1-line rich Discord embeds, REST GET/POST requests, and parsing JSON
 * responses with zero external dependencies or boilerplate.
 */
public class HttpAndDiscordExample implements Listener {

  // Auto-injected helper (or use static Http.get/post/discord)
  private HttpHelper http;

  // Replace with your server's Discord webhook URL
  private static final String DISCORD_WEBHOOK =
      "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN";

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // 1. Send rich Discord embed on join (100% async, non-blocking)
    http.discord(DISCORD_WEBHOOK)
        .username("Server Bot")
        .avatarUrl("https://minotar.net/avatar/" + player.getName() + "/100.png")
        .title("Player Joined")
        .description("**" + player.getName() + "** connected to the server.")
        .color(0x55FF55)
        .thumbnail("https://minotar.net/avatar/" + player.getName() + "/100.png")
        .field("Online Count", String.valueOf(Players.online().size()), true)
        .field("Ping", player.getPing() + "ms", true)
        .timestamp()
        .footer("JavaSkript Webhook", null)
        .send();

    // 2. Query an external REST API (GET JSON)
    Http.request("https://api.github.com/zen")
        .GET()
        .header("Accept", "text/plain")
        .send(
            (status, body) -> {
              if (status == 200) {
                Players.msg(player, "<gray>GitHub Zen of the day: <italic>" + body + "</italic>");
              }
            });
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    // 3. Simple Discord embed using AWT Color
    http.discord(DISCORD_WEBHOOK)
        .title("Player Left")
        .description(player.getName() + " left the server.")
        .color(Color.RED)
        .timestamp()
        .send();
  }

  /** Example of posting JSON payload to an external server. */
  public void postTelemetry(String eventType, Map<String, Object> data) {
    http.postJson(
        "https://api.example.com/telemetry",
        data,
        responseJson -> {
          // Process response
        });
  }
}
