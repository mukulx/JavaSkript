package dev.mukulx.javaskript.api.advancement;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.AdvancementHelper;
import io.papermc.paper.advancement.AdvancementDisplay;
import java.util.Collection;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Static shorthand facade for PaperMC Advancements.
 *
 * <p>Enables 1-line custom achievements, instant toasts, and progression tracking:
 *
 * <pre>{@code
 * // 1. Instant Toast Popup
 * Advancements.toast(player, Material.DIAMOND, "<gold><bold>Quest Complete!</bold></gold>", Frame.CHALLENGE);
 *
 * // 2. Fluent Custom Advancement
 * Advancements.create("dragon_hunter")
 *     .title("<green>Dragon Hunter</green>")
 *     .description("Slay the mighty Ender Dragon")
 *     .icon(Material.DRAGON_HEAD)
 *     .challenge()
 *     .register();
 *
 * // 3. Grant / Check
 * Advancements.grant(player, "dragon_hunter");
 * if (Advancements.has(player, "dragon_hunter")) {
 *     player.sendMessage("You are a legendary hunter!");
 * }
 * }</pre>
 */
public final class Advancements {

  private static AdvancementHelper helper;

  private Advancements() {}

  private static AdvancementHelper get() {
    if (helper == null) {
      helper = new AdvancementHelper(JavaSkriptPlugin.getInstance(), "global");
    }
    return helper;
  }

  public static void setInstance(AdvancementHelper h) {
    helper = h;
  }

  public static CustomAdvancement create(String key) {
    return get().create(key);
  }

  public static CustomAdvancement create(NamespacedKey key) {
    return get().create(key);
  }

  public static CustomAdvancement createRoot(String key, String backgroundTexture) {
    return get().createRoot(key, backgroundTexture);
  }

  public static Advancement register(CustomAdvancement custom) {
    return get().register(custom);
  }

  public static boolean unregister(NamespacedKey key) {
    return get().unregister(key);
  }

  public static boolean unregister(String key) {
    return get().unregister(key);
  }

  public static boolean unregister(Advancement advancement) {
    return get().unregister(advancement);
  }

  // --- Toasts ---

  public static void toast(
      Player player, ItemStack icon, Component title, AdvancementDisplay.Frame frame) {
    get().toast(player, icon, title, frame);
  }

  public static void toast(
      Player player, Material icon, Component title, AdvancementDisplay.Frame frame) {
    get().toast(player, icon, title, frame);
  }

  public static void toast(
      Player player, Material icon, String title, AdvancementDisplay.Frame frame) {
    get().toast(player, icon, title, frame);
  }

  public static void toast(Player player, Material icon, String title) {
    get().toast(player, icon, title);
  }

  public static void toast(Player player, String title, AdvancementDisplay.Frame frame) {
    get().toast(player, title, frame);
  }

  public static void toast(Player player, String title) {
    get().toast(player, title);
  }

  public static void toastAll(Material icon, Component title, AdvancementDisplay.Frame frame) {
    get().toastAll(icon, title, frame);
  }

  public static void toastAll(Material icon, String title, AdvancementDisplay.Frame frame) {
    get().toastAll(icon, title, frame);
  }

  // --- Progression ---

  public static boolean grant(Player player, NamespacedKey key) {
    return get().grant(player, key);
  }

  public static boolean grant(Player player, String key) {
    return get().grant(player, key);
  }

  public static boolean grant(Player player, Advancement advancement) {
    return get().grant(player, advancement);
  }

  public static boolean grant(Player player, NamespacedKey key, String criterion) {
    return get().grant(player, key, criterion);
  }

  public static boolean grant(Player player, String key, String criterion) {
    return get().grant(player, key, criterion);
  }

  public static void grantAll(NamespacedKey key) {
    get().grantAll(key);
  }

  public static boolean revoke(Player player, NamespacedKey key) {
    return get().revoke(player, key);
  }

  public static boolean revoke(Player player, String key) {
    return get().revoke(player, key);
  }

  public static boolean revoke(Player player, Advancement advancement) {
    return get().revoke(player, advancement);
  }

  public static boolean revoke(Player player, NamespacedKey key, String criterion) {
    return get().revoke(player, key, criterion);
  }

  public static boolean revoke(Player player, String key, String criterion) {
    return get().revoke(player, key, criterion);
  }

  public static boolean has(Player player, NamespacedKey key) {
    return get().has(player, key);
  }

  public static boolean has(Player player, String key) {
    return get().has(player, key);
  }

  public static boolean has(Player player, Advancement advancement) {
    return get().has(player, advancement);
  }

  public static boolean hasCriterion(Player player, NamespacedKey key, String criterion) {
    return get().hasCriterion(player, key, criterion);
  }

  public static AdvancementProgress getProgress(Player player, NamespacedKey key) {
    return get().getProgress(player, key);
  }

  public static AdvancementProgress getProgress(Player player, String key) {
    return get().getProgress(player, key);
  }

  public static double getProgressPercent(Player player, NamespacedKey key) {
    return get().getProgressPercent(player, key);
  }

  public static Advancement get(NamespacedKey key) {
    return get().get(key);
  }

  public static Advancement get(String key) {
    return get().get(key);
  }

  public static Collection<NamespacedKey> getRegisteredKeys() {
    return get().getRegisteredKeys();
  }

  public static void onComplete(NamespacedKey key, BiConsumer<Player, Advancement> handler) {
    get().onComplete(key, handler);
  }

  public static void onComplete(String key, BiConsumer<Player, Advancement> handler) {
    get().onComplete(key, handler);
  }

  public static void onAnyComplete(BiConsumer<Player, Advancement> handler) {
    get().onAnyComplete(handler);
  }
}
