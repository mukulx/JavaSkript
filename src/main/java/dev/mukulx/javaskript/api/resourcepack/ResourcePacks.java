package dev.mukulx.javaskript.api.resourcepack;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Static access to resource packs for plugins and addons. Packs registered here belong to the given
 * owner and stay until {@link #unregister} is called, so unregister them when your plugin disables.
 * Scripts should use their injected {@link ResourcePackHelper} instead.
 */
public final class ResourcePacks {

  private static ResourcePackManager instance;

  private ResourcePacks() {}

  public static void setInstance(ResourcePackManager manager) {
    instance = manager;
  }

  private static ResourcePackManager get() {
    if (instance == null) {
      throw new IllegalStateException("JavaSkript resource packs are not available");
    }
    return instance;
  }

  /** Start building a pack owned by a plugin; its default namespace is the plugin's name. */
  public static ResourcePackBuilder builder(Plugin owner, String name) {
    return get().builder(owner.getName(), name);
  }

  /** Register a pack hosted elsewhere. */
  public static ResourcePack external(Plugin owner, String name, String url, String sha1) {
    return get().registerExternal(owner.getName(), name, url, sha1, 0, null);
  }

  /** Register a pack hosted elsewhere, downloading it once in the background to hash it. */
  public static CompletableFuture<ResourcePack> external(Plugin owner, String name, String url) {
    return get().registerExternal(owner.getName(), name, url, 0, null);
  }

  /** A pack by id, e.g. {@code weapons:main}. */
  public static Optional<ResourcePack> get(String id) {
    return get().get(id);
  }

  /** Every registered pack, lowest priority first. */
  public static List<ResourcePack> getAll() {
    return get().getAll();
  }

  /** Remove a pack from the server and from online players. */
  public static boolean unregister(String id) {
    return get().unregister(id);
  }

  /** Whether the player has downloaded and applied the pack. */
  public static boolean isLoaded(Player player, ResourcePack pack) {
    return get().isLoaded(player, pack);
  }

  /** Send every registered pack to a player again. */
  public static void resend(Player player) {
    get().resend(player);
  }
}
