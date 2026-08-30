package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.bukkit.OfflinePlayer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Easy PlaceholderAPI integration for scripts Allows scripts to register custom placeholders */
public class PlaceholderHelper {

  private static final Map<String, PlaceholderHelper> ACTIVE_HELPERS = new ConcurrentHashMap<>();

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private final Map<String, BiFunction<OfflinePlayer, String, String>> placeholders;
  private boolean papiAvailable = false;
  private Object expansion = null;

  public PlaceholderHelper(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName = scriptName.replace(".java", "");
    this.placeholders = new ConcurrentHashMap<>();
    ACTIVE_HELPERS.put(this.scriptName, this);

    // Check if PlaceholderAPI is available
    try {
      Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      papiAvailable = true;
    } catch (ClassNotFoundException e) {
      // PlaceholderAPI not installed
    }
  }

  public static String handlePlaceholder(String scriptName, OfflinePlayer player, String params) {
    PlaceholderHelper helper = ACTIVE_HELPERS.get(scriptName);
    if (helper != null) {
      return helper.getPlaceholder(params, player, params);
    }
    return null;
  }

  /**
   * Register a placeholder
   *
   * @param identifier The placeholder identifier (e.g., "myscript_value")
   * @param handler Function that returns the placeholder value
   * @return true if registered successfully
   */
  public boolean registerPlaceholder(
      String identifier, BiFunction<OfflinePlayer, String, String> handler) {
    if (!papiAvailable) {
      plugin
          .getLogger()
          .warning(
              "["
                  + scriptName
                  + "] PlaceholderAPI not found! Cannot register placeholder: "
                  + identifier);
      return false;
    }

    if (identifier == null || identifier.isEmpty() || handler == null) {
      return false;
    }

    placeholders.put(identifier.toLowerCase(), handler);

    // Register expansion if not already registered
    if (expansion == null) {
      registerExpansion();
    }

    plugin
        .getLogger()
        .info(
            "[" + scriptName + "] Registered placeholder: %" + scriptName + "_" + identifier + "%");
    return true;
  }

  /**
   * Register a simple placeholder (no player context)
   *
   * @param identifier The placeholder identifier
   * @param value The static value
   * @return true if registered successfully
   */
  public boolean registerPlaceholder(String identifier, String value) {
    return registerPlaceholder(identifier, (player, params) -> value);
  }

  /**
   * Unregister a placeholder
   *
   * @param identifier The placeholder identifier
   * @return true if unregistered successfully
   */
  public boolean unregisterPlaceholder(String identifier) {
    if (identifier == null || identifier.isEmpty()) {
      return false;
    }

    return placeholders.remove(identifier.toLowerCase()) != null;
  }

  /** Unregister all placeholders */
  public void unregisterAll() {
    placeholders.clear();
    ACTIVE_HELPERS.remove(scriptName);

    if (expansion != null && papiAvailable) {
      try {
        expansion.getClass().getMethod("unregister").invoke(expansion);
      } catch (Exception e) {
        plugin
            .getLogger()
            .log(
                Level.WARNING,
                "[" + scriptName + "] Failed to unregister PlaceholderAPI expansion",
                e);
      }
      expansion = null;
    }
  }

  /**
   * Get a placeholder value
   *
   * @param identifier The placeholder identifier
   * @param player The player
   * @param params Additional parameters
   * @return The placeholder value or null
   */
  public String getPlaceholder(String identifier, OfflinePlayer player, String params) {
    BiFunction<OfflinePlayer, String, String> handler = placeholders.get(identifier.toLowerCase());
    if (handler != null) {
      try {
        return handler.apply(player, params);
      } catch (Exception e) {
        plugin
            .getLogger()
            .log(
                Level.WARNING,
                "[" + scriptName + "] Error in placeholder handler: " + identifier,
                e);
      }
    }
    return null;
  }

  /**
   * Check if PlaceholderAPI is available
   *
   * @return true if available
   */
  public boolean isPlaceholderAPIAvailable() {
    return papiAvailable;
  }

