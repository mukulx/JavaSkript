package dev.mukulx.javaskript.script;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mukulx.javaskript.TestPlugin;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScriptContextTest {

  @TempDir Path temp;

  private ScriptContext context() {
    return new ScriptContext(TestPlugin.create(temp.toFile()), "Test.java");
  }

  @Test
  void closesInRegistrationOrder() {
    ScriptContext context = context();
    List<String> order = new ArrayList<>();
    context.own("a", () -> order.add("a"));
    context.own("b", () -> order.add("b"));
    context.own("c", () -> order.add("c"));

    context.close();

    assertEquals(List.of("a", "b", "c"), order);
  }

  @Test
  void aFailingCleanupDoesNotStopTheOthers() {
    ScriptContext context = context();
    List<String> order = new ArrayList<>();
    context.own(
        "broken",
        () -> {
          throw new IllegalStateException("boom");
        });
    context.own(
        "linkage",
        () -> {
          throw new NoClassDefFoundError("gone");
        });
    context.own("fine", () -> order.add("fine"));

    context.close();

    assertEquals(List.of("fine"), order);
  }

  @Test
  void closeRunsEachCleanupOnlyOnce() {
    ScriptContext context = context();
    List<String> order = new ArrayList<>();
    context.own("once", () -> order.add("once"));

    context.close();
    context.close();

    assertEquals(List.of("once"), order);
  }

  @Test
  void cleanupsAddedAfterCloseStillRunOnNextClose() {
    ScriptContext context = context();
    List<String> order = new ArrayList<>();
    context.close();
    context.own("late", () -> order.add("late"));

    context.close();

    assertEquals(List.of("late"), order);
  }
}
