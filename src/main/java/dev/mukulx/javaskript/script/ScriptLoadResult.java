package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.script.compiler.CompileError;
import java.util.List;

/**
 * Outcome of {@link ScriptManager#loadScriptAsync}.
 *
 * @param status what happened
 * @param errors the compiler's errors when {@code status} is {@link Status#FAILED} because the
 *     script did not compile; otherwise empty
 */
public record ScriptLoadResult(Status status, List<CompileError> errors) {

  public enum Status {
    /** The script compiled and is running. */
    LOADED,
    /** Compilation or startup failed. A previously running version is left untouched. */
    FAILED,
    /** The script is disabled, marked {@code @Disabled}, cancelled by an addon, or gone. */
    SKIPPED,
    /** The script was loaded, unloaded or disabled again while this one compiled. */
    SUPERSEDED
  }

  public boolean success() {
    return status == Status.LOADED;
  }

  static ScriptLoadResult loaded() {
    return new ScriptLoadResult(Status.LOADED, List.of());
  }

  static ScriptLoadResult failed(List<CompileError> errors) {
    return new ScriptLoadResult(Status.FAILED, errors);
  }

  static ScriptLoadResult skipped() {
    return new ScriptLoadResult(Status.SKIPPED, List.of());
  }

  static ScriptLoadResult superseded() {
    return new ScriptLoadResult(Status.SUPERSEDED, List.of());
  }
}
