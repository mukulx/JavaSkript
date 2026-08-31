package dev.mukulx.javaskript.api.http;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Fluent builder for executing asynchronous Discord Webhooks with embeds. */
public class DiscordWebhook {

  private static final Gson GSON = new Gson();

  private final HttpClient httpClient;
  private final String webhookUrl;
  private String content;
  private String username;
  private String avatarUrl;
  private boolean tts = false;
  private final List<DiscordEmbed> embeds = new ArrayList<>();
  private DiscordEmbed defaultEmbed = null;
  private Consumer<Throwable> errorHandler = null;

  public DiscordWebhook(HttpClient httpClient, String webhookUrl) {
    this.httpClient = httpClient;
    this.webhookUrl = webhookUrl;
  }

  private DiscordEmbed getOrCreateDefaultEmbed() {
    if (defaultEmbed == null) {
      defaultEmbed = new DiscordEmbed();
      embeds.add(defaultEmbed);
    }
    return defaultEmbed;
  }

  public DiscordWebhook content(String content) {
    this.content = content;
    return this;
  }

  public DiscordWebhook username(String username) {
    this.username = username;
    return this;
  }

  public DiscordWebhook avatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
    return this;
  }

  public DiscordWebhook tts(boolean tts) {
    this.tts = tts;
    return this;
  }

  // Delegate directly to the primary embed for 1-line syntax:
  public DiscordWebhook title(String title) {
    getOrCreateDefaultEmbed().title(title);
    return this;
  }

  public DiscordWebhook title(String title, String url) {
    getOrCreateDefaultEmbed().title(title, url);
    return this;
  }

  public DiscordWebhook description(String description) {
    getOrCreateDefaultEmbed().description(description);
    return this;
  }

  public DiscordWebhook color(int rgb) {
    getOrCreateDefaultEmbed().color(rgb);
    return this;
  }

  public DiscordWebhook color(java.awt.Color color) {
    getOrCreateDefaultEmbed().color(color);
    return this;
  }

  public DiscordWebhook timestamp() {
    getOrCreateDefaultEmbed().timestamp();
    return this;
  }

  public DiscordWebhook footer(String text) {
    getOrCreateDefaultEmbed().footer(text);
    return this;
  }

  public DiscordWebhook footer(String text, String iconUrl) {
    getOrCreateDefaultEmbed().footer(text, iconUrl);
    return this;
  }

  public DiscordWebhook thumbnail(String url) {
    getOrCreateDefaultEmbed().thumbnail(url);
    return this;
  }

  public DiscordWebhook image(String url) {
    getOrCreateDefaultEmbed().image(url);
    return this;
  }

  public DiscordWebhook author(String name, String iconUrl) {
    getOrCreateDefaultEmbed().author(name, iconUrl);
    return this;
  }

  public DiscordWebhook field(String name, String value, boolean inline) {
    getOrCreateDefaultEmbed().field(name, value, inline);
    return this;
  }

  public DiscordWebhook field(String name, String value) {
    getOrCreateDefaultEmbed().field(name, value, false);
    return this;
  }

  public DiscordWebhook embed(Consumer<DiscordEmbed> embedConsumer) {
    DiscordEmbed embed = new DiscordEmbed();
    embedConsumer.accept(embed);
    embeds.add(embed);
    return this;
  }

  public DiscordWebhook addEmbed(DiscordEmbed embed) {
    if (embed != null) {
      embeds.add(embed);
    }
    return this;
  }

  public DiscordWebhook onError(Consumer<Throwable> errorHandler) {
    this.errorHandler = errorHandler;
    return this;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> payload = new LinkedHashMap<>();
    if (content != null) payload.put("content", content);
    if (username != null) payload.put("username", username);
    if (avatarUrl != null) payload.put("avatar_url", avatarUrl);
    if (tts) payload.put("tts", true);

    if (!embeds.isEmpty()) {
      List<Map<String, Object>> embedMaps = new ArrayList<>();
      for (DiscordEmbed embed : embeds) {
        embedMaps.add(embed.toMap());
      }
      payload.put("embeds", embedMaps);
    }
    return payload;
  }

  /** Execute the webhook asynchronously (fire-and-forget). */
  public void send() {
    sendAsync();
  }

  /**
   * Execute the webhook asynchronously with completion callback.
   *
   * @param callback Called with true on success (HTTP 200/204), false otherwise
   */
  public void send(Consumer<Boolean> callback) {
    sendAsync()
        .thenAccept(
            success -> {
              if (callback != null) {
                callback.accept(success);
              }
            });
  }

  /**
   * Execute the webhook asynchronously returning a CompletableFuture.
   *
   * @return CompletableFuture completing with true on success
   */
  public CompletableFuture<Boolean> sendAsync() {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      CompletableFuture<Boolean> failed = new CompletableFuture<>();
      failed.complete(false);
      return failed;
    }

    try {
      String json = GSON.toJson(toMap());
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(webhookUrl))
              .header("Content-Type", "application/json")
              .header("User-Agent", "JavaSkript-DiscordWebhook/1.1")
              .POST(HttpRequest.BodyPublishers.ofString(json))
              .build();

      return httpClient
          .sendAsync(request, HttpResponse.BodyHandlers.discarding())
          .thenApply(
              response -> {
                int code = response.statusCode();
                return code >= 200 && code < 300;
              })
          .exceptionally(
              ex -> {
                if (errorHandler != null) {
                  errorHandler.accept(ex);
                }
                return false;
              });
    } catch (Throwable t) {
      if (errorHandler != null) {
        errorHandler.accept(t);
      }
      return CompletableFuture.completedFuture(false);
    }
  }
}
