package dev.mukulx.javaskript.api.advancement;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.AdvancementHelper;
import dev.mukulx.javaskript.util.TextUtil;
import io.papermc.paper.advancement.AdvancementDisplay;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Fluent builder for creating custom Minecraft advancements via PaperMC 1.21.11+ APIs.
 *
 * <p>Supports full Adventure rich text components, MiniMessage, custom icons, frames (Task, Goal,
 * Challenge), criteria trees, and automatic JSON generation for runtime loading.
 */
public class CustomAdvancement {

  public static final class Backgrounds {
    public static final String ADVENTURE =
        "minecraft:textures/gui/advancements/backgrounds/adventure.png";
    public static final String HUSBANDRY =
        "minecraft:textures/gui/advancements/backgrounds/husbandry.png";
    public static final String NETHER =
        "minecraft:textures/gui/advancements/backgrounds/nether.png";
    public static final String END = "minecraft:textures/gui/advancements/backgrounds/end.png";
    public static final String STONE = "minecraft:textures/gui/advancements/backgrounds/stone.png";

    private Backgrounds() {}
  }

  private static final Gson GSON = new Gson();

  private final JavaSkriptPlugin plugin;
  private final AdvancementHelper helper;
  private final NamespacedKey key;

  private Component title = Component.text("Custom Advancement");
  private Component description = Component.empty();
  private String iconKey = "minecraft:diamond";
  private AdvancementDisplay.Frame frame = AdvancementDisplay.Frame.TASK;
  private boolean showToast = true;
  private boolean announceToChat = true;
  private boolean hidden = false;
  private String background = null;
  private String parentKey = null;

  private final Map<String, String> criteria = new LinkedHashMap<>();
  private boolean requireAny = false;
  private List<List<String>> customRequirements = null;

  private BiConsumer<Player, Advancement> completionHandler = null;

  public CustomAdvancement(JavaSkriptPlugin plugin, AdvancementHelper helper, NamespacedKey key) {
    if (key == null) {
      throw new IllegalArgumentException("Advancement key cannot be null");
    }
    this.plugin = plugin;
    this.helper = helper;
    this.key = key;
  }

  /** Gets the unique NamespacedKey of this advancement. */
  public NamespacedKey key() {
    return key;
  }

  /** Sets the advancement title using an Adventure Component. */
  public CustomAdvancement title(Component title) {
    this.title = title != null ? title : Component.empty();
    return this;
  }

  /** Sets the advancement title with MiniMessage or legacy formatting support. */
  public CustomAdvancement title(String title) {
    return title(TextUtil.parse(title));
  }

  /** Sets the advancement description using an Adventure Component. */
  public CustomAdvancement description(Component description) {
    this.description = description != null ? description : Component.empty();
    return this;
  }

  /** Sets the advancement description with MiniMessage or legacy formatting support. */
  public CustomAdvancement description(String description) {
    return description(TextUtil.parse(description));
  }

  /** Sets the icon item by Material. */
  public CustomAdvancement icon(Material material) {
    if (material != null) {
      try {
        if (material.getKey() != null) {
          this.iconKey = material.getKey().asString();
          return this;
        }
      } catch (Throwable ignored) {
      }
      this.iconKey = "minecraft:" + material.name().toLowerCase(Locale.ROOT);
    }
    return this;
  }

  /** Sets the icon item from an ItemStack. */
  public CustomAdvancement icon(ItemStack item) {
    if (item != null && item.getType() != Material.AIR) {
      icon(item.getType());
    }
    return this;
  }

  /** Sets the icon item by NamespacedKey. */
  public CustomAdvancement icon(NamespacedKey key) {
    if (key != null) {
      this.iconKey = key.asString();
    }
    return this;
  }

  /** Sets the icon item by Minecraft ID (e.g. "minecraft:nether_star"). */
  public CustomAdvancement icon(String itemId) {
    if (itemId != null && !itemId.isBlank()) {
      this.iconKey =
          itemId.contains(":")
              ? itemId.toLowerCase(Locale.ROOT)
              : "minecraft:" + itemId.toLowerCase(Locale.ROOT);
    }
    return this;
  }