  /**
   * Parse placeholders in a string (if PlaceholderAPI is available)
   *
   * @param player The player
   * @param text The text with placeholders
   * @return Parsed text
   */
  public String parsePlaceholders(OfflinePlayer player, String text) {
    if (!papiAvailable || text == null) {
      return text;
    }

    try {
      Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
      return (String)
          papiClass
              .getMethod("setPlaceholders", OfflinePlayer.class, String.class)
              .invoke(null, player, text);
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "[" + scriptName + "] Failed to parse placeholders", e);
      return text;
    }
  }

  private void registerExpansion() {
    try {
      Class<?> expansionClass = createDynamicExpansionClass();
      if (expansionClass == null) {
        return;
      }

      expansion = expansionClass.getDeclaredConstructor().newInstance();
      expansionClass.getMethod("register").invoke(expansion);
      plugin.debug("[" + scriptName + "] Registered PlaceholderExpansion successfully");
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "[" + scriptName + "] Failed to register PlaceholderAPI expansion", e);
    }
  }

  private Class<?> createDynamicExpansionClass() {
    try {
      String safeName = scriptName.replaceAll("[^a-zA-Z0-9_]", "_");
      String internalName =
          "dev/mukulx/javaskript/api/papi/DynamicExpansion_"
              + safeName
              + "_"
              + System.currentTimeMillis();

      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      cw.visit(
          Opcodes.V21,
          Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
          internalName,
          null,
          "me/clip/placeholderapi/expansion/PlaceholderExpansion",
          null);

      // Constructor
      {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "me/clip/placeholderapi/expansion/PlaceholderExpansion",
            "<init>",
            "()V",
            false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
      }

      // getIdentifier()
      {
        MethodVisitor mv =
            cw.visitMethod(Opcodes.ACC_PUBLIC, "getIdentifier", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(scriptName.toLowerCase());
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
      }

      // getAuthor()
      {
        MethodVisitor mv =
            cw.visitMethod(Opcodes.ACC_PUBLIC, "getAuthor", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn("JavaSkript");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
      }

      // getVersion()
      {
        MethodVisitor mv =
            cw.visitMethod(Opcodes.ACC_PUBLIC, "getVersion", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn("1.0.0");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
      }

      // persist()
      {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "persist", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
      }

      // canRegister()
      {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "canRegister", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
      }

      // onRequest(OfflinePlayer, String)
      {
        MethodVisitor mv =
            cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "onRequest",
                "(Lorg/bukkit/OfflinePlayer;Ljava/lang/String;)Ljava/lang/String;",
                null,
                null);
        mv.visitCode();
        mv.visitLdcInsn(scriptName);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "dev/mukulx/javaskript/api/PlaceholderHelper",
            "handlePlaceholder",
            "(Ljava/lang/String;Lorg/bukkit/OfflinePlayer;Ljava/lang/String;)Ljava/lang/String;",
            false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
      }

      // onPlaceholderRequest(Player, String)
      {
        MethodVisitor mv =
            cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "onPlaceholderRequest",
                "(Lorg/bukkit/entity/Player;Ljava/lang/String;)Ljava/lang/String;",
                null,
                null);
        mv.visitCode();
        mv.visitLdcInsn(scriptName);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "dev/mukulx/javaskript/api/PlaceholderHelper",
            "handlePlaceholder",
            "(Ljava/lang/String;Lorg/bukkit/OfflinePlayer;Ljava/lang/String;)Ljava/lang/String;",
            false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
      }

      cw.visitEnd();
      byte[] bytecode = cw.toByteArray();

      ClassLoader loader =
          new ClassLoader(plugin.getClass().getClassLoader()) {
            public Class<?> define(String name, byte[] b) {
              return defineClass(name, b, 0, b.length);
            }
          };

      return ((ByteArrayClassLoader)
              (Object) new ByteArrayClassLoader(plugin.getClass().getClassLoader()))
          .define(internalName.replace('/', '.'), bytecode);
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "[" + scriptName + "] Failed to generate expansion class", e);
      return null;
    }
  }

  private static class ByteArrayClassLoader extends ClassLoader {
    public ByteArrayClassLoader(ClassLoader parent) {
      super(parent);
    }

    public Class<?> define(String name, byte[] b) {
      return defineClass(name, b, 0, b.length);
    }
  }
}
