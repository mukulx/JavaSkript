package dev.mukulx.javaskript.api.advancement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.AdvancementHelper;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.UnsafeValues;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AdvancementTest {

  private JavaSkriptPlugin plugin;
  private AdvancementHelper helper;
  private NamespacedKey testKey;

  @BeforeEach
  void setUp() {
    plugin = mock(JavaSkriptPlugin.class);
    when(plugin.getName()).thenReturn("javaskript");
    when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("TestAdvancement"));
    helper = new AdvancementHelper(plugin, "Quests.java");
    testKey = new NamespacedKey("javaskript", "quests_slayer");
  }

  @Test
  void testJsonDefaults() {
    CustomAdvancement custom = helper.create("slayer");
    String jsonStr = custom.toJson();
    assertNotNull(jsonStr);

    JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
    assertTrue(json.has("display"));
    JsonObject display = json.getAsJsonObject("display");

    assertEquals("minecraft:diamond", display.getAsJsonObject("icon").get("id").getAsString());
    assertEquals("task", display.get("frame").getAsString());
    assertTrue(display.get("show_toast").getAsBoolean());
    assertTrue(display.get("announce_to_chat").getAsBoolean());
    assertFalse(display.get("hidden").getAsBoolean());

    assertTrue(json.has("criteria"));
    JsonObject criteria = json.getAsJsonObject("criteria");
    assertTrue(criteria.has("impossible"));
    assertEquals(
        "minecraft:impossible",
        criteria.getAsJsonObject("impossible").get("trigger").getAsString());

    assertTrue(json.has("requirements"));
    JsonArray reqs = json.getAsJsonArray("requirements");
    assertEquals(1, reqs.size());
    assertEquals("impossible", reqs.get(0).getAsJsonArray().get(0).getAsString());
  }

  @Test
  void testJsonCustomDisplayAndParent() {
    CustomAdvancement custom =
        helper
            .create("boss_hunter")
            .title("<gold>Boss Hunter</gold>")
            .description("<gray>Defeat the wither</gray>")
            .icon(Material.NETHER_STAR)
            .challenge()
            .toast(true)
            .announce(false)
            .hidden(true)
            .background(CustomAdvancement.Backgrounds.NETHER)
            .parent("minecraft:nether/root");

    JsonObject json = JsonParser.parseString(custom.toJson()).getAsJsonObject();
    JsonObject display = json.getAsJsonObject("display");

    assertEquals("minecraft:nether_star", display.getAsJsonObject("icon").get("id").getAsString());
    assertEquals("challenge", display.get("frame").getAsString());
    assertTrue(display.get("show_toast").getAsBoolean());
    assertFalse(display.get("announce_to_chat").getAsBoolean());
    assertTrue(display.get("hidden").getAsBoolean());
    assertEquals(CustomAdvancement.Backgrounds.NETHER, display.get("background").getAsString());
    assertEquals("minecraft:nether/root", json.get("parent").getAsString());
  }

  @Test
  void testCriteriaAndRequirementsAll() {
    CustomAdvancement custom =
        helper.create("multi_step").criteria("kill_zombie", "kill_skeleton").requireAll();

    JsonObject json = JsonParser.parseString(custom.toJson()).getAsJsonObject();
    JsonObject criteria = json.getAsJsonObject("criteria");
    assertEquals(2, criteria.size());
    assertTrue(criteria.has("kill_zombie"));
    assertTrue(criteria.has("kill_skeleton"));

    JsonArray reqs = json.getAsJsonArray("requirements");
    assertEquals(2, reqs.size());
    assertEquals("kill_zombie", reqs.get(0).getAsJsonArray().get(0).getAsString());
    assertEquals("kill_skeleton", reqs.get(1).getAsJsonArray().get(0).getAsString());
  }

  @Test
  void testCriteriaAndRequirementsAny() {
    CustomAdvancement custom =
        helper.create("any_step").criteria("find_ruby", "find_sapphire").requireAny();

    JsonObject json = JsonParser.parseString(custom.toJson()).getAsJsonObject();
    JsonArray reqs = json.getAsJsonArray("requirements");
    assertEquals(1, reqs.size());
    JsonArray inner = reqs.get(0).getAsJsonArray();
    assertEquals(2, inner.size());
    assertEquals("find_ruby", inner.get(0).getAsString());
    assertEquals("find_sapphire", inner.get(1).getAsString());
  }

  @Test
  @SuppressWarnings("deprecation")
  void testRegistrationAndLifecycleCleanup() {
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      UnsafeValues unsafe = mock(UnsafeValues.class);
      Advancement mockAdv = mock(Advancement.class);
      when(mockAdv.getKey()).thenReturn(testKey);

      bukkit.when(Bukkit::getUnsafe).thenReturn(unsafe);
      when(unsafe.loadAdvancement(eq(testKey), any(String.class))).thenReturn(mockAdv);
      when(unsafe.removeAdvancement(eq(testKey))).thenReturn(true);
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());

      CustomAdvancement custom = helper.create("slayer");
      Advancement registered = helper.register(custom);

      assertNotNull(registered);
      assertEquals(testKey, registered.getKey());
      assertTrue(helper.getRegisteredKeys().contains(testKey));

      // Test unload cleanup
      helper.removeAll();

      verify(unsafe, atLeastOnce()).removeAdvancement(testKey);
      assertTrue(helper.getRegisteredKeys().isEmpty());
    }
  }

  @Test
  void testGrantRevokeAndProgress() {
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      Player player = mock(Player.class);
      Advancement adv = mock(Advancement.class);
      AdvancementProgress progress = mock(AdvancementProgress.class);

      when(adv.getKey()).thenReturn(testKey);
      when(adv.getCriteria()).thenReturn(List.of("c1", "c2"));
      bukkit.when(() -> Bukkit.getAdvancement(testKey)).thenReturn(adv);
      when(player.getAdvancementProgress(adv)).thenReturn(progress);

      when(progress.getRemainingCriteria()).thenReturn(List.of("c1", "c2"));
      when(progress.getAwardedCriteria()).thenReturn(List.of("c1"));
      when(progress.isDone()).thenReturn(false);

      // Grant remaining
      boolean granted = helper.grant(player, testKey);
      assertTrue(granted);
      verify(progress).awardCriteria("c1");
      verify(progress).awardCriteria("c2");

      // Check progress percentage (1 awarded out of 2 = 50.0%)
      double percent = helper.getProgressPercent(player, testKey);
      assertEquals(50.0, percent, 0.001);

      // Check has
      assertFalse(helper.has(player, testKey));
      when(progress.isDone()).thenReturn(true);
      assertTrue(helper.has(player, testKey));

      // Revoke awarded
      boolean revoked = helper.revoke(player, testKey);
      assertTrue(revoked);
      verify(progress).revokeCriteria("c1");
    }
  }

  @Test
  void testCompletionCallbackDispatch() {
    Player player = mock(Player.class);
    Advancement adv = mock(Advancement.class);
    when(adv.getKey()).thenReturn(testKey);

    AtomicBoolean callbackFired = new AtomicBoolean(false);
    helper.onComplete(
        testKey,
        (p, a) -> {
          assertEquals(player, p);
          assertEquals(adv, a);
          callbackFired.set(true);
        });

    AdvancementHelper.dispatchDone(player, adv);
    assertTrue(callbackFired.get());
  }
}
