package dev.mukulx.javaskript.api.resourcepack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourcePackBuilderTest {

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2};

  @TempDir Path temp;
  private ResourcePackManager manager;

  @BeforeEach
  void setUp() {
    JavaSkriptPlugin plugin = mock(JavaSkriptPlugin.class);
    when(plugin.getDataFolder()).thenReturn(temp.toFile());
    when(plugin.getConfig()).thenReturn(new YamlConfiguration());
    when(plugin.getLogger()).thenReturn(Logger.getLogger("ResourcePackBuilderTest"));
    manager = new ResourcePackManager(plugin);
  }

  @Test
  void sameContentGivesTheSameZip() {
    byte[] first = manager.builder("Weapons", "main").itemModel("ruby", PNG).buildZip();
    byte[] second = manager.builder("Weapons", "main").itemModel("ruby", PNG).buildZip();

    assertArrayEquals(first, second);
  }

  @Test
  void itemModelGeneratesTextureModelAndDefinition() throws IOException {
    Map<String, byte[]> files =
        unzip(manager.builder("PvP/Arena", "main").handheldItemModel("ruby_sword", PNG).buildZip());

    assertArrayEquals(PNG, files.get("assets/pvp_arena/textures/item/ruby_sword.png"));
    JsonObject model = json(files.get("assets/pvp_arena/models/item/ruby_sword.json"));
    assertEquals("minecraft:item/handheld", model.get("parent").getAsString());
    assertEquals(
        "pvp_arena:item/ruby_sword", model.getAsJsonObject("textures").get("layer0").getAsString());
    JsonObject definition = json(files.get("assets/pvp_arena/items/ruby_sword.json"));
    assertEquals(
        "pvp_arena:item/ruby_sword",
        definition.getAsJsonObject("model").get("model").getAsString());
    JsonObject meta = json(files.get("pack.mcmeta")).getAsJsonObject("pack");
    assertEquals(ResourcePackBuilder.DEFAULT_PACK_FORMAT, meta.get("min_format").getAsInt());
  }

  @Test
  void soundsAreMergedIntoSoundsJson() throws IOException {
    byte[] ogg = {'O', 'g', 'g', 'S', 0};
    Map<String, byte[]> files =
        unzip(
            manager
                .builder("fx", "main")
                .file("assets/fx/sounds.json", "{\"old\":{\"sounds\":[\"fx:old\"]}}")
                .sound("boom", ogg)
                .buildZip());

    JsonObject sounds = json(files.get("assets/fx/sounds.json"));
    assertTrue(sounds.has("old"));
    assertEquals(
        "fx:boom", sounds.getAsJsonObject("boom").getAsJsonArray("sounds").get(0).getAsString());
  }

  @Test
  void folderKeepsItsPackMcmeta() throws IOException {
    Path folder = temp.resolve("resourcepacks/custom");
    Files.createDirectories(folder.resolve("assets/minecraft/textures"));
    Files.writeString(folder.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"mine\"}}");
    Files.write(folder.resolve("assets/minecraft/textures/stone.png"), PNG);

    Map<String, byte[]> files = unzip(manager.builder("s", "main").folder("custom").buildZip());

    assertEquals(
        "mine",
        json(files.get("pack.mcmeta")).getAsJsonObject("pack").get("description").getAsString());
    assertArrayEquals(PNG, files.get("assets/minecraft/textures/stone.png"));
  }

  @Test
  void invalidInputIsRejectedWithAClearError() {
    ResourcePackBuilder builder = manager.builder("s", "main");

    assertThrows(IllegalArgumentException.class, () -> builder.file("../escape.txt", "x"));
    assertThrows(IllegalArgumentException.class, () -> builder.itemModel("Ruby", PNG));
    assertThrows(
        IllegalArgumentException.class, () -> builder.texture("item/ruby", new byte[] {1}));
    assertThrows(IllegalArgumentException.class, () -> manager.builder("s", "Bad Name"));
    builder.itemModel("ruby", PNG);
    assertThrows(IllegalStateException.class, () -> builder.namespace("other"));
  }

  @Test
  void serverOnlyServesPublishedPacks() throws Exception {
    PackHttpServer server = PackHttpServer.start(new InetSocketAddress("127.0.0.1", 0));
    try {
      byte[] zip = manager.builder("s", "main").itemModel("ruby", PNG).buildZip();
      String hash = ResourcePackBuilder.sha1(zip);
      server.publish(hash, zip);
      HttpClient client = HttpClient.newHttpClient();
      String base = "http://127.0.0.1:" + server.port() + "/";

      HttpResponse<byte[]> found =
          client.send(
              HttpRequest.newBuilder(URI.create(base + hash + ".zip")).build(),
              HttpResponse.BodyHandlers.ofByteArray());
      HttpResponse<byte[]> missing =
          client.send(
              HttpRequest.newBuilder(URI.create(base + "../config.yml")).build(),
              HttpResponse.BodyHandlers.ofByteArray());

      assertEquals(200, found.statusCode());
      assertArrayEquals(zip, found.body());
      assertEquals(404, missing.statusCode());
    } finally {
      server.stop();
    }
  }

  private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
    Map<String, byte[]> files = new TreeMap<>();
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
      ZipEntry entry;
      while ((entry = in.getNextEntry()) != null) {
        files.put(entry.getName(), in.readAllBytes());
      }
    }
    return files;
  }

  private static JsonObject json(byte[] data) {
    return JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
  }
}
