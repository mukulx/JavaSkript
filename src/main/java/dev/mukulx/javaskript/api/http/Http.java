package dev.mukulx.javaskript.api.http;

import com.google.gson.JsonElement;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Static facade for JavaSkript HTTP requests and Discord Webhooks. Allows sending non-blocking
 * requests anywhere without field declarations.
 */
public final class Http {

  private static HttpHelper instance;

  private Http() {}

  public static void setInstance(HttpHelper helper) {
    instance = helper;
  }

  private static HttpHelper get() {
    if (instance == null) {
      throw new IllegalStateException("Http facade has not been initialized yet!");
    }
    return instance;
  }

  public static DiscordWebhook discord(String webhookUrl) {
    return get().discord(webhookUrl);
  }

  public static HttpRequestBuilder request(String url) {
    return get().request(url);
  }

  public static CompletableFuture<HttpResponse<String>> get(String url) {
    return get().get(url);
  }

  public static void get(String url, BiConsumer<Integer, String> callback) {
    get().get(url, callback);
  }

  public static void getJson(String url, Consumer<JsonElement> callback) {
    get().getJson(url, callback);
  }

  public static CompletableFuture<HttpResponse<String>> post(String url, String body) {
    return get().post(url, body);
  }

  public static CompletableFuture<HttpResponse<String>> postJson(String url, Object jsonPayload) {
    return get().postJson(url, jsonPayload);
  }

  public static void post(String url, String body, BiConsumer<Integer, String> callback) {
    get().post(url, body, callback);
  }

  public static void postJson(String url, Object jsonPayload, Consumer<JsonElement> callback) {
    get().postJson(url, jsonPayload, callback);
  }

  public static CompletableFuture<HttpResponse<String>> put(String url, String body) {
    return get().put(url, body);
  }

  public static CompletableFuture<HttpResponse<String>> delete(String url) {
    return get().delete(url);
  }
}
