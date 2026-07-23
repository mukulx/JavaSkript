package dev.mukulx.javaskript.watcher;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class FileWatcher implements Runnable {

  private final JavaSkriptPlugin plugin;
  private final File scriptsFolder;
  private WatchService watchService;
  private final Map<WatchKey, Path> watchKeys;
  private final Map<String, Long> pendingReloads;
  private volatile boolean running = false;
  private Thread watchThread;
  private ScheduledExecutorService debounceExecutor;
  private final long reloadDelay;

  public FileWatcher(JavaSkriptPlugin plugin, File scriptsFolder) {
    this.plugin = plugin;
    this.scriptsFolder = scriptsFolder;
    this.watchKeys = new HashMap<>();
    this.pendingReloads = new ConcurrentHashMap<>();
    this.reloadDelay = plugin.getConfig().getLong("file-watcher.reload-delay", 500);
    this.debounceExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "JavaSkript-Debounce");
              t.setDaemon(true);
              return t;
            });
  }

  /** Start watching for file changes */
  public void start() {
    if (running) {
      return;
    }

    try {
      watchService = FileSystems.getDefault().newWatchService();

      // Register the scripts folder
      Path path = scriptsFolder.toPath();
      WatchKey key =
          path.register(
              watchService,
              StandardWatchEventKinds.ENTRY_CREATE,
              StandardWatchEventKinds.ENTRY_MODIFY,
              StandardWatchEventKinds.ENTRY_DELETE);
      watchKeys.put(key, path);

      running = true;
      watchThread = new Thread(this, "JavaSkript-FileWatcher");
      watchThread.setDaemon(true);
      watchThread.start();

    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to start file watcher", e);
    }
  }

  public void stop() {
    running = false;

    if (debounceExecutor != null && !debounceExecutor.isShutdown()) {
      debounceExecutor.shutdown();
      try {
        debounceExecutor.awaitTermination(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        debounceExecutor.shutdownNow();
      }
    }

    if (watchThread != null && watchThread.isAlive()) {
      watchThread.interrupt();
    }

    if (watchService != null) {
      try {
        watchService.close();
      } catch (IOException e) {
        plugin.getLogger().log(Level.WARNING, "Error closing file watcher", e);
      }
    }

    plugin.getLogger().info("File watcher stopped");
  }

  @Override
  public void run() {
    while (running) {
      try {
        WatchKey key = watchService.take();
        Path dir = watchKeys.get(key);

        if (dir == null) {
          continue;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();

          if (kind == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }

          @SuppressWarnings("unchecked")
          WatchEvent<Path> ev = (WatchEvent<Path>) event;
          Path filename = ev.context();
          Path fullPath = dir.resolve(filename);

          // Only process .java files
          if (!filename.toString().endsWith(".java")) {
            continue;
          }

          scheduleReload(kind, fullPath.toFile(), filename.toString());
        }

        boolean valid = key.reset();
        if (!valid) {
          watchKeys.remove(key);
          if (watchKeys.isEmpty()) {
            break;
          }
        }

      } catch (ClosedWatchServiceException e) {
        // Watch service was closed, exit gracefully
        break;
      } catch (InterruptedException e) {
        // Thread was interrupted, exit gracefully
        break;
      } catch (Exception e) {
        if (running) {
          plugin.getLogger().log(Level.SEVERE, "Error in file watcher", e);
        }
      }
    }
  }

  private void scheduleReload(WatchEvent.Kind<?> kind, File file, String fileName) {
    String key = fileName + ":" + kind.name();
    pendingReloads.put(key, System.currentTimeMillis());

    debounceExecutor.schedule(
        () -> {
          Long scheduledTime = pendingReloads.get(key);
          if (scheduledTime != null
              && System.currentTimeMillis() - scheduledTime >= reloadDelay - 50) {
            pendingReloads.remove(key);
            handleFileEvent(kind, file, fileName);
          }
        },
        reloadDelay,
        TimeUnit.MILLISECONDS);
  }

  private void handleFileEvent(WatchEvent.Kind<?> kind, File file, String fileName) {
    try {
      if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
        plugin.getLogger().info("New script detected: " + fileName);
        plugin.getScriptManager().loadScript(file);

      } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
        plugin.getLogger().info("Script modified: " + fileName);
        plugin.getScriptManager().unloadScript(fileName);
        plugin.getScriptManager().loadScript(file);

      } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
        plugin.getLogger().info("Script deleted: " + fileName);
        plugin.getScriptManager().unloadScript(fileName);
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error handling file event for: " + fileName, e);
    }
  }

  public boolean isRunning() {
    return running;
  }
}