  /** Sets the display frame type (TASK, GOAL, CHALLENGE). */
  public CustomAdvancement frame(AdvancementDisplay.Frame frame) {
    if (frame != null) {
      this.frame = frame;
    }
    return this;
  }

  /** Sets the display frame type by string name ("task", "goal", "challenge"). */
  public CustomAdvancement frame(String frameName) {
    if (frameName != null) {
      for (AdvancementDisplay.Frame f : AdvancementDisplay.Frame.values()) {
        if (f.name().equalsIgnoreCase(frameName)) {
          this.frame = f;
          break;
        }
      }
    }
    return this;
  }

  /** Convenience helper for normal Task frame (square icon). */
  public CustomAdvancement task() {
    return frame(AdvancementDisplay.Frame.TASK);
  }

  /** Convenience helper for Goal frame (rounded icon). */
  public CustomAdvancement goal() {
    return frame(AdvancementDisplay.Frame.GOAL);
  }

  /** Convenience helper for Challenge frame (ornate icon with purple banner). */
  public CustomAdvancement challenge() {
    return frame(AdvancementDisplay.Frame.CHALLENGE);
  }

  /** Sets whether to show a toast notification popup in the top-right upon unlock. */
  public CustomAdvancement toast(boolean showToast) {
    this.showToast = showToast;
    return this;
  }

  /** Sets whether to broadcast an unlock message in chat. */
  public CustomAdvancement announce(boolean announceToChat) {
    this.announceToChat = announceToChat;
    return this;
  }

