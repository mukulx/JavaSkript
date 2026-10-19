package dev.mukulx.javaskript.api.resourcepack;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.util.ServerUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

/**
 * Keeps every registered resource pack, hosts the generated ones over HTTP and sends them to
 * players. Scripts use their own {@link ResourcePackHelper}; plugins and addons use {@link
 * ResourcePacks}.
 *
 * <p>Each pack is sent separately, so several scripts can each ship their own pack and players
 * receive all of them in one prompt. The HTTP server only starts once a pack needs hosting.
 */
public final class ResourcePackManager implements Listener {

  /** Largest pack the client downloads. */
  static final int MAX_PACK_BYTES = 250 * 1024 * 1024;

  /** How long a reloading script has to register its pack again before players lose it. */
  private static final long RELEASE_DELAY_TICKS = 40L;

  private static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");
  private static final Set<Status> FAILURES =
      EnumSet.of(Status.DECLINED, Status.FAILED_DOWNLOAD, Status.INVALID_URL, Status.FAILED_RELOAD);

  private final JavaSkriptPlugin plugin;
  private final Path folder;
  private final Map<String, ResourcePack> packs = new ConcurrentHashMap<>();
  private final Map<String, Object> pendingRemovals = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, Status>> statuses = new ConcurrentHashMap<>();
  private volatile Settings settings;
  private PackHttpServer server; // guarded by this
  private boolean serverFailed; // guarded by this; retried after a reload
  private volatile boolean warnedNoAddress;

  private record Settings(
      boolean enabled,
      int port,
      String bindAddress,
      String publicAddress,
      boolean required,
      String prompt) {}

  public ResourcePackManager(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.folder = plugin.getDataFolder().toPath().resolve("resourcepacks");
    this.folder.toFile().mkdirs();
    this.settings = readSettings();
  }

  /** {@code plugins/JavaSkript/resourcepacks/}, where relative pack paths are resolved. */
  public Path getFolder() {
    return folder;
  }

  Path resolve(Path path) {
    return path.isAbsolute() ? path : folder.resolve(path).normalize();
  }

  /** Whether resource packs are enabled in config.yml. */
  public boolean isEnabled() {
    return settings.enabled();
  }

  /**
   * Start building a pack. Plugins and addons must {@link #unregister} it themselves when they
   * disable; scripts should use their injected {@link ResourcePackHelper}, which does it for them.
   *
   * @param owner who the pack belongs to; also its default namespace
   */
  public ResourcePackBuilder builder(String owner, String name) {
    return new ResourcePackBuilder(this, checkOwner(owner), name);
  }

  /**
   * Register a pack hosted elsewhere, e.g. on a CDN.
   *
   * @param sha1 the zip's SHA-1 as hex; clients reject the pack if it does not match
   */
  public ResourcePack registerExternal(
      String owner, String name, String url, String sha1, int priority, Boolean required) {
    URI uri = checkUrl(url);
    String hash = sha1 != null ? sha1.toLowerCase() : "";
    if (!SHA1.matcher(hash).matches()) {
      throw new IllegalArgumentException("Invalid SHA-1 for pack " + name + ": " + sha1);
    }
    String id = ResourcePackBuilder.packId(checkOwner(owner), name);
    ResourcePack pack =
        new ResourcePack(
            id,
            ResourcePackBuilder.toNamespace(owner),
            hash,
            null,
            uri,
            priority,
            required,
            null,
            null);
    register(pack);
    return pack;
  }

  /** Register a pack hosted elsewhere, downloading it once in the background to hash it. */
  public CompletableFuture<ResourcePack> registerExternal(
      String owner, String name, String url, int priority, Boolean required) {
    URI uri = checkUrl(url);
    String id = ResourcePackBuilder.packId(checkOwner(owner), name);
    return ResourcePackInfo.resourcePackInfo()
        .id(ResourcePack.uuidFor(id))
        .uri(uri)
        .computeHashAndBuild()
        .thenApply(info -> registerExternal(owner, name, url, info.hash(), priority, required));
  }

