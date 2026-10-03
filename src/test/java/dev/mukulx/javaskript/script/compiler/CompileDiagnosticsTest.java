package dev.mukulx.javaskript.script.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompileDiagnosticsTest {

  @TempDir Path temp;

  private String compilerReport(String source) throws Exception {
    Path file = temp.resolve("Foo.java");
    Files.writeString(file, source);
    StringWriter report = new StringWriter();
    BatchCompiler.compile(
        new String[] {"-21", "-nowarn", "-d", "none", file.toString()},
        new PrintWriter(new StringWriter()),
        new PrintWriter(report),
        null);
    return report.toString();
  }

  @Test
  void parsesLineMessageAndSourceFromARealCompilerReport() throws Exception {
    String report =
        compilerReport(
            "public class Foo {\n  void a() {\n    int x = \"text\";\n    missing();\n  }\n}\n");

    List<CompileError> errors = CompileDiagnostics.parse(report);

    assertEquals(2, errors.size());
    assertEquals(3, errors.get(0).line());
    assertTrue(errors.get(0).message().contains("Type mismatch"), errors.get(0).message());
    assertTrue(errors.get(0).sourceLine().contains("int x = \"text\";"));
    assertTrue(errors.get(0).marker().contains("^"));
    assertEquals(4, errors.get(1).line());
    assertTrue(errors.get(1).message().contains("missing"), errors.get(1).message());
  }

  @Test
  void markerStaysAlignedWithTheSourceLineAfterTabExpansion() throws Exception {
    String report = compilerReport("public class Foo {\n\tint x = \"text\";\n}\n");

    CompileError error = CompileDiagnostics.parse(report).get(0);

    int caret = error.marker().indexOf('^');
    assertEquals("\"text\"", error.sourceLine().substring(caret, caret + 6));
  }

  @Test
  void joinsMultiLineMessages() {
    String report =
        "----------\n"
            + "1. ERROR in /tmp/Foo.java (at line 7)\n"
            + "\tfoo();\n"
            + "\t^^^\n"
            + "The method foo() is undefined\n"
            + "for the type Foo\n"
            + "----------\n"
            + "1 problem (1 error)\n";

    List<CompileError> errors = CompileDiagnostics.parse(report);

    assertEquals(1, errors.size());
    assertEquals("The method foo() is undefined for the type Foo", errors.get(0).message());
  }

  @Test
  void ignoresReportsWithNoErrors() {
    assertTrue(CompileDiagnostics.parse("").isEmpty());
    assertTrue(CompileDiagnostics.parse(null).isEmpty());
    assertTrue(CompileDiagnostics.parse("something unexpected").isEmpty());
  }

  @Test
  void formatListsScriptNameLineAndSnippet() {
    String text =
        CompileDiagnostics.format(
            "Foo.java",
            List.of(new CompileError(3, "Type mismatch", "int x = \"a\";", "        ^^^")));

    assertTrue(text.startsWith("Compilation failed for Foo.java (1 error):"));
    assertTrue(text.contains("Foo.java:3: Type mismatch"));
    assertTrue(text.contains("int x = \"a\";"));
  }

  @Test
  void formatCapsHowManyErrorsAreListed() {
    List<CompileError> many =
        java.util.stream.IntStream.rangeClosed(1, 8)
            .mapToObj(i -> new CompileError(i, "bad " + i, "src", "^"))
            .toList();

    String text = CompileDiagnostics.format("Foo.java", many);

    assertTrue(text.contains("(8 errors)"));
    assertTrue(text.contains("bad 5"));
    assertFalse(text.contains("bad 6"));
    assertTrue(text.contains("and 3 more"));
  }

  @Test
  void summaryNamesTheFirstErrorAndCountsTheRest() {
    List<CompileError> errors =
        List.of(new CompileError(3, "first", "s", "^"), new CompileError(9, "second", "s", "^"));

    assertEquals(
        "Foo.java:3: first (and 1 more, see console)",
        CompileDiagnostics.summary("Foo.java", errors));
    assertEquals("Foo.java:3: first", CompileDiagnostics.summary("Foo.java", errors.subList(0, 1)));
    assertEquals("see console for details", CompileDiagnostics.summary("Foo.java", List.of()));
  }
}
