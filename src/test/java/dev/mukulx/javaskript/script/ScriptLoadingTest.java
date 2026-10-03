package dev.mukulx.javaskript.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the real pipeline: ECJ compile, class loading, instance creation and unload. */
class ScriptLoadingTest {

  private static final String COUNTER_SCRIPT =
      """
      public class Hello {
        public static int enabled;
        public static int disabled;
        public void onEnable() { enabled++; }
        public void onDisable() { disabled++; }
      }
      """;

  @TempDir Path temp;
  private ScriptManager manager;

  @BeforeEach
  void setUp() {
    JavaSkriptPlugin plugin = mock(JavaSkriptPlugin.class, RETURNS_DEEP_STUBS);
    when(plugin.getDataFolder()).thenReturn(temp.toFile());
    when(plugin.getLogger()).thenReturn(Logger.getLogger("JavaSkriptTest"));
    when(plugin.getServer().getPluginManager().getPlugins()).thenReturn(new Plugin[0]);
    when(plugin.getName()).thenReturn("JavaSkript");
    when(plugin.isEnabled()).thenReturn(true);
    manager = new ScriptManager(plugin);
    when(plugin.getScriptManager()).thenReturn(manager);
  }

  private File write(String name, String source) throws Exception {
    File file = new File(manager.getScriptsFolder(), name);
    file.getParentFile().mkdirs();
    Files.writeString(file.toPath(), source);
    return file;
  }

  private static int field(ScriptInstance instance, String name) throws Exception {
    return instance.getScriptClass().getField(name).getInt(null);
  }

  @Test
  void loadRunsOnEnableAndUnloadRunsOnDisable() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);

    assertTrue(manager.loadScript(script));

    ScriptInstance instance = manager.getScript("Hello");
    assertNotNull(instance);
    assertEquals(1, field(instance, "enabled"));

    assertTrue(manager.unloadScript("Hello"));
    assertEquals(1, field(instance, "disabled"));
    assertNull(manager.getScript("Hello"));
  }

  @Test
  void reloadReplacesTheRunningInstance() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);
    assertTrue(manager.loadScript(script));
    ScriptInstance first = manager.getScript("Hello");

    Files.writeString(script.toPath(), COUNTER_SCRIPT + "\n// edited\n");
    assertTrue(manager.loadScript(script));

    ScriptInstance second = manager.getScript("Hello");
    assertNotSame(first, second);
    assertEquals(1, field(first, "disabled"), "old instance must be disabled on reload");
    assertEquals(1, field(second, "enabled"));
  }

  @Test
  void reloadOfUnchangedFileStillReinitializes() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);
    assertTrue(manager.loadScript(script));
    ScriptInstance first = manager.getScript("Hello");

    assertTrue(manager.loadScript(script));

    assertNotSame(first, manager.getScript("Hello"));
    assertEquals(1, field(manager.getScript("Hello"), "enabled"));
  }

  @Test
  void brokenEditKeepsTheRunningScript() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);
    assertTrue(manager.loadScript(script));
    ScriptInstance running = manager.getScript("Hello");

    Files.writeString(script.toPath(), "public class Hello { int x = \"not an int\"; }");

    assertFalse(manager.loadScript(script));
    assertSame(running, manager.getScript("Hello"));
    assertEquals(0, field(running, "disabled"), "running script must not have been unloaded");
  }

  @Test
  void multipleClassesInOneFileLoad() throws Exception {
    File script =
        write(
            "Multi.java",
            """
            public class Multi extends Base { public static int ready = 1; }
            class Base {}
            """);

    assertTrue(manager.loadScript(script));
    assertNotNull(manager.getScript("Multi"));
  }

  @Test
  void disabledMarkerSkipsTheScript() throws Exception {
    File script = write("Off.java", "@Disabled\npublic class Off {}\n");

    assertFalse(manager.loadScript(script));
    assertNull(manager.getScript("Off"));
  }

  @Test
  void loadAllLoadsEveryEnabledScript() throws Exception {
    write("A.java", "public class A {}");
    write("sub/B.java", "public class B {}");
    write("-C.java", "public class C {}");

    manager.loadAllScripts();

    assertEquals(2, manager.getLoadedScripts().size());
    assertNotNull(manager.getScript("A"));
    assertNotNull(manager.getScript("sub/B"));
  }
}
