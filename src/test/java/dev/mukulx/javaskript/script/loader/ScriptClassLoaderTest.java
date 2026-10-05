package dev.mukulx.javaskript.script.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mukulx.javaskript.TestPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScriptClassLoaderTest {

  @TempDir Path temp;

  private Map<String, byte[]> compile(String source, String... classNamesInDefineOrder)
      throws Exception {
    Path src = temp.resolve("Main.java");
    Files.writeString(src, source);
    int status =
        ToolProvider.getSystemJavaCompiler()
            .run(null, null, null, "-d", temp.toString(), src.toString());
    assertEquals(0, status);
    Map<String, byte[]> classes = new LinkedHashMap<>();
    for (String name : classNamesInDefineOrder) {
      classes.put(name, Files.readAllBytes(temp.resolve(name + ".class")));
    }
    return classes;
  }

  @Test
  void definesSubclassBeforeItsSuperclass() throws Exception {
    Map<String, byte[]> classes =
        compile("public class Main extends Base {} class Base {}", "Main", "Base");
    ScriptClassLoader loader = new ScriptClassLoader(TestPlugin.create(temp.toFile()), "test");

    Map<String, Class<?>> defined = loader.defineClasses(classes);

    assertEquals(2, defined.size());
    assertEquals("Base", defined.get("Main").getSuperclass().getName());
  }

  @Test
  void definesClassBeforeInterfaceItImplements() throws Exception {
    Map<String, byte[]> classes =
        compile("public class Main implements Api {} interface Api {}", "Main", "Api");
    ScriptClassLoader loader = new ScriptClassLoader(TestPlugin.create(temp.toFile()), "test");

    Map<String, Class<?>> defined = loader.defineClasses(classes);

    assertEquals(2, defined.size());
    assertEquals("Api", defined.get("Main").getInterfaces()[0].getName());
  }
}
