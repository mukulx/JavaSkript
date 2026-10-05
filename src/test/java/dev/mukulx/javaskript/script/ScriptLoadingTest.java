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
import dev.mukulx.javaskript.script.ScriptLoadResult.Status;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
  // Stands in for the server thread: tasks queue here until a test runs them
  private final BlockingQueue<Runnable> serverTasks = new LinkedBlockingQueue<>();

  @BeforeEach
  void setUp() {
    JavaSkriptPlugin plugin = mock(JavaSkriptPlugin.class, RETURNS_DEEP_STUBS);
    when(plugin.getDataFolder()).thenReturn(temp.toFile());
    when(plugin.getLogger()).thenReturn(Logger.getLogger("JavaSkriptTest"));
    when(plugin.getServer().getPluginManager().getPlugins()).thenReturn(new Plugin[0]);
    when(plugin.getName()).thenReturn("JavaSkript");
    when(plugin.isEnabled()).thenReturn(true);
    manager = new ScriptManager(plugin, serverTasks::add);
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
  void packagedScriptNeverPicksANestedClassAsMain() throws Exception {
    File script =
        write(
            "Packaged.java",
            """
            package demo;
            public class Packaged {
              public static class Helper {}
              public static class Other {}
            }
            """);

    assertTrue(manager.loadScript(script));
    assertEquals("demo.Packaged", manager.getScript("Packaged").getScriptClass().getName());
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

  /** Wait for the background compile to hand back to the "server thread", then run that task. */
  private void runNextServerTask() throws Exception {
    Runnable task = serverTasks.poll(60, TimeUnit.SECONDS);
    assertNotNull(task, "background compile never finished");
    task.run();
  }

  @Test
  void asyncLoadCompilesInTheBackgroundThenActivatesOnTheServerThread() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);
    AtomicReference<ScriptLoadResult> result = new AtomicReference<>();

    manager.loadScriptAsync(script, result::set);

    assertNull(result.get(), "nothing is reported before the server thread finishes the load");
    assertNull(manager.getScript("Hello"), "nothing is activated before the server thread runs");
    runNextServerTask();
    assertEquals(Status.LOADED, result.get().status());
    assertEquals(1, field(manager.getScript("Hello"), "enabled"));
  }

  @Test
  void asyncBrokenEditReportsErrorsAndKeepsTheRunningVersion() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);
    assertTrue(manager.loadScript(script));
    ScriptInstance running = manager.getScript("Hello");
    Files.writeString(script.toPath(), "public class Hello {\n  int x = \"not an int\";\n}\n");
    AtomicReference<ScriptLoadResult> result = new AtomicReference<>();

    manager.loadScriptAsync(script, result::set);
    runNextServerTask();

    assertEquals(Status.FAILED, result.get().status());
    assertEquals(1, result.get().errors().size());
    assertEquals(2, result.get().errors().get(0).line());
    assertTrue(result.get().errors().get(0).message().contains("Type mismatch"));
    assertSame(running, manager.getScript("Hello"));
    assertEquals(0, field(running, "disabled"));
  }

  @Test
  void aNewerLoadSupersedesAnEarlierAsyncLoad() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);
    AtomicReference<ScriptLoadResult> first = new AtomicReference<>();
    AtomicReference<ScriptLoadResult> second = new AtomicReference<>();

    manager.loadScriptAsync(script, first::set);
    Files.writeString(script.toPath(), COUNTER_SCRIPT + "\n// second edit\n");
    manager.loadScriptAsync(script, second::set);
    runNextServerTask();
    runNextServerTask();

    assertEquals(Status.SUPERSEDED, first.get().status());
    assertEquals(Status.LOADED, second.get().status());
    assertEquals(1, field(manager.getScript("Hello"), "enabled"));
  }

  @Test
  void unloadingWhileCompilingDropsTheLoad() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);
    assertTrue(manager.loadScript(script));
    AtomicReference<ScriptLoadResult> result = new AtomicReference<>();

    manager.loadScriptAsync(script, result::set);
    manager.unloadScript("Hello");
    runNextServerTask();

    assertEquals(Status.SUPERSEDED, result.get().status());
    assertNull(manager.getScript("Hello"));
  }

  @Test
  void disablingWhileCompilingSkipsTheLoad() throws Exception {
    File script = write("Hello.java", COUNTER_SCRIPT);
    AtomicReference<ScriptLoadResult> result = new AtomicReference<>();

    manager.loadScriptAsync(script, result::set);
    assertTrue(manager.disableScript("Hello"));
    runNextServerTask();

    assertEquals(Status.SKIPPED, result.get().status());
    assertNull(manager.getScript("Hello"));
  }

  @Test
  void asyncLoadOfADisabledScriptIsSkippedImmediately() throws Exception {
    File script = write("Off.java", "@Disabled\npublic class Off {}\n");
    AtomicReference<ScriptLoadResult> result = new AtomicReference<>();

    manager.loadScriptAsync(script, result::set);

    assertEquals(Status.SKIPPED, result.get().status());
    assertTrue(serverTasks.isEmpty());
  }
}
