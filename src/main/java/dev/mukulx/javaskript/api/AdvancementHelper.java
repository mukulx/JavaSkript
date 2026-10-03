package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.advancement.CustomAdvancement;
import dev.mukulx.javaskript.util.ServerUtil;
import dev.mukulx.javaskript.util.TextUtil;
import io.papermc.paper.advancement.AdvancementDisplay;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Universal Advancement subsystem helper for Paper and Folia 1.21.11+ servers.
 *
 * <p>Enables 1-line custom toasts, runtime achievement registration, progression management, and
 * automatic reload/unload cleanup.
 */
@SuppressWarnings("deprecation")
public class AdvancementHelper {

  private static final Set<AdvancementHelper> ACTIVE_HELPERS = ConcurrentHashMap.newKeySet();

  private final JavaSkriptPlugin plugin;
  private final String scriptName;

  private final Set<NamespacedKey> registeredAdvancements = ConcurrentHashMap.newKeySet();
  private final Set<NamespacedKey> pendingToasts = ConcurrentHashMap.newKeySet();
  private final Map<NamespacedKey, BiConsumer<Player, Advancement>> completionHandlers =
      new ConcurrentHashMap<>();
  private final List<BiConsumer<Player, Advancement>> globalCompletionHandlers =
      new CopyOnWriteArrayList<>();

  public AdvancementHelper(JavaSkriptPlugin plugin) {
    this(plugin, "api");
  }

  public AdvancementHelper(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName =
        scriptName != null ? scriptName.replace(".java", "").toLowerCase(Locale.ROOT) : "api";
    ACTIVE_HELPERS.add(this);
  }

  /** Internal dispatcher called by AdvancementListener when any player completes an advancement. */
  public static void dispatchDone(Player player, Advancement advancement) {
    if (player == null || advancement == null) return;
    NamespacedKey key = advancement.getKey();
    for (AdvancementHelper helper : ACTIVE_HELPERS) {
      BiConsumer<Player, Advancement> handler = helper.completionHandlers.get(key);
      if (handler != null) {
        try {
          handler.accept(player, advancement);
        } catch (Throwable t) {
          helper
              .plugin
              .getLogger()
              .warning("Error executing advancement handler for " + key + ": " + t.getMessage());
        }
      }
      for (BiConsumer<Player, Advancement> global : helper.globalCompletionHandlers) {
        try {
          global.accept(player, advancement);
        } catch (Throwable t) {
          helper
              .plugin
              .getLogger()
              .warning("Error executing global advancement handler: " + t.getMessage());
        }
      }
    }
  }

