package dev.mukulx.javaskript.api.resourcepack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Builds a resource pack in memory and registers it. Combine any of the sources:
 *
 * <pre>{@code
 * ResourcePack pack = resourcePacks.create("main")
 *     .folder("weapons")                                  // plugins/JavaSkript/resourcepacks/weapons
 *     .itemModel("ruby_sword", Path.of("ruby_sword.png")) // flat 2D item
 *     .itemModel3d("hammer", Path.of("hammer.json"))      // Blockbench model
 *     .texture("item/hammer", Path.of("hammer.png"))      // texture the 3D model uses
 *     .sound("boom", Path.of("boom.ogg"))
 *     .register();
 *
 * ItemStack sword = pack.applyModel(new ItemStack(Material.DIAMOND_SWORD), "ruby_sword");
 * }</pre>
 *
 * <p>Relative paths resolve against {@code plugins/JavaSkript/resourcepacks/}. Files added later
 * replace earlier ones at the same path. A {@code pack.mcmeta} is generated unless one is added.
 */
public final class ResourcePackBuilder {

  /** Pack format of Minecraft 1.21.11. */
  public static final int DEFAULT_PACK_FORMAT = 75;

  private static final Pattern NAME = Pattern.compile("[a-z0-9_.-]+");
  private static final Pattern RESOURCE_PATH = Pattern.compile("[a-z0-9_.-]+(/[a-z0-9_.-]+)*");
  private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
  private static final byte[] OGG_SIGNATURE = {'O', 'g', 'g', 'S'};
  private static final FileTime ZIP_TIME = FileTime.fromMillis(315532800000L); // 1980-01-01
  private static final Gson GSON = new Gson();

  private final ResourcePackManager manager;
  private final String id;
  private String namespace;
  private boolean namespaceUsed;
  private final Map<String, byte[]> files = new TreeMap<>();
  private final Map<String, String> sounds = new LinkedHashMap<>();
  private String description;
  private int minFormat = DEFAULT_PACK_FORMAT;
  private int maxFormat = DEFAULT_PACK_FORMAT;
  private int priority;
  private Boolean required;
  private Consumer<Player> onLoaded;
  private BiConsumer<Player, PlayerResourcePackStatusEvent.Status> onFailed;

  ResourcePackBuilder(ResourcePackManager manager, String owner, String name) {
    this.manager = manager;
    this.id = packId(owner, name);
    this.namespace = toNamespace(owner);
    this.description = "JavaSkript: " + id;
  }

