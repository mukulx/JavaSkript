package dev.mukulx.javaskript.api.resourcepack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serves pack zips over plain HTTP at {@code /<sha1>.zip}. Only published hashes are served, so no
 * file on disk is ever reachable through it.
 */
final class PackHttpServer {

  private static final Pattern PACK_PATH = Pattern.compile("^/([0-9a-f]{40})\\.zip$");

  private final HttpServer server;
  private final ExecutorService executor;
  private final Map<String, byte[]> files = new ConcurrentHashMap<>();

  private PackHttpServer(HttpServer server, ExecutorService executor) {
    this.server = server;
    this.executor = executor;
  }

  static PackHttpServer start(InetSocketAddress address) throws IOException {
    HttpServer server = HttpServer.create(address, 0);
    ExecutorService executor =
        Executors.newFixedThreadPool(
            4,
            r -> {
              Thread thread = new Thread(r, "JavaSkript-PackServer");
              thread.setDaemon(true);
              return thread;
            });
    PackHttpServer packServer = new PackHttpServer(server, executor);
    server.createContext("/", packServer::handle);
    server.setExecutor(executor);
    server.start();
    return packServer;
  }

  void publish(String sha1, byte[] data) {
    files.put(sha1, data);
  }

  void retire(String sha1) {
    files.remove(sha1);
  }

  int port() {
    return server.getAddress().getPort();
  }

  void stop() {
    server.stop(0);
    executor.shutdownNow();
    files.clear();
  }

  private void handle(HttpExchange exchange) throws IOException {
    try (exchange) {
      String method = exchange.getRequestMethod();
      if (!method.equals("GET") && !method.equals("HEAD")) {
        exchange.getResponseHeaders().set("Allow", "GET, HEAD");
        exchange.sendResponseHeaders(405, -1);
        return;
      }
      Matcher matcher = PACK_PATH.matcher(exchange.getRequestURI().getPath());
      byte[] data = matcher.matches() ? files.get(matcher.group(1)) : null;
      if (data == null) {
        exchange.sendResponseHeaders(404, -1);
        return;
      }
      exchange.getResponseHeaders().set("Content-Type", "application/zip");
      // Content never changes for a hash, so clients and proxies may cache it forever
      exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
      if (method.equals("HEAD")) {
        exchange.getResponseHeaders().set("Content-Length", Integer.toString(data.length));
        exchange.sendResponseHeaders(200, -1);
        return;
      }
      exchange.sendResponseHeaders(200, data.length);
      try (OutputStream body = exchange.getResponseBody()) {
        body.write(data);
      }
    }
  }
}
