package dev.mukulx.javaskript.script.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns the Eclipse compiler's text report into structured errors and readable messages. */
public final class CompileDiagnostics {

  // ECJ prints each problem as:
  //   1. ERROR in /tmp/compile-123/Foo.java (at line 5)
  //   <source line>
  //   <marker line>
  //   <message, possibly several lines>
  //   ----------
  private static final Pattern ERROR_HEADER =
      Pattern.compile("^\\d+\\. ERROR in .*? \\(at line (\\d+)\\)$");
  private static final String SEPARATOR = "----------";
  private static final int MAX_LISTED = 5;

  private CompileDiagnostics() {}

  /** Errors found in the compiler's report. Warnings and the summary line are ignored. */
  public static List<CompileError> parse(String compilerOutput) {
    List<CompileError> errors = new ArrayList<>();
    if (compilerOutput == null) {
      return errors;
    }
    String[] lines = compilerOutput.split("\\R");
    for (int i = 0; i + 3 < lines.length; i++) {
      Matcher header = ERROR_HEADER.matcher(lines[i]);
      if (!header.matches()) {
        continue;
      }
      StringBuilder message = new StringBuilder();
      int next = i + 3;
      while (next < lines.length && !lines[next].equals(SEPARATOR)) {
        if (message.length() > 0) {
          message.append(' ');
        }
        message.append(lines[next].strip());
        next++;
      }
      errors.add(
          new CompileError(
              Integer.parseInt(header.group(1)),
              message.toString(),
              lines[i + 1].replace("\t", "  "),
              lines[i + 2].replace("\t", "  ")));
      i = next;
    }
    return errors;
  }

  /** A multi-line report with every error's location, message and source line for the console. */
  public static String format(String scriptName, List<CompileError> errors) {
    StringBuilder text = new StringBuilder();
    text.append("Compilation failed for ")
        .append(scriptName)
        .append(" (")
        .append(errors.size())
        .append(errors.size() == 1 ? " error" : " errors")
        .append("):");
    int listed = 0;
    for (CompileError error : errors) {
      if (listed++ == MAX_LISTED) {
        text.append("\n  ... and ").append(errors.size() - MAX_LISTED).append(" more");
        break;
      }
      text.append("\n  ")
          .append(scriptName)
          .append(':')
          .append(error.line())
          .append(": ")
          .append(error.message());
      text.append("\n    ").append(error.sourceLine());
      text.append("\n    ").append(error.marker());
    }
    return text.toString();
  }

  /** One short line for chat: the first error, plus how many more there are. */
  public static String summary(String scriptName, List<CompileError> errors) {
    if (errors.isEmpty()) {
      return "see console for details";
    }
    CompileError first = errors.get(0);
    String text = scriptName + ":" + first.line() + ": " + first.message();
    if (errors.size() > 1) {
      text += " (and " + (errors.size() - 1) + " more, see console)";
    }
    return text;
  }
}
