package dev.mukulx.javaskript.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScriptLoadOrderTest {

  private static ScriptLoadOrder.Entry entry(String key, String... after) {
    return new ScriptLoadOrder.Entry(new File(key), key, List.of(after));
  }

  private static List<String> keys(List<File> files) {
    return files.stream().map(File::getPath).toList();
  }

  @Test
  void parsesSingleAndMultipleNames() {
    assertEquals(
        List.of("Bank"), ScriptLoadOrder.parseLoadAfter("@LoadAfter(\"Bank\")\npublic class A {}"));
    assertEquals(
        List.of("Bank", "eco/Shop"),
        ScriptLoadOrder.parseLoadAfter("@LoadAfter({\"Bank\", \"eco/Shop\"})\npublic class A {}"));
    assertEquals(
        List.of("Bank"),
        ScriptLoadOrder.parseLoadAfter(
            "@FoliaSupport @LoadAfter(value = {\"Bank\"}) public class A {}"));
  }

  @Test
  void ignoresDeclarationsInCommentsAndStrings() {
    assertTrue(ScriptLoadOrder.parseLoadAfter("// @LoadAfter(\"Bank\")\nclass A {}").isEmpty());
    assertTrue(
        ScriptLoadOrder.parseLoadAfter("String s = \"@LoadAfter(\\\"Bank\\\")\";").isEmpty());
    assertTrue(ScriptLoadOrder.parseLoadAfter("public class A {}").isEmpty());
  }

  @Test
  void keepsFileOrderWhenNothingIsDeclared() {
    List<File> sorted =
        ScriptLoadOrder.sort(List.of(entry("C.java"), entry("A.java"), entry("B.java")), w -> {});
    assertEquals(List.of("C.java", "A.java", "B.java"), keys(sorted));
  }

  @Test
  void movesScriptAfterItsDependency() {
    List<File> sorted =
        ScriptLoadOrder.sort(
            List.of(entry("Shop.java", "Bank"), entry("Other.java"), entry("Bank.java")), w -> {});
    assertEquals(List.of("Other.java", "Bank.java", "Shop.java"), keys(sorted));
  }

  @Test
  void matchesByKeyOrClassNameIgnoringCaseAndExtension() {
    List<File> byKey =
        ScriptLoadOrder.sort(
            List.of(entry("a/Shop.java", "eco/bank.java"), entry("eco/Bank.java")), w -> {});
    assertEquals(List.of("eco/Bank.java", "a/Shop.java"), keys(byKey));

    List<File> byName =
        ScriptLoadOrder.sort(
            List.of(entry("a/Shop.java", "BANK"), entry("eco/Bank.java")), w -> {});
    assertEquals(List.of("eco/Bank.java", "a/Shop.java"), keys(byName));
  }

  @Test
  void followsChains() {
    List<File> sorted =
        ScriptLoadOrder.sort(
            List.of(entry("C.java", "B"), entry("B.java", "A"), entry("A.java")), w -> {});
    assertEquals(List.of("A.java", "B.java", "C.java"), keys(sorted));
  }

  @Test
  void unknownTargetIsReportedAndIgnored() {
    List<String> warnings = new ArrayList<>();
    List<File> sorted = ScriptLoadOrder.sort(List.of(entry("A.java", "Missing")), warnings::add);
    assertEquals(List.of("A.java"), keys(sorted));
    assertEquals(1, warnings.size());
    assertTrue(warnings.get(0).contains("Missing"));
  }

  @Test
  void cycleIsReportedAndLoadsInFileOrder() {
    List<String> warnings = new ArrayList<>();
    List<File> sorted =
        ScriptLoadOrder.sort(
            List.of(entry("A.java", "B"), entry("B.java", "A"), entry("C.java")), warnings::add);
    assertEquals(List.of("C.java", "A.java", "B.java"), keys(sorted));
    assertEquals(1, warnings.size());
    assertTrue(warnings.get(0).contains("Circular"));
  }

  @Test
  void selfReferenceIsIgnored() {
    List<File> sorted = ScriptLoadOrder.sort(List.of(entry("A.java", "A")), w -> {});
    assertEquals(List.of("A.java"), keys(sorted));
  }
}
