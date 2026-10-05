package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.*;
import java.io.File;
import java.lang.reflect.Field;

/**
 * Closes executors, pools and other resources left in script fields, for scripts without a full
 * onDisable().
 */
final class ScriptResourceCleanup {

  private ScriptResourceCleanup() {}

  /**
   * Automatically clean up common custom resources by scanning instance fields This helps scripts
   * that don't implement onDisable() but use resources like HikariCP, ExecutorService, etc.
   */
  static void run(JavaSkriptPlugin plugin, File scriptFile, Class<?> scriptClass, Object instance) {
    if (instance == null) {
      return;
    }

    String scriptName = scriptFile.getName();
    int cleanedCount = 0;

    try {
      // Walk the class hierarchy (matches injectAPIs) so inherited resources also clean up.
      Class<?> current = scriptClass;
      while (current != null && current != Object.class) {
        for (Field field : current.getDeclaredFields()) {
          try {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
              continue;
            }
            field.setAccessible(true);
            Object value = field.get(instance);

            if (value == null) {
              continue;
            }

            // Skip framework-owned helpers: they are cleaned explicitly above, and some
            // (economy, variables, http, teams) are shared across scripts — closing them
            // here would break every other script.
            Class<?> valueType = value.getClass();
            String typeName = valueType.getName();
            if (typeName.startsWith("dev.mukulx.javaskript.")
                || valueType == JavaSkriptPlugin.class) {
              continue;
            }

            // The shared HttpClient from the HTTP helper is AutoCloseable on Java 21. Closing it
            // here would break HTTP calls in every other script.
            if (value instanceof java.net.http.HttpClient) {
              continue;
            }

            // Check for HikariDataSource (HikariCP)
            if (typeName.equals("com.zaxxer.hikari.HikariDataSource")) {
              try {
                // Check if already closed
                var isClosedMethod = value.getClass().getMethod("isClosed");
                boolean isClosed = (boolean) isClosedMethod.invoke(value);

                if (!isClosed) {
                  var closeMethod = value.getClass().getMethod("close");
                  closeMethod.invoke(value);
                  plugin
                      .getLogger()
                      .info(
                          "Auto-closed HikariDataSource in field '"
                              + field.getName()
                              + "' for: "
                              + scriptName);
                  cleanedCount++;
                }
              } catch (Exception e) {
                // Ignore - might already be closed
              }
            }

            // Check for ExecutorService
            if (value instanceof java.util.concurrent.ExecutorService) {
              try {
                java.util.concurrent.ExecutorService executor =
                    (java.util.concurrent.ExecutorService) value;
                if (!executor.isShutdown()) {
                  executor.shutdown();
                  try {
                    if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                      executor.shutdownNow();
                    }
                  } catch (InterruptedException ie) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                  }
                  plugin
                      .getLogger()
                      .info(
                          "Auto-shutdown ExecutorService in field '"
                              + field.getName()
                              + "' for: "
                              + scriptName);
                  cleanedCount++;
                }
              } catch (Exception e) {
                // Ignore
              }
            }

            // Check for Thread
            if (value instanceof Thread) {
              try {
                Thread thread = (Thread) value;
                if (thread.isAlive() && !isServerThread(thread)) {
                  thread.interrupt();
                  plugin
                      .getLogger()
                      .info(
                          "Auto-interrupted Thread in field '"
                              + field.getName()
                              + "' for: "
                              + scriptName);
                  cleanedCount++;
                }
              } catch (Exception e) {
                // Ignore
              }
            }

            // Check for Closeable/AutoCloseable (but skip HikariDataSource since handled above)
            if (value instanceof AutoCloseable
                && !typeName.equals("com.zaxxer.hikari.HikariDataSource")) {
              try {
                ((AutoCloseable) value).close();
                plugin
                    .getLogger()
                    .info(
                        "Auto-closed "
                            + value.getClass().getSimpleName()
                            + " in field '"
                            + field.getName()
                            + "' for: "
                            + scriptName);
                cleanedCount++;
              } catch (Exception e) {
                // Ignore - might already be closed
              }
            }
          } catch (Exception e) {
            // Ignore individual field errors
          }
        }
        current = current.getSuperclass();
      }

      if (cleanedCount > 0) {
        plugin
            .getLogger()
            .info("Auto-cleaned " + cleanedCount + " custom resource(s) for: " + scriptName);
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning("Error scanning for custom resources (continuing): " + e.getMessage());
    }
  }

  /** Threads the server or scheduler owns. A script holding one in a field must not stop it. */
  private static boolean isServerThread(Thread thread) {
    if (thread == Thread.currentThread()) {
      return true;
    }
    String name = thread.getName();
    return name.startsWith("Server thread")
        || name.startsWith("Region Scheduler Thread")
        || name.startsWith("Craft Scheduler Thread")
        || name.startsWith("Folia")
        || name.startsWith("Paper");
  }
}
