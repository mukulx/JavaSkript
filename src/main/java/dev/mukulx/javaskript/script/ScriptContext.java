package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns everything one script registers with the server and releases it in a single place.
 *
 * <p>Anything that must not outlive the script (tasks, listeners, commands, database connections
 * and so on) is handed to {@link #own(String, Runnable)} when it is created. {@link #close()} then
 * runs every cleanup in registration order. A failing cleanup is logged and never stops the others,
 * so one broken subsystem cannot leave the rest of the script registered.
 */
final class ScriptContext {

  private record Resource(String description, Runnable cleanup) {}

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private final List<Resource> resources = new ArrayList<>();

  ScriptContext(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName = scriptName;
  }

  /** Register a cleanup to run when the script unloads. */
  synchronized void own(String description, Runnable cleanup) {
    resources.add(new Resource(description, cleanup));
  }

  /** Run every registered cleanup once, in registration order, then forget them. */
  void close() {
    List<Resource> toClose;
    synchronized (this) {
      toClose = new ArrayList<>(resources);
      resources.clear();
    }
    for (Resource resource : toClose) {
      try {
        resource.cleanup().run();
        plugin.debug("Released " + resource.description() + " for: " + scriptName);
      } catch (Exception | LinkageError e) {
        plugin
            .getLogger()
            .warning(
                "Error releasing "
                    + resource.description()
                    + " for "
                    + scriptName
                    + " (continuing): "
                    + e.getMessage());
      }
    }
  }
}
