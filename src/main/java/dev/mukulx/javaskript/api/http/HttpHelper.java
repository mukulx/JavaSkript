package dev.mukulx.javaskript.api.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Built-in asynchronous HTTP client and Discord Webhook engine for JavaSkript.
 *
 * <p>Enables sending REST requests (GET, POST, PUT, DELETE) and rich Discord webhooks with zero
 * external dependencies or boilerplate.
 */
public class HttpHelper {

  private static final Gson GSON = new Gson();

  private final JavaSkriptPlugin plugin;
  private final HttpClient httpClient;

  public HttpHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
  }

  public HttpClient getClient() {
    return httpClient;
  }

  // ==========================================
  // Discord Webhook Builder
  // ==========================================

  /**
   * Create a fluent Discord Webhook builder.
   *
   * @param webhookUrl The Discord webhook URL
   * @return A DiscordWebhook builder instance
   */
  public DiscordWebhook discord(String webhookUrl) {
    return new DiscordWebhook(httpClient, webhookUrl);
  }

  // ==========================================
  // Fluent HTTP Request Builder
  // ==========================================

  /**
   * Create a fluent request builder for any URL.
   *
   * @param url Target URL
   * @return HttpRequestBuilder
   */
  public HttpRequestBuilder request(String url) {
    return new HttpRequestBuilder(httpClient, url);
  }

  // ==========================================
  // Async GET
  // ==========================================

  public CompletableFuture<HttpResponse<String>> get(String url) {
    return request(url).GET().sendAsync();
  }

  public void get(String url, BiConsumer<Integer, String> callback) {
    request(url).GET().send(callback);
  }

  public void getJson(String url, Consumer<JsonElement> callback) {
    request(url).GET().sendJson(callback);
  }

  // ==========================================
  // Async POST
  // ==========================================

  public CompletableFuture<HttpResponse<String>> post(String url, String body) {
    return request(url).POST().body(body).sendAsync();
  }

  public CompletableFuture<HttpResponse<String>> postJson(String url, Object jsonPayload) {
    return request(url).POST().bodyJson(jsonPayload).sendAsync();
  }

  public void post(String url, String body, BiConsumer<Integer, String> callback) {
    request(url).POST().body(body).send(callback);
  }

  public void postJson(String url, Object jsonPayload, Consumer<JsonElement> callback) {
    request(url).POST().bodyJson(jsonPayload).sendJson(callback);
  }

  // ==========================================
  // Async PUT & DELETE
  // ==========================================

  public CompletableFuture<HttpResponse<String>> put(String url, String body) {
    return request(url).PUT().body(body).sendAsync();
  }

  public CompletableFuture<HttpResponse<String>> delete(String url) {
    return request(url).DELETE().sendAsync();
  }
}
