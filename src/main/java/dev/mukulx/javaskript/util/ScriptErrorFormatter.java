package dev.mukulx.javaskript.util;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Smart error diagnostic and stack trace formatter for JavaSkript.
 *
 * <p>Isolates the exact script file and line number causing an exception and filters out internal
 * JVM, Paper, and Folia reflection frames.
 *
 * <p>Can be toggled via `errors.clean-stack-traces` in config.yml.
 */
public final class ScriptErrorFormatter {

  private ScriptErrorFormatter() {}

  /**
   * Format and log an unhandled script error to console.
   *
   * @param plugin The JavaSkriptPlugin instance
   * @param scriptKey The script key (e.g. "pvp/CombatLog.java")
   * @param context Context description (e.g. "event handler 'onDamage'", "scheduled task
   *     'runLater'")
   * @param throwable The caught exception
   */
  public static void log(
      JavaSkriptPlugin plugin, String scriptKey, String context, Throwable throwable) {
    if (throwable == null) return;

    // Check if clean stack traces are enabled in config (default true)
    boolean cleanEnabled =
        plugin != null
            && plugin.getConfig().getBoolean("general.clean-stack-traces", true)
            && !plugin.isDebugMode();

    if (!cleanEnabled) {
      // Print standard full Java stack trace
      if (plugin != null) {
        plugin
            .getLogger()
            .log(
                Level.SEVERE,
                "[" + scriptKey + "] Unhandled error in " + context + ": " + throwable.getMessage(),
                throwable);
      } else {
        throwable.printStackTrace();
      }
      return;
    }

    // Unwrap InvocationTargetException / ExecutionException to find true root cause
    Throwable root = unwrap(throwable);

    // Extract script file basename for frame matching
    String cleanScriptKey = scriptKey != null ? scriptKey : "script";
    String fileName = cleanScriptKey;
    if (fileName.contains("/")) {
      fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
    }
    if (!fileName.endsWith(".java")) {
      fileName = fileName + ".java";
    }

    // Filter relevant frames
    StackTraceElement[] frames = root.getStackTrace();
    List<StackTraceElement> scriptFrames = new ArrayList<>();

    StackTraceElement primaryErrorFrame = null;

    if (frames != null) {
      for (StackTraceElement frame : frames) {
        String frameFile = frame.getFileName();
        String className = frame.getClassName();

        // Check if frame originates from this script or another loaded script
        if ((frameFile != null && frameFile.equalsIgnoreCase(fileName))
            || className.startsWith("dev.mukulx.javaskript.scripts.")
            || (frameFile != null
                && frameFile.endsWith(".java")
                && !className.startsWith("dev.mukulx.javaskript.")
                && !className.startsWith("org.bukkit.")
                && !className.startsWith("io.papermc.")
                && !className.startsWith("net.minecraft."))) {
          if (primaryErrorFrame == null) {
            primaryErrorFrame = frame;
          }
          scriptFrames.add(frame);
        }
      }
    }

    // Build clean formatted error output
    StringBuilder sb = new StringBuilder();
    sb.append("\n");

    if (primaryErrorFrame != null) {
      sb.append("  [Script Error] ")
          .append(cleanScriptKey)
          .append(":")
          .append(primaryErrorFrame.getLineNumber())
          .append(" (in ")
          .append(primaryErrorFrame.getMethodName())
          .append(")\n");
    } else {
      sb.append("  [Script Error] in ").append(cleanScriptKey);
      if (context != null && !context.isEmpty()) {
        sb.append(" (during ").append(context).append(")");
      }
      sb.append("\n");
    }

    // Cause line
    String exceptionName = root.getClass().getSimpleName();
    String message = root.getMessage();
    sb.append("     ↳ ").append(exceptionName);
    if (message != null && !message.isEmpty()) {
      sb.append(": ").append(message);
    }
    sb.append("\n");

    // Script call chain if multiple script frames exist
    if (scriptFrames.size() > 1) {
      sb.append("     ↳ Script Call Chain:\n");
      for (int i = 0; i < Math.min(scriptFrames.size(), 5); i++) {
        StackTraceElement f = scriptFrames.get(i);
        sb.append("        • at ")
            .append(f.getFileName() != null ? f.getFileName() : f.getClassName())
            .append(":")
            .append(f.getLineNumber())
            .append(" (")
            .append(f.getMethodName())
            .append(")\n");
      }
    } else if (primaryErrorFrame == null && frames != null && frames.length > 0) {
      // If we couldn't match a script frame, show top 3 non-internal frames so info is never lost
      sb.append("     ↳ Top Frames:\n");
      int shown = 0;
      for (StackTraceElement f : frames) {
        String cn = f.getClassName();
        if (isInternalFrame(cn)) continue;
        sb.append("        • at ")
            .append(f.getClassName())
            .append(".")
            .append(f.getMethodName())
            .append("(")
            .append(f.getFileName())
            .append(":")
            .append(f.getLineNumber())
            .append(")\n");
        shown++;
        if (shown >= 3) break;
      }
      if (shown == 0) {
        sb.append("        • at ").append(frames[0]).append("\n");
      }
    }

    if (plugin != null) {
      plugin.getLogger().severe(sb.toString());
    } else {
      System.err.println(sb.toString());
    }
  }

  private static Throwable unwrap(Throwable t) {
    Throwable curr = t;
    while (curr.getCause() != null
        && (curr instanceof InvocationTargetException
            || curr.getClass().getName().contains("ExecutionException"))) {
      curr = curr.getCause();
    }
    return curr;
  }

  private static boolean isInternalFrame(String className) {
    return className.startsWith("jdk.internal.")
        || className.startsWith("java.lang.reflect.")
        || className.startsWith("io.papermc.paper.threadedregions.")
        || className.startsWith("io.papermc.paper.plugin.")
        || className.startsWith("org.bukkit.plugin.SimplePluginManager")
        || className.startsWith("co.aikar.timings.");
  }
}
