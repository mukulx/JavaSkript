package dev.mukulx.javaskript.api.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
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
  private final String scriptKey;
  private volatile boolean closed;

  public HttpHelper(JavaSkriptPlugin plugin) {
    this(
        plugin,
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build(),
        null);
  }

  HttpHelper(JavaSkriptPlugin plugin, HttpClient httpClient, String scriptKey) {
    this.plugin = plugin;
    this.httpClient = httpClient;
    this.scriptKey = scriptKey;
  }

  /** A helper for one script that shares this client and stops calling back once closed. */
  public HttpHelper forScript(String scriptKey) {
    return new HttpHelper(plugin, httpClient, scriptKey);
  }

  /** Drop the callbacks of requests still in flight, so they never run unloaded script code. */
  public void close() {
    closed = true;
  }

  /** Run a callback on the server thread, unless its script or the plugin has since stopped. */
  void deliver(Runnable callback) {
    if (closed || plugin == null || !plugin.isEnabled()) {
      return;
    }
    ServerUtil.runSync(plugin, callback);
  }

  /** Log a failure nobody handled. Only the host is named: URLs can carry tokens. */
  void reportFailure(String method, String url, Throwable error) {
    if (plugin == null) {
      return;
    }
    String prefix = scriptKey != null ? "[" + scriptKey + "] " : "";
    plugin
        .getLogger()
        .warning(prefix + method + " request to " + host(url) + " failed: " + unwrap(error));
  }

  static Throwable unwrap(Throwable error) {
    while ((error instanceof CompletionException || error instanceof ExecutionException)
        && error.getCause() != null) {
      error = error.getCause();
    }
    return error;
  }

  private static String host(String url) {
    try {
      String host = URI.create(url).getHost();
      return host != null ? host : "an invalid URL";
    } catch (RuntimeException e) {
      return "an invalid URL";
    }
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
    return new DiscordWebhook(this, webhookUrl);
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
    return new HttpRequestBuilder(httpClient, this, url);
  }

  /** Fetch a URL and run the callback safely on the global server scheduler. */
  public void getGlobal(String url, Consumer<HttpResponse<String>> callback) {
    request(url).GET().sendGlobal(callback, null);
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