  synchronized void register(ResourcePack pack) {
    Object pending = pendingRemovals.remove(pack.getId());
    ResourcePack old = packs.put(pack.getId(), pack);
    if (pack.data() != null && server != null) {
      server.publish(pack.getHash(), pack.data());
    }
    if (old != null && !old.getHash().equals(pack.getHash())) {
      retireIfUnused(old);
    }
    plugin.debug("Registered resource pack " + pack.getId() + " (" + pack.getHash() + ")");

    // A reloading script re-registering an unchanged pack: players already have it
    if (pending != null && old != null && old.getHash().equals(pack.getHash())) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      send(player, List.of(pack));
    }
  }

  /** Remove a pack and take it off every online player. Returns whether it was registered. */
  public synchronized boolean unregister(String id) {
    pendingRemovals.remove(id);
    ResourcePack pack = packs.remove(id);
    if (pack == null) {
      return false;
    }
    retireIfUnused(pack);
    if (plugin.isEnabled()) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        ServerUtil.runForPlayer(
            plugin, player, () -> player.removeResourcePacks(pack.getUniqueId()));
      }
    }
    plugin.debug("Unregistered resource pack " + id);
    return true;
  }

  /**
   * Remove an unloading script's packs after a short delay. A reload registers them again in time,
   * and an unchanged pack then stays on players instead of being removed and downloaded again.
   */
  void release(Collection<String> ids) {
    for (String id : ids) {
      if (!plugin.isEnabled()) {
        unregister(id);
        continue;
      }
      Object token = new Object();
      pendingRemovals.put(id, token);
      ServerUtil.runLaterSync(
          plugin,
          () -> {
            if (pendingRemovals.remove(id, token)) {
              unregister(id);
            }
          },
          RELEASE_DELAY_TICKS);
    }
  }

  /** A pack by id, e.g. {@code weapons:main}. */
  public Optional<ResourcePack> get(String id) {
    return Optional.ofNullable(packs.get(id));
  }

  /** Every registered pack, lowest priority first. */
  public List<ResourcePack> getAll() {
    List<ResourcePack> all = new ArrayList<>(packs.values());
    all.sort(Comparator.comparingInt(ResourcePack::getPriority).thenComparing(ResourcePack::getId));
    return all;
  }

  /** The last status the player's client reported for a pack, if any. */
  public Optional<Status> getStatus(Player player, ResourcePack pack) {
    Map<UUID, Status> byPack = statuses.get(player.getUniqueId());
    return Optional.ofNullable(byPack != null ? byPack.get(pack.getUniqueId()) : null);
  }

  /** Whether the player has downloaded and applied the pack. */
  public boolean isLoaded(Player player, ResourcePack pack) {
    return getStatus(player, pack).orElse(null) == Status.SUCCESSFULLY_LOADED;
  }

  /** Send every registered pack to a player again, e.g. after they declined one. */
  public void resend(Player player) {
    send(player, getAll());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent event) {
    resend(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    statuses.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onStatus(PlayerResourcePackStatusEvent event) {
    Player player = event.getPlayer();
    ResourcePack pack = findByUniqueId(event.getID());
    if (pack == null) {
      return;
    }
    statuses
        .computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
        .put(pack.getUniqueId(), event.getStatus());

    try {
      if (event.getStatus() == Status.SUCCESSFULLY_LOADED) {
        pack.notifyLoaded(player);
      } else if (FAILURES.contains(event.getStatus())) {
        pack.notifyFailed(player, event.getStatus());
      }
    } catch (Throwable t) {
      plugin.getLogger().warning("Error in resource pack callback for " + pack.getId() + ": " + t);
    }

    if (FAILURES.contains(event.getStatus()) && isRequired(pack) && player.isOnline()) {
      player.kick(
          plugin
              .getMessageManager()
              .get(
                  "resource-packs.required-failed",
                  "<red>This server requires its resource pack. Rejoin and accept it to play.</red>"));
    }
  }

  /**
   * Re-read config.yml. Online players get the packs again if they were just enabled or their
   * address changed, and lose them if packs were disabled.
   */
  public void reload() {
    Settings old;
    synchronized (this) {
      old = settings;
      settings = readSettings();
      serverFailed = false;
      warnedNoAddress = false;
      boolean moved =
          old.port() != settings.port() || !old.bindAddress().equals(settings.bindAddress());
      if (server != null && (moved || !settings.enabled())) {
        server.stop();
        server = null;
      }
    }

    Settings current = settings;
    if (old.enabled() && !current.enabled()) {
      List<UUID> ids = getAll().stream().map(ResourcePack::getUniqueId).toList();
      for (Player player : Bukkit.getOnlinePlayers()) {
        ServerUtil.runForPlayer(plugin, player, () -> player.removeResourcePacks(ids));
      }
    } else if (current.enabled()
        && (!old.enabled()
            || old.port() != current.port()
            || !old.publicAddress().equals(current.publicAddress()))) {
      Bukkit.getOnlinePlayers().forEach(this::resend);
    }
  }

  /** Called when the plugin disables. */
  public synchronized void shutdown() {
    if (server != null) {
      server.stop();
      server = null;
    }
    packs.clear();
    pendingRemovals.clear();
    statuses.clear();
  }

  private void send(Player player, List<ResourcePack> toSend) {
    Settings current = settings;
    if (!current.enabled() || toSend.isEmpty() || !plugin.isEnabled()) {
      return;
    }
    List<ResourcePackInfo> required = new ArrayList<>();
    List<ResourcePackInfo> optional = new ArrayList<>();
    for (ResourcePack pack : toSend) {
      URI uri = uriFor(player, pack);
      if (uri == null) {
        continue;
      }
      ResourcePackInfo info =
          ResourcePackInfo.resourcePackInfo(pack.getUniqueId(), uri, pack.getHash());
      (isRequired(pack) ? required : optional).add(info);
    }
    // One request per kind, because "required" applies to a whole request
    List<ResourcePackRequest> requests = new ArrayList<>();
    if (!required.isEmpty()) {
      requests.add(request(required, true, current));
    }
    if (!optional.isEmpty()) {
      requests.add(request(optional, false, current));
    }
    if (!requests.isEmpty()) {
      ServerUtil.runForPlayer(plugin, player, () -> requests.forEach(player::sendResourcePacks));
    }
  }

  private static ResourcePackRequest request(
      List<ResourcePackInfo> infos, boolean required, Settings settings) {
    ResourcePackRequest.Builder builder =
        ResourcePackRequest.resourcePackRequest().packs(infos).required(required).replace(false);
    if (!settings.prompt().isBlank()) {
      builder.prompt(MiniMessage.miniMessage().deserialize(settings.prompt()));
    }
    return builder.build();
  }

  private URI uriFor(Player player, ResourcePack pack) {
    if (pack.isExternal()) {
      return pack.getExternalUri();
    }
    if (ensureServer() == null) {
      return null;
    }
    String base = baseUrl(player);
    if (base == null) {
      if (!warnedNoAddress) {
        warnedNoAddress = true;
        plugin
            .getLogger()
            .warning(
                "Cannot tell players where to download resource packs from. Set"
                    + " modules.resource-packs.public-address in config.yml");
      }
      return null;
    }
    return URI.create(base + "/" + pack.getHash() + ".zip");
  }

  /**
   * The configured public address, else the host the player typed to join. That host reaches this
   * server unless a proxy sits in front of it, which is when public-address is needed.
   */
  private String baseUrl(Player player) {
    Settings current = settings;
    String address = current.publicAddress();
    if (!address.isEmpty()) {
      if (address.contains("://")) {
        return address.endsWith("/") ? address.substring(0, address.length() - 1) : address;
      }
      return "http://" + address + ":" + current.port();
    }
    String host = null;
    try {
      InetSocketAddress virtualHost = player.getConnection().getVirtualHost();
      host = virtualHost != null ? virtualHost.getHostString() : null;
    } catch (Throwable ignored) {
      // Not every connection reports a virtual host
    }
    if (host == null || host.isBlank()) {
      host = Bukkit.getIp();
    }
    if (host == null || host.isBlank()) {
      return null;
    }
    if (host.contains(":") && !host.startsWith("[")) {
      host = "[" + host + "]";
    }
    return "http://" + host + ":" + current.port();
  }

  private synchronized PackHttpServer ensureServer() {
    if (server != null || serverFailed) {
      return server;
    }
    Settings current = settings;
    InetSocketAddress address =
        current.bindAddress().isEmpty()
            ? new InetSocketAddress(current.port())
            : new InetSocketAddress(current.bindAddress(), current.port());
    try {
      server = PackHttpServer.start(address);
    } catch (IOException | RuntimeException e) {
      serverFailed = true;
      plugin
          .getLogger()
          .severe(
              "Could not start the resource pack server on port "
                  + current.port()
                  + ": "
                  + e.getMessage()
                  + ". Change modules.resource-packs.port and run /js configreload");
      return null;
    }
    for (ResourcePack pack : packs.values()) {
      if (pack.data() != null) {
        server.publish(pack.getHash(), pack.data());
      }
    }
    plugin.getLogger().info("Resource pack server listening on port " + server.port());
    return server;
  }

  private void retireIfUnused(ResourcePack pack) {
    if (server == null || pack.data() == null) {
      return;
    }
    boolean stillUsed = packs.values().stream().anyMatch(p -> p.getHash().equals(pack.getHash()));
    if (!stillUsed) {
      server.retire(pack.getHash());
    }
  }

  private ResourcePack findByUniqueId(UUID uuid) {
    for (ResourcePack pack : packs.values()) {
      if (pack.getUniqueId().equals(uuid)) {
        return pack;
      }
    }
    return null;
  }

  private boolean isRequired(ResourcePack pack) {
    return pack.getRequired() != null ? pack.getRequired() : settings.required();
  }

  private Settings readSettings() {
    ConfigurationSection config =
        plugin.getConfig().getConfigurationSection("modules.resource-packs");
    if (config == null) {
      return new Settings(true, 8163, "", "", false, "");
    }
    int port = config.getInt("port", 8163);
    if (port < 1 || port > 65535) {
      plugin.getLogger().warning("modules.resource-packs.port " + port + " is invalid; using 8163");
      port = 8163;
    }
    return new Settings(
        config.getBoolean("enabled", true),
        port,
        config.getString("bind-address", "").trim(),
        config.getString("public-address", "").trim(),
        config.getBoolean("required", false),
        config.getString("prompt", ""));
  }

  private static String checkOwner(String owner) {
    if (owner == null || owner.isBlank()) {
      throw new IllegalArgumentException("A resource pack owner is required");
    }
    return owner;
  }

  private static URI checkUrl(String url) {
    URI uri;
    try {
      uri = URI.create(url);
    } catch (NullPointerException | IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid resource pack URL: " + url, e);
    }
    if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException("Resource pack URL must be http or https: " + url);
    }
    return uri;
  }
}
