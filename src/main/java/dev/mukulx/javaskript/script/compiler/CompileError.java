package dev.mukulx.javaskript.script.compiler;

/**
 * One compilation error.
 *
 * @param line 1-based line in the compiled source
 * @param message what the compiler reported
 * @param sourceLine the offending source line, tabs expanded
 * @param marker a line of spaces and carets that lines up under the problem in {@code sourceLine}
 */
public record CompileError(int line, String message, String sourceLine, String marker) {}
