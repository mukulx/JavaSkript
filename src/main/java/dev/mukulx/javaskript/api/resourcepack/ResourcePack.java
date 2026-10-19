package dev.mukulx.javaskript.api.resourcepack;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

/**
 * A resource pack registered with JavaSkript. Packs are built with {@link ResourcePackBuilder} or
 * point at a URL hosted elsewhere, and are sent to every player who joins.
 *
 * <p>Instances are immutable. Registering a pack with the same id replaces this one.
 */
public final class ResourcePack {

  private final String id;
  private final String namespace;
  private final UUID uuid;
  private final String hash;
  private final byte @Nullable [] data;
  private final @Nullable URI externalUri;
  private final int priority;
  private final @Nullable Boolean required;
  private final @Nullable Consumer<Player> onLoaded;
  private final @Nullable BiConsumer<Player, PlayerResourcePackStatusEvent.Status> onFailed;

  ResourcePack(
      String id,
      String namespace,
      String hash,
      byte @Nullable [] data,
      @Nullable URI externalUri,
      int priority,
      @Nullable Boolean required,
      @Nullable Consumer<Player> onLoaded,
      @Nullable BiConsumer<Player, PlayerResourcePackStatusEvent.Status> onFailed) {
    this.id = id;
    this.namespace = namespace;
    this.uuid = uuidFor(id);
    this.hash = hash;
    this.data = data;
    this.externalUri = externalUri;
    this.priority = priority;
    this.required = required;
    this.onLoaded = onLoaded;
    this.onFailed = onFailed;
  }

  /**
   * The id clients know the pack by. Stable, so a pack that did not change is not downloaded again.
   */
  static UUID uuidFor(String id) {
    return UUID.nameUUIDFromBytes(("javaskript-pack:" + id).getBytes(StandardCharsets.UTF_8));
  }

  /** Unique id in the form {@code owner:name}, e.g. {@code weapons:main}. */
  public String getId() {
    return id;
  }

  /** Namespace of the generated textures, models and sounds, e.g. {@code weapons}. */
  public String getNamespace() {
    return namespace;
  }

  /** The id sent to clients. Matches {@link PlayerResourcePackStatusEvent#getID()}. */
  public UUID getUniqueId() {
    return uuid;
  }

  /** Lower-case hex SHA-1 of the pack zip. */
  public String getHash() {
    return hash;
  }

  /** Size of the zip in bytes, or -1 for a pack hosted elsewhere. */
  public long getSize() {
    return data != null ? data.length : -1;
  }

  /**
   * Packs are applied in ascending priority, so a higher one overrides the textures of a lower one.
   */
  public int getPriority() {
    return priority;
  }

  /** Whether players must accept it; null means the {@code required} setting in config.yml. */
  public @Nullable Boolean getRequired() {
    return required;
  }

  /** Whether the pack is hosted elsewhere instead of by JavaSkript. */
  public boolean isExternal() {
    return externalUri != null;
  }

  /** The URL of a pack hosted elsewhere, or null. */
  public @Nullable URI getExternalUri() {
    return externalUri;
  }

  /** Key of an item model in this pack, for {@link ItemMeta#setItemModel}. */
  public NamespacedKey itemModel(String name) {
    return new NamespacedKey(namespace, name);
  }

  /** Key of a sound in this pack, for {@code player.playSound(player, key.asString(), ...)}. */
  public NamespacedKey sound(String name) {
    return new NamespacedKey(namespace, name);
  }

  /**
   * Give an item one of this pack's item models. Changes the item in place and returns it.
   *
   * @param model the name passed to {@link ResourcePackBuilder#itemModel}
   */
  public ItemStack applyModel(ItemStack item, String model) {
    Objects.requireNonNull(item, "item cannot be null");
    item.editMeta(meta -> meta.setItemModel(itemModel(model)));
    return item;
  }

  /** Play one of this pack's sounds to a player at their location. */
  public void playSound(Player player, String sound, float volume, float pitch) {
    player.playSound(player, sound(sound).asString(), volume, pitch);
  }

  byte @Nullable [] data() {
    return data;
  }

  void notifyLoaded(Player player) {
    if (onLoaded != null) {
      onLoaded.accept(player);
    }
  }

  void notifyFailed(Player player, PlayerResourcePackStatusEvent.Status status) {
    if (onFailed != null) {
      onFailed.accept(player, status);
    }
  }

  @Override
  public String toString() {
    return "ResourcePack{" + id + ", " + hash + "}";
  }
}
