package dev.mukulx.javaskript.api.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Fluent builder for general asynchronous HTTP REST requests. */
public class HttpRequestBuilder {

  private static final Gson GSON = new Gson();

  private final HttpClient httpClient;
  private final JavaSkriptPlugin plugin;
  private final String url;
  private String method = "GET";
  private final Map<String, String> headers = new LinkedHashMap<>();
  private HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
  private Duration timeout = Duration.ofSeconds(15);
  private Consumer<Throwable> errorHandler = null;

  public HttpRequestBuilder(HttpClient httpClient, String url) {
    this(httpClient, null, url);
  }

  public HttpRequestBuilder(HttpClient httpClient, JavaSkriptPlugin plugin, String url) {
    this.httpClient = httpClient;
    this.plugin = plugin;
    this.url = url;
  }

  public HttpRequestBuilder method(String method) {
    this.method = method != null ? method.toUpperCase() : "GET";
    return this;
  }

  public HttpRequestBuilder GET() {
    return method("GET");
  }

  public HttpRequestBuilder POST() {
    return method("POST");
  }

  public HttpRequestBuilder PUT() {
    return method("PUT");
  }

  public HttpRequestBuilder DELETE() {
    return method("DELETE");
  }

  public HttpRequestBuilder header(String key, String value) {
    if (key != null && value != null) {
      headers.put(key, value);
    }
    return this;
  }

  public HttpRequestBuilder bearerAuth(String token) {
    return header("Authorization", "Bearer " + token);
  }

  public HttpRequestBuilder basicAuth(String username, String password) {
    String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    return header("Authorization", "Basic " + encoded);
  }

  public HttpRequestBuilder timeout(Duration timeout) {
    if (timeout != null) {
      this.timeout = timeout;
    }
    return this;
  }

  public HttpRequestBuilder body(String body) {
    if (body != null) {
      this.bodyPublisher = HttpRequest.BodyPublishers.ofString(body);
    }
    return this;
  }

  public HttpRequestBuilder bodyJson(Object object) {
    header("Content-Type", "application/json");
    if (object instanceof String str) {
      return body(str);
    }
    return body(GSON.toJson(object));
  }

  public HttpRequestBuilder onError(Consumer<Throwable> errorHandler) {
    this.errorHandler = errorHandler;
    return this;
  }

  public CompletableFuture<HttpResponse<String>> sendAsync() {
    try {
      HttpRequest.Builder builder =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(timeout)
              .method(method, bodyPublisher);

      headers.forEach(builder::header);
      if (!headers.containsKey("User-Agent")) {
        builder.header("User-Agent", "JavaSkript-HttpClient/1.1");
      }

      HttpRequest request = builder.build();
      return httpClient
          .sendAsync(request, HttpResponse.BodyHandlers.ofString())
          .exceptionally(
              ex -> {
                if (errorHandler != null) {
                  errorHandler.accept(ex);
                }
                return null;
              });
    } catch (Throwable t) {
      if (errorHandler != null) {
        errorHandler.accept(t);
      }
      return CompletableFuture.completedFuture(null);
    }
  }

  /**
   * Execute the request asynchronously with status code and body string callback.
   *
   * @param callback BiConsumer receiving (statusCode, responseBody)
   */
  public void send(BiConsumer<Integer, String> callback) {
    sendAsync()
        .thenAccept(
            resp -> {
              if (resp != null && callback != null) {
                dispatchGlobal(() -> callback.accept(resp.statusCode(), resp.body()));
              }
            });
  }

  /**
   * Execute the request and parse the JSON response body asynchronously.
   *
   * @param callback Consumer receiving parsed JsonElement
   */
  public void sendJson(Consumer<JsonElement> callback) {
    sendAsync()
        .thenAccept(
            resp -> {
              if (resp != null && callback != null) {
                try {
                  JsonElement element = JsonParser.parseString(resp.body());
                  dispatchGlobal(() -> callback.accept(element));
                } catch (Exception e) {
                  if (errorHandler != null) {
                    errorHandler.accept(e);
                  }
                }
              }
            });
  }

  /** Sends the request and invokes callbacks on the global server scheduler. */
  public void sendGlobal(Consumer<HttpResponse<String>> success, Consumer<Throwable> failure) {
    sendAsync()
        .whenComplete(
            (response, throwable) -> {
              if (throwable != null) {
                if (failure != null) dispatchGlobal(() -> failure.accept(throwable));
              } else if (response != null && success != null) {
                dispatchGlobal(() -> success.accept(response));
              }
            });
  }

  private void dispatchGlobal(Runnable action) {
    if (plugin == null || !plugin.isEnabled()) {
      action.run();
    } else if (ServerUtil.isFolia()) {
      plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> action.run());
    } else {
      plugin.getServer().getScheduler().runTask(plugin, action);
    }
  }
}
