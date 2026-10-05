package dev.mukulx.javaskript.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mukulx.javaskript.TestPlugin;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScriptManagerTest {

  // Scripts carry @Disabled so loading stops before compilation and no server is needed
  private static final String SCRIPT = "@Disabled\npublic class %s {}\n";

  @TempDir Path temp;
  private ScriptManager manager;
  private File scripts;

  @BeforeEach
  void setUp() {
    manager = new ScriptManager(TestPlugin.create(temp.toFile()));
    scripts = manager.getScriptsFolder();
  }

  private File script(String relativePath) throws Exception {
    File file = new File(scripts, relativePath);
    file.getParentFile().mkdirs();
    Files.writeString(file.toPath(), SCRIPT.formatted(file.getName().replace(".java", "")));
    return file;
  }

  @Test
  void scriptKeyDropsLeadingDashesOnEveryPathPart() throws Exception {
    File file = script("-folder/--Foo.java");
    assertEquals("folder/Foo.java", manager.getScriptKey(file));
  }

  @Test
  void dashDisabledByFileNameOrByParentFolder() throws Exception {
    assertTrue(manager.isDashDisabled(script("-A.java")));
    assertTrue(manager.isDashDisabled(script("-dir/B.java")));
    assertFalse(manager.isDashDisabled(script("C.java")));
  }

  @Test
  void disableRenamesFileAndEnableRenamesItBack() throws Exception {
    script("Foo.java");

    assertTrue(manager.disableScript("Foo"));
    assertFalse(new File(scripts, "Foo.java").exists());
    assertTrue(new File(scripts, "-Foo.java").exists());
    assertTrue(manager.isScriptDisabled("Foo"));

    assertTrue(manager.enableScript("Foo"));
    assertTrue(new File(scripts, "Foo.java").exists());
    assertFalse(manager.isScriptDisabled("Foo"));
  }

  @Test
  void enableInsideDashedFolderLeavesFileNameAlone() throws Exception {
    File file = script("-folder/Foo.java");

    assertFalse(manager.enableScript("folder/Foo"));

    assertTrue(file.exists(), "file must keep its name");
    assertFalse(new File(file.getParentFile(), "oo.java").exists());
  }

  @Test
  void unsafeKeysNeverResolveOutsideScriptsFolder() throws Exception {
    File outside = new File(temp.toFile(), "Outside.java");
    Files.writeString(outside.toPath(), SCRIPT.formatted("Outside"));

    assertFalse(manager.resolveScriptFile("../Outside").exists());
    assertFalse(manager.resolveScriptFile(outside.getAbsolutePath()).exists());
    assertFalse(manager.disableScript("../Outside"));
    assertFalse(manager.enableScript("../Outside"));
    assertTrue(outside.exists(), "file outside scripts/ must be untouched");
  }

  @Test
  void disabledScriptsAreListedAndKeysAreCleaned() throws Exception {
    script("-Off.java");
    script("On.java");

    assertTrue(manager.getDisabledScripts().contains("Off.java"));
    assertFalse(manager.getDisabledScripts().contains("On.java"));
    assertTrue(manager.getAllScriptKeys().containsAll(java.util.List.of("Off.java", "On.java")));
  }

  @Test
  void disablingAScriptThatDoesNotExistFails() {
    assertFalse(manager.disableScript("Missing"));
    assertFalse(new File(temp.toFile(), "disabled-scripts.json").exists());
  }

  @Test
  void disableAllRenamesEveryScript() throws Exception {
    script("A.java");
    script("sub/B.java");

    manager.disableAllScripts();

    assertTrue(new File(scripts, "-A.java").exists());
    assertTrue(new File(scripts, "sub/-B.java").exists());
    assertTrue(manager.isScriptDisabled("A"));
  }

  @Test
  void legacyDisabledListIsMigratedToDashPrefixes() throws Exception {
    Path dataFolder = temp.resolve("legacy");
    Path legacyScripts = dataFolder.resolve("scripts");
    Files.createDirectories(legacyScripts.resolve("sub"));
    Files.writeString(legacyScripts.resolve("Foo.java"), SCRIPT.formatted("Foo"));
    Files.writeString(legacyScripts.resolve("sub/Bar.java"), SCRIPT.formatted("Bar"));
    Files.writeString(
        dataFolder.resolve("disabled-scripts.json"),
        "[\"Foo.java\", \"sub/Bar.java\", \"Gone.java\"]");

    ScriptManager migrated = new ScriptManager(TestPlugin.create(dataFolder.toFile()));

    assertTrue(Files.exists(legacyScripts.resolve("-Foo.java")));
    assertTrue(Files.exists(legacyScripts.resolve("sub/-Bar.java")));
    assertFalse(Files.exists(dataFolder.resolve("disabled-scripts.json")));
    assertTrue(migrated.isScriptDisabled("Foo"));
  }
}
