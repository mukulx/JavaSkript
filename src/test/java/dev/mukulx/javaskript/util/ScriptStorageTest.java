package dev.mukulx.javaskript.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ScriptStorageTest {

  @Test
  void preservesLegacyNamesWithoutDirectories() {
    assertEquals("Welcome", ScriptStorage.id("Welcome.java"));
  }

  @Test
  void distinguishesSameClassNamesInDifferentDirectories() {
    assertNotEquals(ScriptStorage.id("admin/Main.java"), ScriptStorage.id("events/Main.java"));
  }

  @Test
  void rejectsTraversal() {
    assertThrows(IllegalArgumentException.class, () -> ScriptStorage.id("../outside.java"));
  }
}
