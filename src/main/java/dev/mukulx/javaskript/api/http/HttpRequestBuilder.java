package dev.mukulx.javaskript.api.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.mukulx.javaskript.JavaSkriptPlugin;
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
  private final HttpHelper owner;
  private final String url;
  private String method = "GET";
  private final Map<String, String> headers = new LinkedHashMap<>();
  private HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
  private Duration timeout = Duration.ofSeconds(15);
  private Consumer<Throwable> errorHandler = null;

  public HttpRequestBuilder(HttpClient httpClient, String url) {
    this(httpClient, (HttpHelper) null, url);
  }

  public HttpRequestBuilder(HttpClient httpClient, JavaSkriptPlugin plugin, String url) {
    this(httpClient, plugin != null ? new HttpHelper(plugin, httpClient, null) : null, url);
  }

  HttpRequestBuilder(HttpClient httpClient, HttpHelper owner, String url) {
    this.httpClient = httpClient;
    this.owner = owner;
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

  /** Called on the server thread when the request fails or its JSON cannot be parsed. */
  public HttpRequestBuilder onError(Consumer<Throwable> errorHandler) {
    this.errorHandler = errorHandler;
    return this;
  }

  /**
   * Send the request. The future fails if the request does; {@link #onError} is also called when
   * set.
   */
  public CompletableFuture<HttpResponse<String>> sendAsync() {
    CompletableFuture<HttpResponse<String>> future = execute();
    if (errorHandler != null) {
      future.whenComplete(
          (response, error) -> {
            if (error != null) {
              fail(error);
            }
          });
    }
    return future;
  }

  /**
   * Execute the request asynchronously with status code and body string callback.
   *
   * @param callback BiConsumer receiving (statusCode, responseBody)
   */
  public void send(BiConsumer<Integer, String> callback) {
    execute()
        .whenComplete(
            (response, error) -> {
              if (error != null) {
                fail(error);
              } else if (callback != null) {
                deliver(() -> callback.accept(response.statusCode(), response.body()));
              }
            });
  }

  /**
   * Execute the request and parse the JSON response body asynchronously.
   *
   * @param callback Consumer receiving parsed JsonElement
   */
  public void sendJson(Consumer<JsonElement> callback) {
    execute()
        .whenComplete(
            (response, error) -> {
              if (error != null) {
                fail(error);
                return;
              }
              JsonElement element;
              try {
                element = JsonParser.parseString(response.body());
              } catch (RuntimeException e) {
                fail(e);
                return;
              }
              if (callback != null) {
                deliver(() -> callback.accept(element));
              }
            });
  }

  /** Sends the request and invokes callbacks on the global server scheduler. */
  public void sendGlobal(Consumer<HttpResponse<String>> success, Consumer<Throwable> failure) {
    execute()
        .whenComplete(
            (response, error) -> {
              if (error == null) {
                if (success != null) {
                  deliver(() -> success.accept(response));
                }
              } else if (failure != null) {
                Throwable cause = HttpHelper.unwrap(error);
                deliver(() -> failure.accept(cause));
              } else {
                fail(error);
              }
            });
  }

  private CompletableFuture<HttpResponse<String>> execute() {
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

      return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString());
    } catch (Throwable t) {
      return CompletableFuture.failedFuture(t);
    }
  }

  private void fail(Throwable error) {
    Throwable cause = HttpHelper.unwrap(error);
    if (errorHandler != null) {
      deliver(() -> errorHandler.accept(cause));
    } else if (owner != null) {
      owner.reportFailure(method, url, cause);
    }
  }

  private void deliver(Runnable callback) {
    if (owner != null) {
      owner.deliver(callback);
    } else {
      callback.run();
    }
  }
}
