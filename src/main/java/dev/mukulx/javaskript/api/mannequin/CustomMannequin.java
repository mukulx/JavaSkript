package dev.mukulx.javaskript.api.mannequin;

import com.destroystokyo.paper.SkinParts;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.MannequinHelper;
import dev.mukulx.javaskript.util.TextUtil;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/**
 * Fluent builder and active controller for Paper 1.21.11+ Mannequin entities.
 *
 * <p>Enables full control over player statues, NPCs, and mannequins with custom skins, poses,
 * descriptions, equipment, and interactive click callbacks:
 *
 * <pre>{@code
 * mannequins.create(location)
 *     .name("<gold><bold>Shopkeeper</bold></gold>")
 *     .description("<gray>Click to open market</gray>")
 *     .skin("Steve")
 *     .helmet(Material.DIAMOND_HELMET)
 *     .mainHandItem(Material.EMERALD)
 *     .standing()
 *     .immovable()
 *     .onClick((player, mannequin) -> player.sendMessage("Welcome!"))
 *     .spawn();
 * }</pre>
 */
public class CustomMannequin {

  private final JavaSkriptPlugin plugin;
  private final MannequinHelper helper;
  private Location location;
  private Mannequin entity;
  private boolean spawned = false;

  // Visuals & Display
  private Component customName = null;
  private boolean customNameVisible = true;
  private Component description = null;
  private ResolvableProfile profile = null;
  private String skinName = null;
  private UUID skinUuid = null;
  private String textureBase64 = null;
  private String textureSignature = null;
  private Pose pose = Pose.STANDING;
  private MainHand mainHand = MainHand.RIGHT;

  // Physics & Behavior
  private boolean immovable = true;
  private boolean invulnerable = true;
  private boolean gravity = false;
  private boolean silent = true;
  private boolean collidable = false;
  private boolean glowing = false;
  private boolean invisible = false;
  private boolean persistent = false;
  private boolean equipmentLocked = true;

  // Equipment cache
  private ItemStack helmet = null;
  private ItemStack chestplate = null;
  private ItemStack leggings = null;
  private ItemStack boots = null;
  private ItemStack mainHandItem = null;
  private ItemStack offHandItem = null;

  // Skin parts cache (all enabled by default)
  private boolean cape = true;
  private boolean jacket = true;
  private boolean leftSleeve = true;
  private boolean rightSleeve = true;
  private boolean leftPants = true;
  private boolean rightPants = true;
  private boolean hats = true;

  // Callbacks
  private final List<MannequinClickCallback> clickCallbacks = new CopyOnWriteArrayList<>();
  private final List<MannequinAttackCallback> attackCallbacks = new CopyOnWriteArrayList<>();
  private final List<MannequinInteractCallback> interactCallbacks = new CopyOnWriteArrayList<>();

  // Scoreboard Tags
  private final Set<String> tags = new HashSet<>();

  public CustomMannequin(JavaSkriptPlugin plugin, MannequinHelper helper, Location location) {
    this.plugin = plugin;
    this.helper = helper;
    this.location = location != null ? location.clone() : null;
  }

  public CustomMannequin(
      JavaSkriptPlugin plugin, MannequinHelper helper, Mannequin existingEntity) {
    this.plugin = plugin;
    this.helper = helper;
    this.entity = existingEntity;
    this.location = existingEntity.getLocation().clone();
    this.spawned = true;
    readFromEntity(existingEntity);
  }

  private void readFromEntity(Mannequin m) {
    this.customName = m.customName();
    this.customNameVisible = m.isCustomNameVisible();
    this.description = m.getDescription();
    this.profile = m.getProfile();
    this.pose = m.getPose();
    this.mainHand = m.getMainHand();
    this.immovable = m.isImmovable();
    this.invulnerable = m.isInvulnerable();
    this.gravity = m.hasGravity();
    this.silent = m.isSilent();
    this.collidable = m.isCollidable();
    this.glowing = m.isGlowing();
    this.invisible = m.isInvisible();
    this.persistent = m.isPersistent();

    SkinParts parts = m.getSkinParts();
    if (parts != null) {
      this.cape = parts.hasCapeEnabled();
      this.jacket = parts.hasJacketEnabled();
      this.leftSleeve = parts.hasLeftSleeveEnabled();
      this.rightSleeve = parts.hasRightSleeveEnabled();
      this.leftPants = parts.hasLeftPantsEnabled();
      this.rightPants = parts.hasRightPantsEnabled();
      this.hats = parts.hasHatsEnabled();
    }

    EntityEquipment eq = m.getEquipment();
    if (eq != null) {
      this.helmet = eq.getHelmet();
      this.chestplate = eq.getChestplate();
      this.leggings = eq.getLeggings();
      this.boots = eq.getBoots();
      this.mainHandItem = eq.getItemInMainHand();
      this.offHandItem = eq.getItemInOffHand();
    }
    this.tags.addAll(m.getScoreboardTags());
  }

