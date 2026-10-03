package dev.mukulx.javaskript.script.compiler;

import java.util.List;
import java.util.Map;

/**
 * Outcome of compiling one script.
 *
 * @param classes compiled bytecode by class name, or null when compilation failed
 * @param errors the compiler's errors; empty on success
 */
public record CompileResult(Map<String, byte[]> classes, List<CompileError> errors) {

  public boolean success() {
    return classes != null && !classes.isEmpty();
  }

  static CompileResult failure(List<CompileError> errors) {
    return new CompileResult(null, errors);
  }
}
