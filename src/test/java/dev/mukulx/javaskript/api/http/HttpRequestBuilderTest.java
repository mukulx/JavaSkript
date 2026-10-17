package dev.mukulx.javaskript.api.http;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.ConnectException;
import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HttpRequestBuilderTest {

  // Nothing listens on port 1, so the connection is refused straight away
  private static final String UNREACHABLE = "http://127.0.0.1:1/";
  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void failedRequestFailsTheFuture() {
    CompletableFuture<?> future = new HttpRequestBuilder(client, UNREACHABLE).sendAsync();

    ExecutionException error =
        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    assertInstanceOf(ConnectException.class, HttpHelper.unwrap(error));
  }

  @Test
  void failedRequestReachesTheFailureCallback() throws Exception {
    CompletableFuture<Throwable> failure = new CompletableFuture<>();

    new HttpRequestBuilder(client, UNREACHABLE).sendGlobal(response -> {}, failure::complete);

    assertInstanceOf(ConnectException.class, failure.get(5, TimeUnit.SECONDS));
  }
}