  // ==========================================
  // Display & Name
  // ==========================================

  /** Set custom name using MiniMessage, legacy formatting, or plain text. */
  public CustomMannequin name(String name) {
    return name(name != null ? TextUtil.parse(name) : null);
  }

  /** Set custom name using Adventure Component. */
  public CustomMannequin name(Component name) {
    this.customName = name;
    if (spawned && entity != null) {
      entity.customName(name);
      entity.setCustomNameVisible(customNameVisible && name != null);
    }
    return this;
  }

  /** Get the current custom name component. */
  public Component name() {
    return spawned && entity != null ? entity.customName() : customName;
  }

  /** Set whether the custom name is visible above the mannequin. */
  public CustomMannequin nameVisible(boolean visible) {
    this.customNameVisible = visible;
    if (spawned && entity != null) {
      entity.setCustomNameVisible(visible);
    }
    return this;
  }

  /** Check if the custom name is set to visible. */
  public boolean nameVisible() {
    return spawned && entity != null ? entity.isCustomNameVisible() : customNameVisible;
  }

  // ==========================================
  // Description (Paper 1.21.11+ feature)
  // ==========================================

  /**
   * Set the description text appearing directly below the mannequin's name. Supports MiniMessage
   * and legacy formatting.
   */
  public CustomMannequin description(String text) {
    return description(text != null ? TextUtil.parse(text) : null);
  }

  /** Set the description Component appearing directly below the mannequin's name. */
  public CustomMannequin description(Component text) {
    this.description = text;
    if (spawned && entity != null) {
      entity.setDescription(text);
    }
    return this;
  }

  /** Get the current description component. */
  public Component description() {
    return spawned && entity != null ? entity.getDescription() : description;
  }

  /** Reset description to Paper's default mannequin description. */
  public CustomMannequin defaultDescription() {
    try {
      return description(Mannequin.defaultDescription());
    } catch (Throwable t) {
      return description((Component) null);
    }
  }

  // ==========================================
  // Profile & Skin
  // ==========================================

  /** Set skin by player name (e.g. "Notch", "Mukulx"). */
  public CustomMannequin skin(String playerNameOrUuid) {
    if (playerNameOrUuid == null || playerNameOrUuid.isBlank()) {
      return defaultSkin();
    }
    this.profile = null;
    this.textureBase64 = null;
    this.textureSignature = null;
    try {
      this.skinUuid = UUID.fromString(playerNameOrUuid);
      this.skinName = null;
    } catch (IllegalArgumentException notUuid) {
      this.skinName = playerNameOrUuid;
      this.skinUuid = null;
    }
    if (spawned && entity != null) {
      applyResolvedProfile();
    }
    return this;
  }

  /** Set skin by player UUID. */
  public CustomMannequin skin(UUID uuid) {
    if (uuid == null) {
      return defaultSkin();
    }
    this.profile = null;
    this.skinUuid = uuid;
    this.skinName = null;
    this.textureBase64 = null;
    this.textureSignature = null;
    if (spawned && entity != null) {
      applyResolvedProfile();
    }
    return this;
  }

