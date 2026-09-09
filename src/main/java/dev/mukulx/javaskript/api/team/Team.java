package dev.mukulx.javaskript.api.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Represents a persistent team, clan, or party.
 *
 * <p>Thread-safe in-memory model backed by database persistence.
 */
public class Team {

  private final String id;
  private volatile String name;
  private volatile String tag;
  private volatile String prefix;
  private volatile String suffix;
  private volatile NamedTextColor color;
  private volatile UUID leader;
  private final Map<UUID, TeamMember> members;
  private volatile boolean friendlyFire;
  private volatile boolean open;
  private volatile int maxSize;
  private volatile Location home;
  private volatile double balance;
  private final Map<String, Object> metadata;
  private final long createdAt;

  public Team(String id, String name, UUID leader) {
    this(
        id,
        name,
        name != null && name.length() > 4
            ? "[" + name.substring(0, 4).toUpperCase() + "]"
            : "[" + id.toUpperCase() + "]",
        "",
        "",
        NamedTextColor.WHITE,
        leader,
        false,
        false,
        0,
        null,
        0.0,
        System.currentTimeMillis());
  }

  public Team(
      String id,
      String name,
      String tag,
      String prefix,
      String suffix,
      NamedTextColor color,
      UUID leader,
      boolean friendlyFire,
      boolean open,
      int maxSize,
      Location home,
      double balance,
      long createdAt) {
    this.id = Objects.requireNonNull(id, "team id cannot be null").toLowerCase();
    this.name = name != null && !name.isBlank() ? name : id;
    this.tag = tag != null ? tag : "";
    this.prefix = prefix != null ? prefix : "";
    this.suffix = suffix != null ? suffix : "";
    this.color = color != null ? color : NamedTextColor.WHITE;
    this.leader = Objects.requireNonNull(leader, "leader cannot be null");
    this.members = new ConcurrentHashMap<>();
    this.friendlyFire = friendlyFire;
    this.open = open;
    this.maxSize = Math.max(0, maxSize);
    this.home = home;
    this.balance = Math.max(0.0, balance);
    this.metadata = new ConcurrentHashMap<>();
    this.createdAt = createdAt;

    // Add leader as initial member
    this.members.put(leader, new TeamMember(leader, TeamRole.LEADER, createdAt, createdAt));
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag != null ? tag : "";
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix != null ? prefix : "";
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix != null ? suffix : "";
  }

  public NamedTextColor getColor() {
    return color;
  }

  public void setColor(NamedTextColor color) {
    this.color = color != null ? color : NamedTextColor.WHITE;
  }

  public UUID getLeader() {
    return leader;
  }

  public void setLeader(UUID leader) {
    Objects.requireNonNull(leader, "leader cannot be null");
    this.leader = leader;
    TeamMember existing = members.get(leader);
    if (existing != null) {
      existing.setRole(TeamRole.LEADER);
    } else {
      members.put(leader, new TeamMember(leader, TeamRole.LEADER));
    }
  }

  public boolean isLeader(UUID uuid) {
    return leader.equals(uuid);
  }

  public boolean isCaptain(UUID uuid) {
    TeamMember member = members.get(uuid);
    return member != null && member.getRole().isAtLeast(TeamRole.CAPTAIN);
  }

  public Map<UUID, TeamMember> getMembers() {
    return Collections.unmodifiableMap(members);
  }

  public Set<UUID> getMemberUuids() {
    return Collections.unmodifiableSet(members.keySet());
  }

  public Optional<TeamMember> getMember(UUID uuid) {
    return Optional.ofNullable(members.get(uuid));
  }

  public boolean hasMember(UUID uuid) {
    return members.containsKey(uuid);
  }

  public boolean hasMember(Player player) {
    return player != null && hasMember(player.getUniqueId());
  }

  public void addMember(TeamMember member) {
    Objects.requireNonNull(member, "member cannot be null");
    members.put(member.getUniqueId(), member);
  }

  public void addMember(UUID uuid, TeamRole role) {
    Objects.requireNonNull(uuid, "uuid cannot be null");
    TeamRole resolvedRole = role != null ? role : TeamRole.MEMBER;
    members.put(uuid, new TeamMember(uuid, resolvedRole));
  }

  public TeamMember removeMember(UUID uuid) {
    return members.remove(uuid);
  }

  public Optional<TeamRole> getRole(UUID uuid) {
    TeamMember member = members.get(uuid);
    return member != null ? Optional.of(member.getRole()) : Optional.empty();
  }

  public boolean setRole(UUID uuid, TeamRole role) {
    TeamMember member = members.get(uuid);
    if (member != null) {
      member.setRole(role);
      return true;
    }
    return false;
  }

  public int getSize() {
    return members.size();
  }

  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = Math.max(0, maxSize);
  }

  public boolean isFull() {
    return maxSize > 0 && members.size() >= maxSize;
  }

  public boolean isFriendlyFireEnabled() {
    return friendlyFire;
  }

  public void setFriendlyFire(boolean friendlyFire) {
    this.friendlyFire = friendlyFire;
  }

  public boolean isOpen() {
    return open;
  }

  public void setOpen(boolean open) {
    this.open = open;
  }

  public Location getHome() {
    return home;
  }

  public void setHome(Location home) {
    this.home = home != null ? home.clone() : null;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = Math.max(0.0, balance);
  }

  public synchronized void deposit(double amount) {
    if (amount > 0) {
      this.balance += amount;
    }
  }

  public synchronized boolean withdraw(double amount) {
    if (amount > 0 && this.balance >= amount) {
      this.balance -= amount;
      return true;
    }
    return false;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public Object getMetadata(String key) {
    return metadata.get(key);
  }

  public void setMetadata(String key, Object value) {
    if (value != null) {
      metadata.put(key, value);
    } else {
      metadata.remove(key);
    }
  }

  public void removeMetadata(String key) {
    metadata.remove(key);
  }

  public long getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns all players belonging to this team who are currently online.
   *
   * @return list of online players
   */
  public List<Player> getOnlinePlayers() {
    List<Player> list = new ArrayList<>();
    for (UUID uuid : members.keySet()) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null && player.isOnline()) {
        list.add(player);
      }
    }
    return list;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Team team)) return false;
    return id.equals(team.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return "Team{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", tag='"
        + tag
        + '\''
        + ", leader="
        + leader
        + ", members="
        + members.size()
        + '}';
  }
}
