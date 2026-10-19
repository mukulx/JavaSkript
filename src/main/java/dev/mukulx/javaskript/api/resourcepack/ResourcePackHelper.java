package dev.mukulx.javaskript.api.resourcepack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

/**
 * Resource packs of one script, injected into any field of this type. Every pack the script
 * registers is removed when it unloads; on a reload, a pack that did not change stays on players
 * without being downloaded again.
 *
 * <pre>{@code
 * private ResourcePackHelper resourcePacks;
 *
 * public void onEnable() {
 *     ResourcePack pack = resourcePacks.create("main")
 *         .itemModel("ruby", Path.of("ruby.png"))
 *         .register();
 * }
 * }</pre>
 */
public final class ResourcePackHelper {

  private final ResourcePackManager manager;
  private final String owner;
  private final Set<String> owned = ConcurrentHashMap.newKeySet();

  public ResourcePackHelper(ResourcePackManager manager, String owner) {
    this.manager = manager;
    this.owner = owner;
  }

  /** Start building a pack. Pack ids are {@code <script>:<name>}. */
  public ResourcePackBuilder create(String name) {
    owned.add(ResourcePackBuilder.packId(owner, name));
    return manager.builder(owner, name);
  }

  /** Register an unpacked pack folder, relative to {@code plugins/JavaSkript/resourcepacks/}. */
  public ResourcePack fromFolder(String name, String folder) {
    return create(name).folder(folder).register();
  }

  /** Register an existing pack zip, relative to {@code plugins/JavaSkript/resourcepacks/}. */
  public ResourcePack fromZip(String name, String zip) {
    return create(name).zip(zip).register();
  }

  /**
   * Register a pack hosted elsewhere.
   *
   * @param sha1 the zip's SHA-1 as hex
   */
  public ResourcePack external(String name, String url, String sha1) {
    owned.add(ResourcePackBuilder.packId(owner, name));
    return manager.registerExternal(owner, name, url, sha1, 0, null);
  }

  /**
   * Register a pack hosted elsewhere with a priority and whether players must accept it.
   *
   * @param required null to use {@code required} from config.yml
   */
  public ResourcePack external(
      String name, String url, String sha1, int priority, Boolean required) {
    owned.add(ResourcePackBuilder.packId(owner, name));
    return manager.registerExternal(owner, name, url, sha1, priority, required);
  }

  /** Register a pack hosted elsewhere, downloading it once in the background to hash it. */
  public CompletableFuture<ResourcePack> external(String name, String url) {
    owned.add(ResourcePackBuilder.packId(owner, name));
    return manager.registerExternal(owner, name, url, 0, null);
  }

  /** One of this script's packs by name. */
  public Optional<ResourcePack> get(String name) {
    return manager.get(ResourcePackBuilder.packId(owner, name));
  }

  /** This script's registered packs. */
  public List<ResourcePack> getAll() {
    return manager.getAll().stream().filter(pack -> owned.contains(pack.getId())).toList();
  }

  /** Every registered pack, from all scripts and plugins. */
  public List<ResourcePack> getAllPacks() {
    return manager.getAll();
  }

  /** Remove one of this script's packs from the server and from online players. */
  public boolean unregister(String name) {
    String id = ResourcePackBuilder.packId(owner, name);
    owned.remove(id);
    return manager.unregister(id);
  }

  /** Whether the player has downloaded and applied the pack. */
  public boolean isLoaded(Player player, ResourcePack pack) {
    return manager.isLoaded(player, pack);
  }

  /** The last status the player's client reported for the pack. */
  public Optional<Status> getStatus(Player player, ResourcePack pack) {
    return manager.getStatus(player, pack);
  }

  /** Send every registered pack to a player again. */
  public void resend(Player player) {
    manager.resend(player);
  }

  /** Called when the script unloads. */
  public void close() {
    Collection<String> ids = List.copyOf(owned);
    owned.clear();
    manager.release(ids);
  }
}