  /** Set skin directly from an online Player's current profile. */
  public CustomMannequin skin(Player player) {
    if (player == null) {
      return defaultSkin();
    }
    try {
      return skin(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
    } catch (Throwable t) {
      return skin(player.getName());
    }
  }

  /** Set skin from a Paper PlayerProfile. */
  public CustomMannequin skin(PlayerProfile playerProfile) {
    if (playerProfile == null) {
      return defaultSkin();
    }
    try {
      return skin(ResolvableProfile.resolvableProfile(playerProfile));
    } catch (Throwable t) {
      return skin(playerProfile.getName());
    }
  }

  /** Set skin directly from a ResolvableProfile. */
  public CustomMannequin skin(ResolvableProfile resolvableProfile) {
    this.profile = resolvableProfile;
    this.skinName = null;
    this.skinUuid = null;
    this.textureBase64 = null;
    this.textureSignature = null;
    if (spawned && entity != null && resolvableProfile != null) {
      entity.setProfile(resolvableProfile);
    }
    return this;
  }

  /** Set skin with raw base64 texture value and optional signature. */
  public CustomMannequin skin(String textureBase64, String signature) {
    this.profile = null;
    this.skinName = null;
    this.skinUuid = null;
    this.textureBase64 = textureBase64;
    this.textureSignature = signature;
    if (spawned && entity != null) {
      applyResolvedProfile();
    }
    return this;
  }

  /** Reset skin to default mannequin skin profile. */
  public CustomMannequin defaultSkin() {
    this.skinName = null;
    this.skinUuid = null;
    this.textureBase64 = null;
    this.textureSignature = null;
    try {
      this.profile = Mannequin.defaultProfile();
    } catch (Throwable ignored) {
      this.profile = null;
    }
    if (spawned && entity != null && profile != null) {
      entity.setProfile(profile);
    }
    return this;
  }

  /** Get current ResolvableProfile. Resolves on-demand if built via skin(name/uuid). */
  public ResolvableProfile profile() {
    if (spawned && entity != null) {
      return entity.getProfile();
    }
    return resolveProfile();
  }

  /** Resolves the profile based on the configured skin properties. */
  public ResolvableProfile resolveProfile() {
    if (profile != null) return profile;
    try {
      if (skinUuid != null) {
        return ResolvableProfile.resolvableProfile().uuid(skinUuid).build();
      }
      if (skinName != null) {
        return ResolvableProfile.resolvableProfile().name(skinName).build();
      }
      if (textureBase64 != null) {
        var builder =
            ResolvableProfile.resolvableProfile()
                .name(customName != null ? "Mannequin" : "CustomMannequin");
        ProfileProperty prop =
            (textureSignature != null && !textureSignature.isBlank())
                ? new ProfileProperty("textures", textureBase64, textureSignature)
                : new ProfileProperty("textures", textureBase64);
        builder.addProperty(prop);
        return builder.build();
      }
    } catch (Throwable t) {
      // Handled gracefully in mock / test environments
    }
    return null;
  }

  private void applyResolvedProfile() {
    ResolvableProfile resolved = resolveProfile();
    if (resolved != null && entity != null) {
      entity.setProfile(resolved);
    }
  }

  // ==========================================
  // Pose
  // ==========================================

  /** Set the mannequin pose. Locks the pose automatically so gravity/physics will not reset it. */
  public CustomMannequin pose(Pose pose) {
    this.pose = pose != null ? pose : Pose.STANDING;
    if (spawned && entity != null) {
      entity.setPose(this.pose, true);
    }
    return this;
  }

  /** Get current pose. */
  public Pose pose() {
    return spawned && entity != null ? entity.getPose() : pose;
  }

  public CustomMannequin standing() {
    return pose(Pose.STANDING);
  }

  public CustomMannequin sneaking() {
    return pose(Pose.SNEAKING);
  }

  public CustomMannequin crouching() {
    return pose(Pose.SNEAKING);
  }

  public CustomMannequin swimming() {
    return pose(Pose.SWIMMING);
  }

  public CustomMannequin fallFlying() {
    return pose(Pose.FALL_FLYING);
  }

  public CustomMannequin gliding() {
    return pose(Pose.FALL_FLYING);
  }

  public CustomMannequin sleeping() {
    return pose(Pose.SLEEPING);
  }

  /** Returns all valid poses supported by Mannequins in Paper 1.21.11. */
  public static Set<Pose> validPoses() {
    try {
      return Mannequin.validPoses();
    } catch (Throwable t) {
      return Set.of(Pose.STANDING, Pose.SNEAKING, Pose.SWIMMING, Pose.FALL_FLYING, Pose.SLEEPING);
    }
  }

  // ==========================================
  // Hand & Handedness
  // ==========================================

  public CustomMannequin mainHand(MainHand hand) {
    this.mainHand = hand != null ? hand : MainHand.RIGHT;
    if (spawned && entity != null) {
      entity.setMainHand(this.mainHand);
    }
    return this;
  }

  public MainHand mainHand() {
    return spawned && entity != null ? entity.getMainHand() : mainHand;
  }

  public CustomMannequin rightHanded() {
    return mainHand(MainHand.RIGHT);
  }

  public CustomMannequin leftHanded() {
    return mainHand(MainHand.LEFT);
  }

  public boolean isRightHanded() {
    return mainHand() == MainHand.RIGHT;
  }

  public boolean isLeftHanded() {
    return mainHand() == MainHand.LEFT;
  }

  // ==========================================
  // Equipment
  // ==========================================

  public CustomMannequin helmet(ItemStack item) {
    this.helmet = item != null ? item.clone() : null;
    if (spawned && entity != null && entity.getEquipment() != null) {
      entity.getEquipment().setHelmet(this.helmet);
    }
    return this;
  }

  public CustomMannequin helmet(Material material) {
    return helmet(material != null ? new ItemStack(material) : null);
  }

  public CustomMannequin chestplate(ItemStack item) {
    this.chestplate = item != null ? item.clone() : null;
    if (spawned && entity != null && entity.getEquipment() != null) {
      entity.getEquipment().setChestplate(this.chestplate);
    }
    return this;
  }

  public CustomMannequin chestplate(Material material) {
    return chestplate(material != null ? new ItemStack(material) : null);
  }

  public CustomMannequin leggings(ItemStack item) {
    this.leggings = item != null ? item.clone() : null;
    if (spawned && entity != null && entity.getEquipment() != null) {
      entity.getEquipment().setLeggings(this.leggings);
    }
    return this;
  }

  public CustomMannequin leggings(Material material) {
    return leggings(material != null ? new ItemStack(material) : null);
  }

  public CustomMannequin boots(ItemStack item) {
    this.boots = item != null ? item.clone() : null;
    if (spawned && entity != null && entity.getEquipment() != null) {
      entity.getEquipment().setBoots(this.boots);
    }
    return this;
  }

  public CustomMannequin boots(Material material) {
    return boots(material != null ? new ItemStack(material) : null);
  }

  public CustomMannequin armor(ItemStack h, ItemStack c, ItemStack l, ItemStack b) {
    helmet(h);
    chestplate(c);
    leggings(l);
    boots(b);
    return this;
  }

  public CustomMannequin armor(Material h, Material c, Material l, Material b) {
    helmet(h);
    chestplate(c);
    leggings(l);
    boots(b);
    return this;
  }

  public CustomMannequin mainHandItem(ItemStack item) {
    this.mainHandItem = item != null ? item.clone() : null;
    if (spawned && entity != null && entity.getEquipment() != null) {
      entity.getEquipment().setItemInMainHand(this.mainHandItem);
    }
    return this;
  }

  public CustomMannequin mainHandItem(Material material) {
    return mainHandItem(material != null ? new ItemStack(material) : null);
  }

  public CustomMannequin item(ItemStack item) {
    return mainHandItem(item);
  }

  public CustomMannequin item(Material material) {
    return mainHandItem(material);
  }

  public CustomMannequin offHandItem(ItemStack item) {
    this.offHandItem = item != null ? item.clone() : null;
    if (spawned && entity != null && entity.getEquipment() != null) {
      entity.getEquipment().setItemInOffHand(this.offHandItem);
    }
    return this;
  }

  public CustomMannequin offHandItem(Material material) {
    return offHandItem(material != null ? new ItemStack(material) : null);
  }

  public CustomMannequin clearEquipment() {
    helmet((ItemStack) null);
    chestplate((ItemStack) null);
    leggings((ItemStack) null);
    boots((ItemStack) null);
    mainHandItem((ItemStack) null);
    offHandItem((ItemStack) null);
    return this;
  }

  /** Lock equipment so players cannot equip/swap items when interacting. Default is true. */
  public CustomMannequin lockEquipment(boolean lock) {
    this.equipmentLocked = lock;
    return this;
  }

  public boolean isEquipmentLocked() {
    return equipmentLocked;
  }

  public EntityEquipment equipment() {
    return spawned && entity != null ? entity.getEquipment() : null;
  }

  public ItemStack getHelmet() {
    return spawned && entity != null && entity.getEquipment() != null
        ? entity.getEquipment().getHelmet()
        : helmet;
  }

  public ItemStack getChestplate() {
    return spawned && entity != null && entity.getEquipment() != null
        ? entity.getEquipment().getChestplate()
        : chestplate;
  }

  public ItemStack getLeggings() {
    return spawned && entity != null && entity.getEquipment() != null
        ? entity.getEquipment().getLeggings()
        : leggings;
  }

  public ItemStack getBoots() {
    return spawned && entity != null && entity.getEquipment() != null
        ? entity.getEquipment().getBoots()
        : boots;
  }

  public ItemStack getMainHandItem() {
    return spawned && entity != null && entity.getEquipment() != null
        ? entity.getEquipment().getItemInMainHand()
        : mainHandItem;
  }

  public ItemStack getOffHandItem() {
    return spawned && entity != null && entity.getEquipment() != null
        ? entity.getEquipment().getItemInOffHand()
        : offHandItem;
  }

  // ==========================================
  // Skin Parts / Layers
  // ==========================================

  public CustomMannequin cape(boolean enabled) {
    this.cape = enabled;
    applySkinParts();
    return this;
  }

  public boolean hasCape() {
    return cape;
  }

  public CustomMannequin jacket(boolean enabled) {
    this.jacket = enabled;
    applySkinParts();
    return this;
  }

  public boolean hasJacket() {
    return jacket;
  }

  public CustomMannequin leftSleeve(boolean enabled) {
    this.leftSleeve = enabled;
    applySkinParts();
    return this;
  }

  public boolean hasLeftSleeve() {
    return leftSleeve;
  }

  public CustomMannequin rightSleeve(boolean enabled) {
    this.rightSleeve = enabled;
    applySkinParts();
    return this;
  }

  public boolean hasRightSleeve() {
    return rightSleeve;
  }

  public CustomMannequin sleeves(boolean enabled) {
    this.leftSleeve = enabled;
    this.rightSleeve = enabled;
    applySkinParts();
    return this;
  }

  public CustomMannequin leftPants(boolean enabled) {
    this.leftPants = enabled;
    applySkinParts();
    return this;
  }

  public boolean hasLeftPants() {
    return leftPants;
  }

  public CustomMannequin rightPants(boolean enabled) {
    this.rightPants = enabled;
    applySkinParts();
    return this;
  }

  public boolean hasRightPants() {
    return rightPants;
  }

  public CustomMannequin pants(boolean enabled) {
    this.leftPants = enabled;
    this.rightPants = enabled;
    applySkinParts();
    return this;
  }

  public CustomMannequin hat(boolean enabled) {
    this.hats = enabled;
    applySkinParts();
    return this;
  }

  public boolean hasHat() {
    return hats;
  }

  public CustomMannequin allSkinParts() {
    this.cape = true;
    this.jacket = true;
    this.leftSleeve = true;
    this.rightSleeve = true;
    this.leftPants = true;
    this.rightPants = true;
    this.hats = true;
    applySkinParts();
    return this;
  }

  public CustomMannequin noSkinParts() {
    this.cape = false;
    this.jacket = false;
    this.leftSleeve = false;
    this.rightSleeve = false;
    this.leftPants = false;
    this.rightPants = false;
    this.hats = false;
    applySkinParts();
    return this;
  }

  public CustomMannequin skinParts(Consumer<SkinParts.Mutable> consumer) {
    if (spawned && entity != null) {
      SkinParts.Mutable parts = entity.getSkinParts();
      if (parts != null) {
        consumer.accept(parts);
        entity.setSkinParts(parts);
        readSkinParts(parts);
      }
    } else {
      SkinParts.Mutable dummy = SkinParts.allParts();
      dummy.setCapeEnabled(cape);
      dummy.setJacketEnabled(jacket);
      dummy.setLeftSleeveEnabled(leftSleeve);
      dummy.setRightSleeveEnabled(rightSleeve);
      dummy.setLeftPantsEnabled(leftPants);
      dummy.setRightPantsEnabled(rightPants);
      dummy.setHatsEnabled(hats);
      consumer.accept(dummy);
      readSkinParts(dummy);
    }
    return this;
  }

  public CustomMannequin skinParts(SkinParts parts) {
    if (parts != null) {
      readSkinParts(parts);
      if (spawned && entity != null) {
        entity.setSkinParts(parts);
      }
    }
    return this;
  }

  public SkinParts skinParts() {
    return spawned && entity != null ? entity.getSkinParts() : null;
  }

  private void readSkinParts(SkinParts parts) {
    this.cape = parts.hasCapeEnabled();
    this.jacket = parts.hasJacketEnabled();
    this.leftSleeve = parts.hasLeftSleeveEnabled();
    this.rightSleeve = parts.hasRightSleeveEnabled();
    this.leftPants = parts.hasLeftPantsEnabled();
    this.rightPants = parts.hasRightPantsEnabled();
    this.hats = parts.hasHatsEnabled();
  }

  private void applySkinParts() {
    if (spawned && entity != null) {
      SkinParts.Mutable parts = entity.getSkinParts();
      if (parts != null) {
        parts.setCapeEnabled(cape);
        parts.setJacketEnabled(jacket);
        parts.setLeftSleeveEnabled(leftSleeve);
        parts.setRightSleeveEnabled(rightSleeve);
        parts.setLeftPantsEnabled(leftPants);
        parts.setRightPantsEnabled(rightPants);
        parts.setHatsEnabled(hats);
        entity.setSkinParts(parts);
      }
    }
  }

  // ==========================================
  // Physics, Safety & Movement
  // ==========================================

  public CustomMannequin immovable(boolean immovable) {
    this.immovable = immovable;
    if (spawned && entity != null) {
      entity.setImmovable(immovable);
    }
    return this;
  }

  public CustomMannequin immovable() {
    return immovable(true);
  }

  public boolean isImmovable() {
    return spawned && entity != null ? entity.isImmovable() : immovable;
  }

  public CustomMannequin invulnerable(boolean invulnerable) {
    this.invulnerable = invulnerable;
    if (spawned && entity != null) {
      entity.setInvulnerable(invulnerable);
    }
    return this;
  }

  public CustomMannequin invulnerable() {
    return invulnerable(true);
  }

  public boolean isInvulnerable() {
    return spawned && entity != null ? entity.isInvulnerable() : invulnerable;
  }

  public CustomMannequin gravity(boolean gravity) {
    this.gravity = gravity;
    if (spawned && entity != null) {
      entity.setGravity(gravity);
    }
    return this;
  }

  public CustomMannequin gravity() {
    return gravity(true);
  }

  public boolean hasGravity() {
    return spawned && entity != null ? entity.hasGravity() : gravity;
  }

  public CustomMannequin silent(boolean silent) {
    this.silent = silent;
    if (spawned && entity != null) {
      entity.setSilent(silent);
    }
    return this;
  }

  public CustomMannequin silent() {
    return silent(true);
  }

  public boolean isSilent() {
    return spawned && entity != null ? entity.isSilent() : silent;
  }

  public CustomMannequin collidable(boolean collidable) {
    this.collidable = collidable;
    if (spawned && entity != null) {
      entity.setCollidable(collidable);
    }
    return this;
  }

  public boolean isCollidable() {
    return spawned && entity != null ? entity.isCollidable() : collidable;
  }

  public CustomMannequin glowing(boolean glowing) {
    this.glowing = glowing;
    if (spawned && entity != null) {
      entity.setGlowing(glowing);
    }
    return this;
  }

  public boolean isGlowing() {
    return spawned && entity != null ? entity.isGlowing() : glowing;
  }

  public CustomMannequin invisible(boolean invisible) {
    this.invisible = invisible;
    if (spawned && entity != null) {
      entity.setInvisible(invisible);
    }
    return this;
  }

  public CustomMannequin visible(boolean visible) {
    return invisible(!visible);
  }

  public boolean isInvisible() {
    return spawned && entity != null ? entity.isInvisible() : invisible;
  }

  public CustomMannequin persistent(boolean persistent) {
    this.persistent = persistent;
    if (spawned && entity != null) {
      entity.setPersistent(persistent);
    }
    return this;
  }

  public boolean isPersistent() {
    return spawned && entity != null ? entity.isPersistent() : persistent;
  }

  // ==========================================
  // Rotation & Looking
  // ==========================================

  public CustomMannequin rotation(float yaw, float pitch) {
    if (spawned && entity != null) {
      entity.setRotation(yaw, pitch);
      this.location = entity.getLocation();
    } else if (location != null) {
      location.setYaw(yaw);
      location.setPitch(pitch);
    }
    return this;
  }

  public CustomMannequin yaw(float yaw) {
    return rotation(yaw, location != null ? location.getPitch() : 0.0f);
  }

  public CustomMannequin pitch(float pitch) {
    return rotation(location != null ? location.getYaw() : 0.0f, pitch);
  }

  public CustomMannequin lookAt(Location target) {
    if (target == null) return this;
    Location cur = location != null ? location : (entity != null ? entity.getLocation() : null);
    if (cur == null || cur.getWorld() == null || !cur.getWorld().equals(target.getWorld())) {
      return this;
    }
    Vector dir = target.toVector().subtract(cur.toVector());
    if (dir.lengthSquared() < 0.0001) return this;
    double dx = dir.getX();
    double dy = dir.getY();
    double dz = dir.getZ();
    double distXZ = Math.sqrt(dx * dx + dz * dz);
    float yaw = (float) Math.toDegrees(-Math.atan2(dx, dz));
    float pitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));
    return rotation(yaw, pitch);
  }

  public CustomMannequin lookAt(Player player) {
    return player != null ? lookAt(player.getEyeLocation()) : this;
  }

  public CustomMannequin lookAt(Entity targetEntity) {
    return targetEntity != null ? lookAt(targetEntity.getLocation()) : this;
  }

  public CustomMannequin lookAt(double x, double y, double z) {
    Location cur = location();
    if (cur != null && cur.getWorld() != null) {
      return lookAt(new Location(cur.getWorld(), x, y, z));
    }
    return this;
  }

  // ==========================================
  // Interactions & Actions
  // ==========================================

  public CustomMannequin onClick(MannequinClickCallback callback) {
    if (callback != null) {
      clickCallbacks.add(callback);
    }
    return this;
  }

  public CustomMannequin onAttack(MannequinAttackCallback callback) {
    if (callback != null) {
      attackCallbacks.add(callback);
    }
    return this;
  }

  public CustomMannequin onInteract(MannequinInteractCallback callback) {
    if (callback != null) {
      interactCallbacks.add(callback);
    }
    return this;
  }

  public CustomMannequin clearClickActions() {
    clickCallbacks.clear();
    attackCallbacks.clear();
    interactCallbacks.clear();
    return this;
  }

  public void handleClick(Player player) {
    for (MannequinClickCallback callback : clickCallbacks) {
      try {
        callback.onClick(player, this);
      } catch (Throwable t) {
        plugin.getLogger().warning("Error executing mannequin click callback: " + t.getMessage());
      }
    }
  }

  public void handleAttack(Player player) {
    for (MannequinAttackCallback callback : attackCallbacks) {
      try {
        callback.onAttack(player, this);
      } catch (Throwable t) {
        plugin.getLogger().warning("Error executing mannequin attack callback: " + t.getMessage());
      }
    }
  }

  public void handleInteract(Player player, EquipmentSlot hand) {
    for (MannequinInteractCallback callback : interactCallbacks) {
      try {
        callback.onInteract(player, this, hand);
      } catch (Throwable t) {
        plugin
            .getLogger()
            .warning("Error executing mannequin interact callback: " + t.getMessage());
      }
    }
  }

  // ==========================================
  // Visual Effects & Animations
  // ==========================================

  public CustomMannequin swingMainHand() {
    if (spawned && entity != null) {
      entity.swingMainHand();
    }
    return this;
  }

  public CustomMannequin swingOffHand() {
    if (spawned && entity != null) {
      entity.swingOffHand();
    }
    return this;
  }

  public CustomMannequin playEffect(EntityEffect effect) {
    if (spawned && entity != null && effect != null) {
      entity.playEffect(effect);
    }
    return this;
  }

  // ==========================================
  // Tags & Persistent Data Container (PDC)
  // ==========================================

  public CustomMannequin tag(String tag) {
    if (tag != null && !tag.isBlank()) {
      tags.add(tag);
      if (spawned && entity != null) {
        entity.addScoreboardTag(tag);
      }
    }
    return this;
  }

  public CustomMannequin removeTag(String tag) {
    if (tag != null) {
      tags.remove(tag);
      if (spawned && entity != null) {
        entity.removeScoreboardTag(tag);
      }
    }
    return this;
  }

  public boolean hasTag(String tag) {
    return spawned && entity != null
        ? entity.getScoreboardTags().contains(tag)
        : tags.contains(tag);
  }

  public Set<String> tags() {
    return spawned && entity != null
        ? entity.getScoreboardTags()
        : Collections.unmodifiableSet(tags);
  }

  public CustomMannequin pdc(String key, String value) {
    if (spawned && entity != null && key != null && value != null) {
      NamespacedKey nsKey = new NamespacedKey(plugin, key.toLowerCase());
      entity.getPersistentDataContainer().set(nsKey, PersistentDataType.STRING, value);
    }
    return this;
  }

  public CustomMannequin pdc(String key, int value) {
    if (spawned && entity != null && key != null) {
      NamespacedKey nsKey = new NamespacedKey(plugin, key.toLowerCase());
      entity.getPersistentDataContainer().set(nsKey, PersistentDataType.INTEGER, value);
    }
    return this;
  }

  public String getPdcString(String key) {
    if (spawned && entity != null && key != null) {
      NamespacedKey nsKey = new NamespacedKey(plugin, key.toLowerCase());
      return entity.getPersistentDataContainer().get(nsKey, PersistentDataType.STRING);
    }
    return null;
  }

  // ==========================================
  // Location & Teleport
  // ==========================================

  public Location location() {
    return spawned && entity != null
        ? entity.getLocation()
        : (location != null ? location.clone() : null);
  }

  public CustomMannequin location(Location loc) {
    this.location = loc != null ? loc.clone() : null;
    if (spawned && entity != null && location != null) {
      teleport(location);
    }
    return this;
  }

  public boolean teleport(Location targetLocation) {
    if (targetLocation == null) return false;
    this.location = targetLocation.clone();
    if (spawned && entity != null) {
      return entity.teleport(targetLocation);
    }
    return true;
  }

  public CompletableFuture<Boolean> teleportAsync(Location targetLocation) {
    if (targetLocation == null) return CompletableFuture.completedFuture(false);
    this.location = targetLocation.clone();
    if (spawned && entity != null) {
      return entity.teleportAsync(targetLocation);
    }
    return CompletableFuture.completedFuture(true);
  }

  // ==========================================
  // Lifecycle: Spawn & Remove
  // ==========================================

  /** Spawn this mannequin into the world. */
  public CustomMannequin spawn() {
    if (spawned && entity != null && entity.isValid()) {
      return this;
    }

    if (location == null || location.getWorld() == null) {
      plugin.getLogger().warning("Cannot spawn mannequin: location or world is null!");
      return this;
    }

    try {
      this.entity =
          location
              .getWorld()
              .spawn(
                  location,
                  Mannequin.class,
                  m -> {
                    // Name & Visibility
                    if (customName != null) {
                      m.customName(customName);
                      m.setCustomNameVisible(customNameVisible);
                    }

                    // Description
                    if (description != null) {
                      m.setDescription(description);
                    }

                    // Profile / Skin
                    ResolvableProfile resolved = resolveProfile();
                    if (resolved != null) {
                      m.setProfile(resolved);
                    }

                    // Pose
                    m.setPose(pose, true);

                    // Hand
                    m.setMainHand(mainHand);

                    // Physics & AI
                    m.setImmovable(immovable);
                    m.setInvulnerable(invulnerable);
                    m.setGravity(gravity);
                    m.setSilent(silent);
                    m.setCollidable(collidable);
                    m.setGlowing(glowing);
                    m.setInvisible(invisible);
                    m.setPersistent(persistent);

                    // Skin parts
                    SkinParts.Mutable parts = m.getSkinParts();
                    if (parts != null) {
                      parts.setCapeEnabled(cape);
                      parts.setJacketEnabled(jacket);
                      parts.setLeftSleeveEnabled(leftSleeve);
                      parts.setRightSleeveEnabled(rightSleeve);
                      parts.setLeftPantsEnabled(leftPants);
                      parts.setRightPantsEnabled(rightPants);
                      parts.setHatsEnabled(hats);
                      m.setSkinParts(parts);
                    }

                    // Equipment
                    EntityEquipment eq = m.getEquipment();
                    if (eq != null) {
                      if (helmet != null) eq.setHelmet(helmet);
                      if (chestplate != null) eq.setChestplate(chestplate);
                      if (leggings != null) eq.setLeggings(leggings);
                      if (boots != null) eq.setBoots(boots);
                      if (mainHandItem != null) eq.setItemInMainHand(mainHandItem);
                      if (offHandItem != null) eq.setItemInOffHand(offHandItem);
                    }

                    // Scoreboard tags
                    for (String tag : tags) {
                      m.addScoreboardTag(tag);
                    }
                  });

      this.spawned = true;
      MannequinListener.register(this);
      if (helper != null) {
        helper.track(this);
      }
    } catch (Throwable t) {
      plugin.getLogger().severe("Failed to spawn Mannequin at " + location + ": " + t.getMessage());
    }

    return this;
  }

  /** Remove and despawn the mannequin from the world. */
  public void remove() {
    MannequinListener.unregister(this);
    if (helper != null) {
      helper.untrack(this);
    }
    if (entity != null) {
      try {
        entity.remove();
      } catch (Throwable ignored) {
      }
      entity = null;
    }
    spawned = false;
  }

  /** Alias for {@link #remove()}. */
  public void destroy() {
    remove();
  }

  public boolean isSpawned() {
    return spawned && entity != null && entity.isValid();
  }

  public boolean isValid() {
    return isSpawned();
  }

  public Mannequin getEntity() {
    return entity;
  }

  public Mannequin getBukkitEntity() {
    return entity;
  }

  public Mannequin entity() {
    return entity;
  }

  public Mannequin get() {
    return entity;
  }

  public UUID getUniqueId() {
    return entity != null ? entity.getUniqueId() : null;
  }

  public UUID getId() {
    return getUniqueId();
  }
}
