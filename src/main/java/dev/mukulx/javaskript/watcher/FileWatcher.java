package dev.mukulx.javaskript.watcher;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
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
    this.watchKeys = new ConcurrentHashMap<>();
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

      // Register the scripts folder and all subdirectories recursively
      registerTree(scriptsFolder.toPath());

      running = true;
      watchThread = new Thread(this, "JavaSkript-FileWatcher");
      watchThread.setDaemon(true);
      watchThread.start();

    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to start file watcher", e);
    }
  }

  private void registerTree(Path start) throws IOException {
    if (!Files.exists(start)) {
      return;
    }
    Files.walkFileTree(
        start,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(
              Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
            registerDir(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private void registerDir(Path dir) throws IOException {
    WatchKey key =
        dir.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);
    watchKeys.put(key, dir);
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

          // If a new subdirectory is created, register it for watching
          if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
            try {
              registerTree(fullPath);
              plugin.debug("Registered new subfolder in watcher: " + fullPath);
            } catch (IOException e) {
              plugin.getLogger().warning("Failed to watch new folder: " + fullPath);
            }
            continue;
          }

          // Only process .java files
          String fileNameStr = filename.toString();
          if (!fileNameStr.endsWith(".java")) {
            continue;
          }

          scheduleReload(kind, fullPath.toFile());
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

  private void scheduleReload(WatchEvent.Kind<?> kind, File file) {
    // Use the script key (relative path) for dedup
    String scriptKey = plugin.getScriptManager().getScriptKey(file);
    String debounceKey = scriptKey + ":" + kind.name();
    pendingReloads.put(debounceKey, System.currentTimeMillis());

    debounceExecutor.schedule(
        () -> {
          Long scheduledTime = pendingReloads.get(debounceKey);
          if (scheduledTime != null
              && System.currentTimeMillis() - scheduledTime >= reloadDelay - 50) {
            pendingReloads.remove(debounceKey);
            handleFileEvent(kind, file, scriptKey);
          }
        },
        reloadDelay,
        TimeUnit.MILLISECONDS);
  }

  private void handleFileEvent(WatchEvent.Kind<?> kind, File file, String scriptKey) {
    // WatchService and the debounce executor are not server threads. Script lifecycle operations
    // register Bukkit state, so always hand them back to the appropriate server scheduler.
    Runnable operation = () -> handleFileEventOnServerThread(kind, file, scriptKey);
    if (dev.mukulx.javaskript.util.ServerUtil.isFolia()) {
      plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> operation.run());
    } else {
      plugin.getServer().getScheduler().runTask(plugin, operation);
    }
  }

  private void handleFileEventOnServerThread(WatchEvent.Kind<?> kind, File file, String scriptKey) {
    try {
      if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
        plugin.getLogger().info("New script detected: " + scriptKey);
        plugin.getScriptManager().loadScript(file);

      } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
        plugin.getLogger().info("Script modified: " + scriptKey);
        plugin.getScriptManager().unloadScript(scriptKey);
        plugin.getScriptManager().loadScript(file);

      } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
        plugin.getLogger().info("Script deleted: " + scriptKey);
        plugin.getScriptManager().unloadScript(scriptKey);
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error handling file event for: " + scriptKey, e);
    }
  }

  public boolean isRunning() {
    return running;
  }
}