  /** Create a NamespacedKey prefixed by this script's name to avoid cross-script collisions. */
  public NamespacedKey createKey(String key) {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Advancement key cannot be empty");
    }
    if (key.contains(":")) {
      NamespacedKey parsed = NamespacedKey.fromString(key);
      if (parsed != null) {
        return parsed;
      }
    }
    String subKey =
        (scriptName.equals("api") || scriptName.equals("global")) ? key : scriptName + "_" + key;
    String cleanSubKey = subKey.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    String namespace =
        (plugin != null && plugin.getName() != null)
            ? plugin.getName().toLowerCase(Locale.ROOT)
            : "javaskript";
    return new NamespacedKey(namespace, cleanSubKey);
  }

  /** Create a new fluent CustomAdvancement builder with an auto-namespaced key. */
  public CustomAdvancement create(String key) {
    return new CustomAdvancement(plugin, this, createKey(key));
  }

  /** Create a new fluent CustomAdvancement builder with an explicit NamespacedKey. */
  public CustomAdvancement create(NamespacedKey key) {
    return new CustomAdvancement(plugin, this, key);
  }

  /** Create a new root advancement tab with a custom background texture. */
  public CustomAdvancement createRoot(String key, String backgroundTexture) {
    return create(key).background(backgroundTexture);
  }

  /** Register a custom advancement on the server. */
  public Advancement register(CustomAdvancement custom) {
    if (custom == null) {
      throw new IllegalArgumentException("CustomAdvancement cannot be null");
    }
    NamespacedKey key = custom.key();
    String json = custom.toJson();

    try {
      Bukkit.getUnsafe().removeAdvancement(key);
    } catch (Throwable ignored) {
    }

    Advancement adv = Bukkit.getUnsafe().loadAdvancement(key, json);
    if (adv != null) {
      registeredAdvancements.add(key);
      if (custom.hasCompletionHandler()) {
        completionHandlers.put(key, custom.getCompletionHandler());
      }
      try {
        Bukkit.updateResources();
      } catch (Throwable ignored) {
      }
    }
    return adv;
  }

  /** Unregister a custom advancement by NamespacedKey. */
  public boolean unregister(NamespacedKey key) {
    if (key == null) return false;
    completionHandlers.remove(key);
    registeredAdvancements.remove(key);
    boolean removed = false;
    try {
      removed = Bukkit.getUnsafe().removeAdvancement(key);
      Bukkit.updateResources();
    } catch (Throwable ignored) {
    }
    return removed;
  }

  /** Unregister a custom advancement by string key. */
  public boolean unregister(String key) {
    return unregister(createKey(key));
  }

  /** Unregister a custom advancement instance. */
  public boolean unregister(Advancement advancement) {
    return advancement != null && unregister(advancement.getKey());
  }

  /** Unregister all advancements created by this script and remove from connected players. */
  public void removeAll() {
    ACTIVE_HELPERS.remove(this);

    // Clean up temporary toast advancements
    for (NamespacedKey key : new ArrayList<>(pendingToasts)) {
      try {
        Bukkit.getUnsafe().removeAdvancement(key);
      } catch (Throwable ignored) {
      }
    }
    pendingToasts.clear();

    // Revoke registered script advancements from online players
    for (Player player : Bukkit.getOnlinePlayers()) {
      for (NamespacedKey key : registeredAdvancements) {
        try {
          Advancement adv = Bukkit.getAdvancement(key);
          if (adv != null) {
            AdvancementProgress progress = player.getAdvancementProgress(adv);
            for (String c : progress.getAwardedCriteria()) {
              progress.revokeCriteria(c);
            }
          }
        } catch (Throwable ignored) {
        }
      }
    }

    // Remove advancements from server registry
    for (NamespacedKey key : new ArrayList<>(registeredAdvancements)) {
      try {
        Bukkit.getUnsafe().removeAdvancement(key);
      } catch (Throwable t) {
        plugin.getLogger().warning("Failed to remove advancement " + key + ": " + t.getMessage());
      }
    }
    registeredAdvancements.clear();
    completionHandlers.clear();
    globalCompletionHandlers.clear();

    try {
      Bukkit.updateResources();
    } catch (Throwable ignored) {
    }

    plugin.debug("Removed all custom advancements for script: " + scriptName);
  }

  // ==========================================
  // TOAST NOTIFICATIONS (Top-Right Popups)
  // ==========================================

  /**
   * Send an instant advancement toast popup to a player.
   *
   * @param player Target player
   * @param icon Item icon displayed on the toast
   * @param title Adventure Component title
   * @param frame Frame styling (TASK, GOAL, CHALLENGE)
   */
  public void toast(
      Player player, ItemStack icon, Component title, AdvancementDisplay.Frame frame) {
    if (player == null || !player.isOnline()) return;
    if (frame == null) frame = AdvancementDisplay.Frame.TASK;

    String randomId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    NamespacedKey key = createKey("toast_" + randomId);
    pendingToasts.add(key);

    CustomAdvancement builder =
        new CustomAdvancement(plugin, this, key)
            .title(title)
            .description(Component.empty())
            .frame(frame)
            .icon(icon)
            .toast(true)
            .announce(false)
            .hidden(true);

    String json = builder.toJson();

    Runnable showAction =
        () -> {
          try {
            Advancement adv = Bukkit.getUnsafe().loadAdvancement(key, json);
            if (adv != null) {
              AdvancementProgress progress = player.getAdvancementProgress(adv);
              for (String c : progress.getRemainingCriteria()) {
                progress.awardCriteria(c);
              }
              scheduleToastCleanup(player, key);
            }
          } catch (Throwable t) {
            plugin.debug("Failed to send toast to " + player.getName() + ": " + t.getMessage());
          }
        };

    if (Bukkit.isOwnedByCurrentRegion(player)) {
      showAction.run();
    } else {
      ServerUtil.runForPlayer(plugin, player, showAction);
    }
  }

  /** Send a toast popup with a Material icon and Component title. */
  public void toast(Player player, Material icon, Component title, AdvancementDisplay.Frame frame) {
    toast(player, new ItemStack(icon != null ? icon : Material.DIAMOND), title, frame);
  }

  /** Send a toast popup with a Material icon, MiniMessage/legacy string title, and Frame. */
  public void toast(Player player, Material icon, String title, AdvancementDisplay.Frame frame) {
    toast(player, icon, TextUtil.parse(title), frame);
  }

  /** Send a toast popup with a Material icon and MiniMessage/legacy string title (default TASK). */
  public void toast(Player player, Material icon, String title) {
    toast(player, icon, title, AdvancementDisplay.Frame.TASK);
  }

  /**
   * Send a toast popup with a MiniMessage/legacy string title (default DIAMOND icon and TASK
   * frame).
   */
  public void toast(Player player, String title, AdvancementDisplay.Frame frame) {
    toast(player, Material.DIAMOND, title, frame);
  }

  /**
   * Send a toast popup with a MiniMessage/legacy string title (default DIAMOND icon and TASK
   * frame).
   */
  public void toast(Player player, String title) {
    toast(player, Material.DIAMOND, title, AdvancementDisplay.Frame.TASK);
  }

  /** Broadcast a toast popup to all online players. */
  public void toastAll(Material icon, Component title, AdvancementDisplay.Frame frame) {
    ItemStack item = new ItemStack(icon != null ? icon : Material.DIAMOND);
    for (Player player : Bukkit.getOnlinePlayers()) {
      toast(player, item, title, frame);
    }
  }

  /** Broadcast a toast popup to all online players with MiniMessage formatting. */
  public void toastAll(Material icon, String title, AdvancementDisplay.Frame frame) {
    toastAll(icon, TextUtil.parse(title), frame);
  }

  private void scheduleToastCleanup(Player player, NamespacedKey key) {
    long delayTicks = 50L; // 2.5 seconds
    Runnable cleanup =
        () -> {
          try {
            if (player.isOnline()) {
              Advancement adv = Bukkit.getAdvancement(key);
              if (adv != null) {
                AdvancementProgress progress = player.getAdvancementProgress(adv);
                for (String c : progress.getAwardedCriteria()) {
                  progress.revokeCriteria(c);
                }
              }
            }
          } catch (Throwable ignored) {
          }
          try {
            Bukkit.getUnsafe().removeAdvancement(key);
          } catch (Throwable ignored) {
          }
          pendingToasts.remove(key);
        };

    if (ServerUtil.isFolia()) {
      try {
        player.getScheduler().runDelayed(plugin, task -> cleanup.run(), null, delayTicks);
        return;
      } catch (Throwable ignored) {
      }
    }
    ServerUtil.runLaterSync(plugin, cleanup, delayTicks);
  }

  // ==========================================
  // PROGRESSION (Grant / Revoke / Check)
  // ==========================================

  /** Grant all remaining criteria for an advancement to a player. */
  public boolean grant(Player player, NamespacedKey key) {
    if (player == null || key == null) return false;
    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) return false;
    return grant(player, adv);
  }

  /** Grant all remaining criteria for an advancement to a player. */
  public boolean grant(Player player, String key) {
    return grant(player, createKey(key));
  }

  /** Grant all remaining criteria for an advancement to a player. */
  public boolean grant(Player player, Advancement advancement) {
    if (player == null || advancement == null) return false;
    AdvancementProgress progress = player.getAdvancementProgress(advancement);
    if (progress.isDone()) return false;
    for (String criteria : progress.getRemainingCriteria()) {
      progress.awardCriteria(criteria);
    }
    return true;
  }

  /** Grant a specific criterion to a player. */
  public boolean grant(Player player, NamespacedKey key, String criterion) {
    if (player == null || key == null || criterion == null) return false;
    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) return false;
    AdvancementProgress progress = player.getAdvancementProgress(adv);
    return progress.awardCriteria(criterion);
  }

  /** Grant a specific criterion to a player by string key. */
  public boolean grant(Player player, String key, String criterion) {
    return grant(player, createKey(key), criterion);
  }

  /** Grant an advancement to all online players. */
  public void grantAll(NamespacedKey key) {
    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) return;
    for (Player player : Bukkit.getOnlinePlayers()) {
      grant(player, adv);
    }
  }

  /** Revoke all awarded criteria for an advancement from a player. */
  public boolean revoke(Player player, NamespacedKey key) {
    if (player == null || key == null) return false;
    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) return false;
    return revoke(player, adv);
  }

  /** Revoke all awarded criteria for an advancement from a player. */
  public boolean revoke(Player player, String key) {
    return revoke(player, createKey(key));
  }

  /** Revoke all awarded criteria for an advancement from a player. */
  public boolean revoke(Player player, Advancement advancement) {
    if (player == null || advancement == null) return false;
    AdvancementProgress progress = player.getAdvancementProgress(advancement);
    Collection<String> awarded = progress.getAwardedCriteria();
    if (awarded.isEmpty()) return false;
    for (String criteria : new ArrayList<>(awarded)) {
      progress.revokeCriteria(criteria);
    }
    return true;
  }

  /** Revoke a specific criterion from a player. */
  public boolean revoke(Player player, NamespacedKey key, String criterion) {
    if (player == null || key == null || criterion == null) return false;
    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) return false;
    return player.getAdvancementProgress(adv).revokeCriteria(criterion);
  }

  /** Revoke a specific criterion from a player by string key. */
  public boolean revoke(Player player, String key, String criterion) {
    return revoke(player, createKey(key), criterion);
  }

  /** Check if a player has completed all criteria of an advancement. */
  public boolean has(Player player, NamespacedKey key) {
    if (player == null || key == null) return false;
    Advancement adv = Bukkit.getAdvancement(key);
    return adv != null && player.getAdvancementProgress(adv).isDone();
  }

  /** Check if a player has completed all criteria of an advancement by string key. */
  public boolean has(Player player, String key) {
    return has(player, createKey(key));
  }

  /** Check if a player has completed all criteria of an advancement instance. */
  public boolean has(Player player, Advancement advancement) {
    return player != null
        && advancement != null
        && player.getAdvancementProgress(advancement).isDone();
  }

  /** Check if a player has been awarded a specific criterion. */
  public boolean hasCriterion(Player player, NamespacedKey key, String criterion) {
    if (player == null || key == null || criterion == null) return false;
    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) return false;
    return player.getAdvancementProgress(adv).getAwardedCriteria().contains(criterion);
  }

  /** Get the AdvancementProgress for a player. */
  public AdvancementProgress getProgress(Player player, NamespacedKey key) {
    if (player == null || key == null) return null;
    Advancement adv = Bukkit.getAdvancement(key);
    return adv != null ? player.getAdvancementProgress(adv) : null;
  }

  /** Get the AdvancementProgress for a player by string key. */
  public AdvancementProgress getProgress(Player player, String key) {
    return getProgress(player, createKey(key));
  }

  /** Get the percentage completion of an advancement for a player (0.0 to 100.0). */
  public double getProgressPercent(Player player, NamespacedKey key) {
    if (player == null || key == null) return 0.0;
    Advancement adv = Bukkit.getAdvancement(key);
    if (adv == null) return 0.0;
    AdvancementProgress progress = player.getAdvancementProgress(adv);
    int total = adv.getCriteria().size();
    if (total == 0) return progress.isDone() ? 100.0 : 0.0;
    return ((double) progress.getAwardedCriteria().size() / total) * 100.0;
  }

  /** Lookup a loaded advancement by NamespacedKey. */
  public Advancement get(NamespacedKey key) {
    return key != null ? Bukkit.getAdvancement(key) : null;
  }

  /** Lookup a loaded advancement by string key. */
  public Advancement get(String key) {
    return get(createKey(key));
  }

  /** Get all advancement keys registered by this script. */
  public Collection<NamespacedKey> getRegisteredKeys() {
    return Collections.unmodifiableSet(registeredAdvancements);
  }

  /** Register a completion handler for a specific advancement key. */
  public void onComplete(NamespacedKey key, BiConsumer<Player, Advancement> handler) {
    if (key != null && handler != null) {
      completionHandlers.put(key, handler);
    }
  }

  /** Register a completion handler for a specific advancement by string key. */
  public void onComplete(String key, BiConsumer<Player, Advancement> handler) {
    onComplete(createKey(key), handler);
  }

  /** Register a global completion handler for any advancement completed on the server. */
  public void onAnyComplete(BiConsumer<Player, Advancement> handler) {
    if (handler != null) {
      globalCompletionHandlers.add(handler);
    }
  }
}
