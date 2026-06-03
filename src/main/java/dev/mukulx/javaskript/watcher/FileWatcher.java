package dev.mukulx.javaskript.watcher;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/** Watches script files for changes and auto-reloads them */
public class FileWatcher implements Runnable {

  private final JavaSkriptPlugin plugin;
  private final File scriptsFolder;
  private WatchService watchService;
  private final Map<WatchKey, Path> watchKeys;
  private volatile boolean running = false;
  private Thread watchThread;
  private final long reloadDelay;

  public FileWatcher(JavaSkriptPlugin plugin, File scriptsFolder) {
    this.plugin = plugin;
    this.scriptsFolder = scriptsFolder;
    this.watchKeys = new HashMap<>();
    this.reloadDelay = plugin.getConfig().getLong("file-watcher.reload-delay", 500);
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

  /** Stop watching for file changes */
  public void stop() {
    running = false;

    // Interrupt the thread first
    if (watchThread != null && watchThread.isAlive()) {
      watchThread.interrupt();
    }

    // Then close the watch service
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

          // Handle the event - use direct call for Folia compatibility
          // File events are already async from the watch service thread
          handleFileEvent(kind, fullPath.toFile(), filename.toString());
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

  private void handleFileEvent(WatchEvent.Kind<?> kind, File file, String fileName) {
    try {
      if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
        plugin.getLogger().info("New script detected: " + fileName);
        // Wait for file to be fully written
        Thread.sleep(reloadDelay);
        plugin.getScriptManager().loadScript(file);

      } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
        plugin.getLogger().info("Script modified: " + fileName);
        // Wait for file to be fully written
        Thread.sleep(reloadDelay);

        // Fully unload the script first
        plugin.getScriptManager().unloadScript(fileName);

        // Give time for cleanup (listeners, commands, etc.) to complete
        // Increased delay for Folia command unregistration
        Thread.sleep(200);

        // Suggest GC to clean up old class loader and instances
        System.gc();

        // Small delay to let GC run
        Thread.sleep(100);

        // Now load the script fresh
        plugin.getScriptManager().loadScript(file);

      } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
        plugin.getLogger().info("Script deleted: " + fileName);
        plugin.getScriptManager().unloadScript(fileName);
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error handling file event for: " + fileName, e);
    }
  }

  /**
   * Check if the watcher is running
   *
   * @return true if running
   */
  public boolean isRunning() {
    return running;
  }
}
