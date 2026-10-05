package dev.mukulx.javaskript.api.team;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/** Represents a member of a team with their role and membership timestamps. */
public class TeamMember {

  private final UUID uuid;
  private volatile TeamRole role;
  private final long joinTimestamp;
  private volatile long lastActiveTimestamp;

  public TeamMember(UUID uuid, TeamRole role) {
    this(uuid, role, System.currentTimeMillis(), System.currentTimeMillis());
  }

  public TeamMember(UUID uuid, TeamRole role, long joinTimestamp, long lastActiveTimestamp) {
    this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
    this.role = Objects.requireNonNull(role, "role cannot be null");
    this.joinTimestamp = joinTimestamp;
    this.lastActiveTimestamp = lastActiveTimestamp;
  }

  public UUID getUniqueId() {
    return uuid;
  }

  public TeamRole getRole() {
    return role;
  }

  public void setRole(TeamRole role) {
    this.role = Objects.requireNonNull(role, "role cannot be null");
  }

  public long getJoinTimestamp() {
    return joinTimestamp;
  }

  public long getLastActiveTimestamp() {
    return lastActiveTimestamp;
  }

  public void updateLastActive() {
    this.lastActiveTimestamp = System.currentTimeMillis();
  }

  public void setLastActiveTimestamp(long timestamp) {
    this.lastActiveTimestamp = timestamp;
  }

  /**
   * Retrieves the bukkit Player if they are currently online.
   *
   * @return Player instance if online, or null
   */
  public Player getPlayer() {
    return Bukkit.getPlayer(uuid);
  }

  /**
   * Retrieves the OfflinePlayer representation of this member.
   *
   * @return OfflinePlayer instance
   */
  public OfflinePlayer getOfflinePlayer() {
    return Bukkit.getOfflinePlayer(uuid);
  }

  /**
   * Checks whether this member is currently online on the server.
   *
   * @return true if online
   */
  public boolean isOnline() {
    Player player = getPlayer();
    return player != null && player.isOnline();
  }

  /**
   * Gets the last known username of this member.
   *
   * @return player name or UUID string if unavailable
   */
  public String getName() {
    Player player = getPlayer();
    if (player != null) {
      return player.getName();
    }
    String offlineName = getOfflinePlayer().getName();
    return offlineName != null ? offlineName : uuid.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TeamMember that)) return false;
    return uuid.equals(that.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public String toString() {
    return "TeamMember{"
        + "uuid="
        + uuid
        + ", role="
        + role
        + ", joinTimestamp="
        + joinTimestamp
        + '}';
  }
}