  /** Sets whether this advancement is hidden until completed. */
  public CustomAdvancement hidden(boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  /** Sets the background texture for root tab advancements. */
  public CustomAdvancement background(String texturePath) {
    this.background = texturePath;
    return this;
  }

  /** Sets the background texture by NamespacedKey. */
  public CustomAdvancement background(NamespacedKey backgroundKey) {
    if (backgroundKey != null) {
      this.background = backgroundKey.asString();
    }
    return this;
  }

  /**
   * Sets the parent advancement key (e.g. "minecraft:story/root" or another custom advancement).
   */
  public CustomAdvancement parent(String parentKey) {
    this.parentKey = parentKey;
    return this;
  }

  /** Sets the parent advancement by NamespacedKey. */
  public CustomAdvancement parent(NamespacedKey parentKey) {
    if (parentKey != null) {
      this.parentKey = parentKey.asString();
    }
    return this;
  }

  /** Sets the parent advancement by existing Advancement instance. */
  public CustomAdvancement parent(Advancement parent) {
    if (parent != null) {
      this.parentKey = parent.getKey().asString();
    }
    return this;
  }

  /** Sets the parent advancement from another CustomAdvancement builder. */
  public CustomAdvancement parent(CustomAdvancement parent) {
    if (parent != null) {
      this.parentKey = parent.key().asString();
    }
    return this;
  }

  /**
   * Adds a criterion with the default manual trigger ("minecraft:impossible").
   *
   * @param name Unique criterion name
   */
  public CustomAdvancement criterion(String name) {
    return criterion(name, "minecraft:impossible");
  }

  /**
   * Adds a criterion with a specific trigger identifier.
   *
   * @param name Unique criterion name
   * @param triggerType Minecraft trigger type (e.g. "minecraft:impossible")
   */
  public CustomAdvancement criterion(String name, String triggerType) {
    if (name != null && !name.isBlank()) {
      criteria.put(name, triggerType != null ? triggerType : "minecraft:impossible");
    }
    return this;
  }

  /** Adds multiple criteria with manual triggers. */
  public CustomAdvancement criteria(String... names) {
    if (names != null) {
      for (String name : names) {
        criterion(name);
      }
    }
    return this;
  }

  /** Adds a collection of criteria with manual triggers. */
  public CustomAdvancement criteria(Collection<String> names) {
    if (names != null) {
      for (String name : names) {
        criterion(name);
      }
    }
    return this;
  }

  /** Requires all declared criteria to be awarded to complete the advancement (AND logic). */
  public CustomAdvancement requireAll() {
    this.requireAny = false;
    this.customRequirements = null;
    return this;
  }

  /** Requires any one of the declared criteria to complete the advancement (OR logic). */
  public CustomAdvancement requireAny() {
    this.requireAny = true;
    this.customRequirements = null;
    return this;
  }

  /** Sets a custom requirements matrix. */
  public CustomAdvancement requirements(List<List<String>> requirements) {
    this.customRequirements = requirements;
    return this;
  }

  /**
   * Register a callback triggered when a player completes this advancement.
   *
   * @param handler Completion callback with Player and Advancement
   */
  public CustomAdvancement onComplete(BiConsumer<Player, Advancement> handler) {
    this.completionHandler = handler;
    return this;
  }

  /**
   * Register a callback triggered when a player completes this advancement.
   *
   * @param handler Completion callback with Player
   */
  public CustomAdvancement onComplete(Consumer<Player> handler) {
    if (handler != null) {
      this.completionHandler = (player, adv) -> handler.accept(player);
    }
    return this;
  }

  public boolean hasCompletionHandler() {
    return completionHandler != null;
  }

  public BiConsumer<Player, Advancement> getCompletionHandler() {
    return completionHandler;
  }

  /**
   * Generates the Minecraft 1.21.11+ compliant JSON payload for server advancement registration.
   *
   * @return Formatted advancement JSON
   */
  public String toJson() {
    JsonObject json = new JsonObject();

    // Display section
    JsonObject displayObj = new JsonObject();

    JsonObject iconObj = new JsonObject();
    iconObj.addProperty("id", iconKey);
    displayObj.add("icon", iconObj);

    displayObj.add("title", GsonComponentSerializer.gson().serializeToTree(title));
    displayObj.add("description", GsonComponentSerializer.gson().serializeToTree(description));
    displayObj.addProperty("frame", frame.name().toLowerCase(Locale.ROOT));
    displayObj.addProperty("show_toast", showToast);
    displayObj.addProperty("announce_to_chat", announceToChat);
    displayObj.addProperty("hidden", hidden);

    if (background != null && !background.isBlank()) {
      displayObj.addProperty("background", background);
    }

    json.add("display", displayObj);

    // Parent
    if (parentKey != null && !parentKey.isBlank()) {
      json.addProperty("parent", parentKey);
    }

    // Criteria
    JsonObject criteriaObj = new JsonObject();
    if (criteria.isEmpty()) {
      JsonObject trig = new JsonObject();
      trig.addProperty("trigger", "minecraft:impossible");
      criteriaObj.add("impossible", trig);
    } else {
      for (Map.Entry<String, String> entry : criteria.entrySet()) {
        JsonObject trig = new JsonObject();
        trig.addProperty("trigger", entry.getValue());
        criteriaObj.add(entry.getKey(), trig);
      }
    }
    json.add("criteria", criteriaObj);

    // Requirements
    JsonArray reqArray = new JsonArray();
    if (customRequirements != null && !customRequirements.isEmpty()) {
      for (List<String> group : customRequirements) {
        JsonArray groupArray = new JsonArray();
        for (String c : group) {
          groupArray.add(c);
        }
        reqArray.add(groupArray);
      }
    } else if (criteria.isEmpty()) {
      JsonArray groupArray = new JsonArray();
      groupArray.add("impossible");
      reqArray.add(groupArray);
    } else if (requireAny) {
      JsonArray groupArray = new JsonArray();
      for (String c : criteria.keySet()) {
        groupArray.add(c);
      }
      reqArray.add(groupArray);
    } else {
      for (String c : criteria.keySet()) {
        JsonArray groupArray = new JsonArray();
        groupArray.add(c);
        reqArray.add(groupArray);
      }
    }
    json.add("requirements", reqArray);

    return GSON.toJson(json);
  }

  /**
   * Registers this advancement with the server and syncs resources to clients.
   *
   * @return Loaded Bukkit Advancement instance, or null if registration failed
   */
  public Advancement register() {
    if (helper != null) {
      return helper.register(this);
    }
    return null;
  }
}
