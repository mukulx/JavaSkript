package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.script.ScriptInstance;
import dev.mukulx.javaskript.util.ServerUtil;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/** Typed, scheduler-safe access to another loaded script. */
public final class ScriptHandle<T> {
  private final JavaSkriptPlugin plugin;
  private final String scriptKey;
  private final Class<T> type;

  ScriptHandle(JavaSkriptPlugin plugin, String scriptKey, Class<T> type) {
    this.plugin = plugin;
    this.scriptKey = scriptKey;
    this.type = type;
  }

  public CompletableFuture<Void> callGlobal(Consumer<T> action) {
    return supplyGlobal(
            target -> {
              action.accept(target);
              return null;
            })
        .thenApply(ignored -> null);
  }

  public <R> CompletableFuture<R> supplyGlobal(Function<T, R> action) {
    CompletableFuture<R> future = new CompletableFuture<>();
    Runnable invocation =
        () -> {
          try {
            ScriptInstance script = plugin.getScriptManager().getScript(scriptKey);
            if (script == null || script.getInstance() == null) {
              throw new IllegalStateException("Script is not loaded: " + scriptKey);
            }
            Object instance = script.getInstance();
            if (!type.isInstance(instance)) {
              throw new IllegalStateException(
                  "Script " + scriptKey + " is not a " + type.getName());
            }
            future.complete(action.apply(type.cast(instance)));
          } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
          }
        };
    if (ServerUtil.isFolia()) {
      plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> invocation.run());
    } else {
      plugin.getServer().getScheduler().runTask(plugin, invocation);
    }
    return future;
  }
}
