package dev.mukulx.javaskript.api.mannequin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.MannequinHelper;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MannequinTest {

  private JavaSkriptPlugin plugin;
  private MannequinHelper helper;

  @BeforeEach
  void setUp() {
    plugin = mock(JavaSkriptPlugin.class);
    when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Test"));
    helper = new MannequinHelper(plugin, "TestScript.java");
  }

  @Test
  void testValidPoses() {
    Set<Pose> poses = MannequinHelper.validPoses();
    assertNotNull(poses);
    assertTrue(poses.contains(Pose.STANDING));
    assertTrue(poses.contains(Pose.SNEAKING));
    assertTrue(poses.contains(Pose.SWIMMING));
    assertTrue(poses.contains(Pose.FALL_FLYING));
    assertTrue(poses.contains(Pose.SLEEPING));
  }

  @Test
  void testBuilderDefaults() {
    CustomMannequin mannequin = helper.create(null);

    assertEquals(Pose.STANDING, mannequin.pose());
    assertEquals(MainHand.RIGHT, mannequin.mainHand());
    assertTrue(mannequin.isRightHanded());
    assertFalse(mannequin.isLeftHanded());
    assertTrue(mannequin.isImmovable());
    assertTrue(mannequin.isInvulnerable());
    assertTrue(mannequin.isSilent());
    assertFalse(mannequin.hasGravity());
    assertFalse(mannequin.isCollidable());
    assertFalse(mannequin.isGlowing());
    assertFalse(mannequin.isInvisible());
    assertTrue(mannequin.isEquipmentLocked());
    assertTrue(mannequin.hasCape());
    assertTrue(mannequin.hasJacket());
    assertTrue(mannequin.hasHat());
    assertTrue(mannequin.hasLeftSleeve());
    assertTrue(mannequin.hasRightSleeve());
    assertTrue(mannequin.hasLeftPants());
    assertTrue(mannequin.hasRightPants());
  }

  @Test
  void testFluentBuilderConfiguration() {
    ItemStack mockHelmet = mock(ItemStack.class);
    when(mockHelmet.getType()).thenReturn(Material.DIAMOND_HELMET);
    when(mockHelmet.clone()).thenReturn(mockHelmet);

    ItemStack mockChestplate = mock(ItemStack.class);
    when(mockChestplate.getType()).thenReturn(Material.NETHERITE_CHESTPLATE);
    when(mockChestplate.clone()).thenReturn(mockChestplate);

    ItemStack mockSword = mock(ItemStack.class);
    when(mockSword.getType()).thenReturn(Material.DIAMOND_SWORD);
    when(mockSword.clone()).thenReturn(mockSword);

    CustomMannequin mannequin =
        helper
            .create(null)
            .name("<gold>Shopkeeper</gold>")
            .description("<gray>Sells rare artifacts</gray>")
            .skin("Notch")
            .sneaking()
            .leftHanded()
            .helmet(mockHelmet)
            .chestplate(mockChestplate)
            .mainHandItem(mockSword)
            .tag("merchant")
            .tag("quest_npc")
            .cape(false)
            .glowing(true);

    assertNotNull(mannequin.name());
    assertNotNull(mannequin.description());
    io.papermc.paper.datacomponent.item.ResolvableProfile mockProfile =
        mock(io.papermc.paper.datacomponent.item.ResolvableProfile.class);
    mannequin.skin(mockProfile);
    assertEquals(mockProfile, mannequin.profile());
    assertEquals(Pose.SNEAKING, mannequin.pose());
    assertEquals(MainHand.LEFT, mannequin.mainHand());
    assertTrue(mannequin.isLeftHanded());
    assertNotNull(mannequin.getHelmet());
    assertEquals(Material.DIAMOND_HELMET, mannequin.getHelmet().getType());
    assertNotNull(mannequin.getChestplate());
    assertEquals(Material.NETHERITE_CHESTPLATE, mannequin.getChestplate().getType());
    assertNotNull(mannequin.getMainHandItem());
    assertEquals(Material.DIAMOND_SWORD, mannequin.getMainHandItem().getType());
    assertTrue(mannequin.hasTag("merchant"));
    assertTrue(mannequin.hasTag("quest_npc"));
    assertFalse(mannequin.hasCape());
    assertTrue(mannequin.isGlowing());
  }

  @Test
  void testPoseShortcuts() {
    CustomMannequin m = helper.create(null);

    m.standing();
    assertEquals(Pose.STANDING, m.pose());

    m.crouching();
    assertEquals(Pose.SNEAKING, m.pose());

    m.swimming();
    assertEquals(Pose.SWIMMING, m.pose());

    m.gliding();
    assertEquals(Pose.FALL_FLYING, m.pose());

    m.sleeping();
    assertEquals(Pose.SLEEPING, m.pose());
  }

  @Test
  void testSkinPartsToggles() {
    CustomMannequin m = helper.create(null);

    m.noSkinParts();
    assertFalse(m.hasCape());
    assertFalse(m.hasJacket());
    assertFalse(m.hasHat());
    assertFalse(m.hasLeftSleeve());
    assertFalse(m.hasRightSleeve());
    assertFalse(m.hasLeftPants());
    assertFalse(m.hasRightPants());

    m.allSkinParts();
    assertTrue(m.hasCape());
    assertTrue(m.hasJacket());
    assertTrue(m.hasHat());
    assertTrue(m.hasLeftSleeve());
    assertTrue(m.hasRightSleeve());
    assertTrue(m.hasLeftPants());
    assertTrue(m.hasRightPants());

    m.sleeves(false);
    assertFalse(m.hasLeftSleeve());
    assertFalse(m.hasRightSleeve());

    m.pants(false);
    assertFalse(m.hasLeftPants());
    assertFalse(m.hasRightPants());
  }

  @Test
  void testCallbacksExecution() {
    CustomMannequin m = helper.create(null);
    Player player = mock(Player.class);

    AtomicBoolean clicked = new AtomicBoolean(false);
    AtomicBoolean attacked = new AtomicBoolean(false);
    AtomicBoolean interacted = new AtomicBoolean(false);

    m.onClick((p, target) -> clicked.set(true));
    m.onAttack((p, target) -> attacked.set(true));
    m.onInteract((p, target, hand) -> interacted.set(hand == EquipmentSlot.HAND));

    m.handleClick(player);
    assertTrue(clicked.get());

    m.handleAttack(player);
    assertTrue(attacked.get());

    m.handleInteract(player, EquipmentSlot.HAND);
    assertTrue(interacted.get());
  }

  @Test
  void testHelperTrackingAndQuerying() {
    CustomMannequin m1 = helper.create(null).tag("npc_1");
    CustomMannequin m2 = helper.create(null).tag("npc_2");

    helper.track(m1);
    helper.track(m2);

    assertEquals(2, helper.getAll().size());
    assertEquals(1, helper.findByTag("npc_1").size());
    assertEquals(1, helper.findByTag("npc_2").size());

    helper.untrack(m1);
    assertEquals(1, helper.getAll().size());

    helper.removeAll();
    assertTrue(helper.getAll().isEmpty());
  }

  @Test
  void testWrapExistingMannequin() {
    Mannequin mockMannequin = mock(Mannequin.class);
    UUID uuid = UUID.randomUUID();
    Location loc = mock(Location.class);

    when(mockMannequin.getUniqueId()).thenReturn(uuid);
    when(mockMannequin.getLocation()).thenReturn(loc);
    when(mockMannequin.getPose()).thenReturn(Pose.SLEEPING);
    when(mockMannequin.getMainHand()).thenReturn(MainHand.LEFT);
    when(mockMannequin.isImmovable()).thenReturn(true);
    when(mockMannequin.isInvulnerable()).thenReturn(true);
    when(mockMannequin.hasGravity()).thenReturn(false);
    when(mockMannequin.isSilent()).thenReturn(true);
    when(mockMannequin.getScoreboardTags()).thenReturn(Set.of("statue"));

    CustomMannequin wrapped = helper.wrap(mockMannequin);

    assertNotNull(wrapped);
    assertEquals(uuid, wrapped.getUniqueId());
    assertEquals(Pose.SLEEPING, wrapped.pose());
    assertEquals(MainHand.LEFT, wrapped.mainHand());
    assertTrue(wrapped.hasTag("statue"));
    assertTrue(helper.getAll().contains(wrapped));
  }
}