  static String packId(String owner, String name) {
    if (name == null || !NAME.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "Invalid pack name '" + name + "': use lower-case letters, digits, '_', '-' or '.'");
    }
    return owner + ":" + name;
  }

  /** A valid namespace from any owner name: {@code PvP/Arena} becomes {@code pvp_arena}. */
  static String toNamespace(String owner) {
    String namespace = owner.toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
    return namespace.isEmpty() ? "javaskript" : namespace;
  }

  /** Text shown for the pack in the client's pack list. */
  public ResourcePackBuilder description(String description) {
    this.description = Objects.requireNonNull(description, "description cannot be null");
    return this;
  }

  /**
   * Namespace for {@link #texture}, {@link #itemModel} and {@link #sound}. Defaults to the owning
   * script's name. Two packs using the same namespace and file names override each other. Set it
   * before adding textures, models or sounds.
   */
  public ResourcePackBuilder namespace(String namespace) {
    if (namespaceUsed) {
      throw new IllegalStateException(
          "Set the namespace before adding textures, item models or sounds");
    }
    if (namespace == null || !NAME.matcher(namespace).matches()) {
      throw new IllegalArgumentException(
          "Invalid namespace '" + namespace + "': use lower-case letters, digits, '_', '-' or '.'");
    }
    this.namespace = namespace;
    return this;
  }

  /** Higher priority packs are applied on top and win when two packs change the same file. */
  public ResourcePackBuilder priority(int priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Whether players must accept the pack. Defaults to {@code required} in config.yml. A player who
   * declines a required pack, or fails to download it, is disconnected.
   */
  public ResourcePackBuilder required(boolean required) {
    this.required = required;
    return this;
  }

  /**
   * Pack format range written to the generated {@code pack.mcmeta}. Defaults to {@value
   * #DEFAULT_PACK_FORMAT} (1.21.11). Raise {@code max} to mark the pack as compatible with newer
   * clients.
   */
  public ResourcePackBuilder packFormat(int min, int max) {
    if (min <= 0 || max < min) {
      throw new IllegalArgumentException("Invalid pack format range " + min + ".." + max);
    }
    this.minFormat = min;
    this.maxFormat = max;
    return this;
  }

  /** Run when a player has downloaded and applied the pack. Runs on that player's thread. */
  public ResourcePackBuilder onLoaded(Consumer<Player> callback) {
    this.onLoaded = callback;
    return this;
  }

  /**
   * Run when a player declines the pack or it fails to download or apply. Runs on that player's
   * thread.
   */
  public ResourcePackBuilder onFailed(
      BiConsumer<Player, PlayerResourcePackStatusEvent.Status> callback) {
    this.onFailed = callback;
    return this;
  }

  /** Add a file at a path inside the pack, e.g. {@code assets/minecraft/lang/en_us.json}. */
  public ResourcePackBuilder file(String path, byte[] content) {
    files.put(checkPackPath(path), Objects.requireNonNull(content, "content cannot be null"));
    return this;
  }

  /** Add a text file (UTF-8) at a path inside the pack. */
  public ResourcePackBuilder file(String path, String content) {
    return file(
        path,
        Objects.requireNonNull(content, "content cannot be null").getBytes(StandardCharsets.UTF_8));
  }

  /** Add a file from disk at a path inside the pack. */
  public ResourcePackBuilder file(String path, Path source) {
    return file(path, read(source));
  }

  /**
   * Add every file of an unpacked pack folder, keeping their paths. The folder usually holds {@code
   * assets/} and optionally {@code pack.mcmeta}.
   */
  public ResourcePackBuilder folder(String folder) {
    return folder(Path.of(folder));
  }

  /** Add every file of an unpacked pack folder, keeping their paths. */
  public ResourcePackBuilder folder(Path folder) {
    Path root = manager.resolve(folder);
    if (!Files.isDirectory(root)) {
      throw new IllegalArgumentException("Resource pack folder not found: " + root);
    }
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .forEach(
              path -> {
                String relative = root.relativize(path).toString().replace('\\', '/');
                files.put(checkPackPath(relative), read(path));
              });
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read resource pack folder " + root, e);
    }
    return this;
  }

  /** Add every file of an existing pack zip, keeping their paths. */
  public ResourcePackBuilder zip(String zip) {
    return zip(Path.of(zip));
  }

  /** Add every file of an existing pack zip, keeping their paths. */
  public ResourcePackBuilder zip(Path zip) {
    Path file = manager.resolve(zip);
    if (!Files.isRegularFile(file)) {
      throw new IllegalArgumentException("Resource pack zip not found: " + file);
    }
    try (ZipInputStream in = new ZipInputStream(Files.newInputStream(file))) {
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          files.put(checkPackPath(entry.getName()), in.readAllBytes());
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read resource pack zip " + file, e);
    }
    return this;
  }

  /**
   * Add a PNG texture at {@code assets/<namespace>/textures/<path>.png}.
   *
   * @param path e.g. {@code item/ruby}; refer to it in models as {@code <namespace>:item/ruby}
   */
  public ResourcePackBuilder texture(String path, Path png) {
    return texture(path, read(png));
  }

  /** Add a PNG texture at {@code assets/<namespace>/textures/<path>.png}. */
  public ResourcePackBuilder texture(String path, byte[] png) {
    checkSignature(png, PNG_SIGNATURE, "PNG", path);
    namespaceUsed = true;
    return file("assets/" + namespace + "/textures/" + checkResourcePath(path) + ".png", png);
  }

  /**
   * Add a flat item model, like vanilla items, from a single PNG. Apply it with {@link
   * ResourcePack#applyModel(org.bukkit.inventory.ItemStack, String)}.
   */
  public ResourcePackBuilder itemModel(String name, Path png) {
    return itemModel(name, read(png));
  }

  /** Add a flat item model from a single PNG. */
  public ResourcePackBuilder itemModel(String name, byte[] png) {
    return flatItemModel(name, png, "minecraft:item/generated");
  }

  /** Add a flat item model held like a tool or sword. */
  public ResourcePackBuilder handheldItemModel(String name, Path png) {
    return handheldItemModel(name, read(png));
  }

  /** Add a flat item model held like a tool or sword. */
  public ResourcePackBuilder handheldItemModel(String name, byte[] png) {
    return flatItemModel(name, png, "minecraft:item/handheld");
  }

  /**
   * Add a 3D item model, e.g. exported from Blockbench as "Java Block/Item". Add the textures it
   * uses with {@link #texture}, and point the model at them as {@code <namespace>:<path>}.
   */
  public ResourcePackBuilder itemModel3d(String name, Path modelJson) {
    return itemModel3d(name, new String(read(modelJson), StandardCharsets.UTF_8));
  }

  /** Add a 3D item model from its JSON. */
  public ResourcePackBuilder itemModel3d(String name, String modelJson) {
    checkResourcePath(name);
    try {
      JsonParser.parseString(modelJson).getAsJsonObject();
    } catch (JsonParseException | IllegalStateException e) {
      throw new IllegalArgumentException("Model '" + name + "' is not a valid JSON object", e);
    }
    namespaceUsed = true;
    file("assets/" + namespace + "/models/item/" + name + ".json", modelJson);
    return itemDefinition(name);
  }

  /**
   * Add an OGG sound. Play it with {@link ResourcePack#playSound} or as {@code <namespace>:<name>}.
   */
  public ResourcePackBuilder sound(String name, Path ogg) {
    return sound(name, read(ogg));
  }

  /** Add an OGG sound. */
  public ResourcePackBuilder sound(String name, byte[] ogg) {
    checkSignature(ogg, OGG_SIGNATURE, "OGG", name);
    namespaceUsed = true;
    file("assets/" + namespace + "/sounds/" + checkResourcePath(name) + ".ogg", ogg);
    sounds.put(name, namespace + ":" + name);
    return this;
  }

  /**
   * Build the pack and register it. It is sent to everyone online and to every player who joins. A
   * pack with the same name from the same owner is replaced; players only download it again if its
   * content changed.
   *
   * @throws IllegalArgumentException if the pack is invalid or larger than the client accepts
   */
  public ResourcePack register() {
    byte[] zip = buildZip();
    if (zip.length > ResourcePackManager.MAX_PACK_BYTES) {
      throw new IllegalArgumentException(
          "Resource pack "
              + id
              + " is "
              + (zip.length / (1024 * 1024))
              + " MiB; clients accept at most "
              + (ResourcePackManager.MAX_PACK_BYTES / (1024 * 1024))
              + " MiB");
    }
    ResourcePack pack =
        new ResourcePack(
            id, namespace, sha1(zip), zip, null, priority, required, onLoaded, onFailed);
    manager.register(pack);
    return pack;
  }

  private ResourcePackBuilder flatItemModel(String name, byte[] png, String parent) {
    checkResourcePath(name);
    texture("item/" + name, png);
    JsonObject textures = new JsonObject();
    textures.addProperty("layer0", namespace + ":item/" + name);
    JsonObject model = new JsonObject();
    model.addProperty("parent", parent);
    model.add("textures", textures);
    file("assets/" + namespace + "/models/item/" + name + ".json", GSON.toJson(model));
    return itemDefinition(name);
  }

  /** The 1.21.4+ item definition that {@code item_model} points at. */
  private ResourcePackBuilder itemDefinition(String name) {
    JsonObject model = new JsonObject();
    model.addProperty("type", "minecraft:model");
    model.addProperty("model", namespace + ":item/" + name);
    JsonObject definition = new JsonObject();
    definition.add("model", model);
    return file("assets/" + namespace + "/items/" + name + ".json", GSON.toJson(definition));
  }

  byte[] buildZip() {
    Map<String, byte[]> content = new TreeMap<>(files);
    content.putIfAbsent("pack.mcmeta", packMeta());
    if (!sounds.isEmpty()) {
      content.put("assets/" + namespace + "/sounds.json", soundsJson(content));
    }
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    // Sorted entries and a fixed timestamp keep the hash stable, so an unchanged pack is not
    // downloaded again after a reload
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      for (Map.Entry<String, byte[]> file : content.entrySet()) {
        ZipEntry entry = new ZipEntry(file.getKey());
        entry.setLastModifiedTime(ZIP_TIME);
        zip.putNextEntry(entry);
        zip.write(file.getValue());
        zip.closeEntry();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not build resource pack " + id, e);
    }
    return bytes.toByteArray();
  }

  private byte[] packMeta() {
    JsonObject pack = new JsonObject();
    pack.addProperty("description", description);
    // pack_format for clients before 1.21.9, min/max_format for newer ones
    pack.addProperty("pack_format", minFormat);
    pack.addProperty("min_format", minFormat);
    pack.addProperty("max_format", maxFormat);
    JsonObject meta = new JsonObject();
    meta.add("pack", pack);
    return GSON.toJson(meta).getBytes(StandardCharsets.UTF_8);
  }

  /** Generated entries are merged into a sounds.json the pack already has. */
  private byte[] soundsJson(Map<String, byte[]> content) {
    String path = "assets/" + namespace + "/sounds.json";
    JsonObject json = new JsonObject();
    byte[] existing = content.get(path);
    if (existing != null) {
      try {
        json =
            JsonParser.parseString(new String(existing, StandardCharsets.UTF_8)).getAsJsonObject();
      } catch (JsonParseException | IllegalStateException e) {
        throw new IllegalArgumentException(path + " in pack " + id + " is not valid JSON", e);
      }
    }
    for (Map.Entry<String, String> sound : sounds.entrySet()) {
      JsonArray files = new JsonArray();
      files.add(sound.getValue());
      JsonObject event = new JsonObject();
      event.add("sounds", files);
      json.add(sound.getKey(), event);
    }
    return GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
  }

  private byte[] read(Path path) {
    Path file = manager.resolve(Objects.requireNonNull(path, "path cannot be null"));
    try (InputStream in = Files.newInputStream(file)) {
      return in.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read " + file, e);
    }
  }

  /** Paths inside the zip; blocks absolute paths and {@code ..} so a pack cannot escape. */
  private static String checkPackPath(String path) {
    if (path == null || path.isEmpty() || path.startsWith("/") || path.contains("\\")) {
      throw new IllegalArgumentException("Invalid path inside a resource pack: " + path);
    }
    for (String part : path.split("/")) {
      if (part.isEmpty() || part.equals("..") || part.equals(".")) {
        throw new IllegalArgumentException("Invalid path inside a resource pack: " + path);
      }
    }
    return path;
  }

  private static String checkResourcePath(String path) {
    if (path == null || !RESOURCE_PATH.matcher(path).matches()) {
      throw new IllegalArgumentException(
          "Invalid resource name '"
              + path
              + "': Minecraft only allows lower-case letters, digits, '_', '-', '.' and '/'");
    }
    return path;
  }

  private static void checkSignature(byte[] data, byte[] signature, String type, String name) {
    Objects.requireNonNull(data, "data cannot be null");
    boolean matches = data.length >= signature.length;
    for (int i = 0; matches && i < signature.length; i++) {
      matches = data[i] == signature[i];
    }
    if (!matches) {
      throw new IllegalArgumentException("'" + name + "' is not a " + type + " file");
    }
  }

  static String sha1(byte[] data) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 is required by every Java runtime", e);
    }
  }
}
