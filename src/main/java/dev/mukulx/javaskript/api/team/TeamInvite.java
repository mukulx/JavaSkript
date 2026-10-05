package dev.mukulx.javaskript.api.team;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Represents a pending team invitation with expiration tracking. */
public class TeamInvite {

  private final String teamId;
  private final UUID targetUuid;
  private final UUID inviterUuid;
  private final long createdAt;
  private final long expirationMillis;

  public TeamInvite(String teamId, UUID targetUuid, UUID inviterUuid, long expirationMillis) {
    this.teamId = Objects.requireNonNull(teamId, "teamId cannot be null").toLowerCase();
    this.targetUuid = Objects.requireNonNull(targetUuid, "targetUuid cannot be null");
    this.inviterUuid = Objects.requireNonNull(inviterUuid, "inviterUuid cannot be null");
    this.createdAt = System.currentTimeMillis();
    this.expirationMillis = Math.max(1000L, expirationMillis);
  }

  public String getTeamId() {
    return teamId;
  }

  public UUID getTargetUuid() {
    return targetUuid;
  }

  public UUID getInviterUuid() {
    return inviterUuid;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getExpirationMillis() {
    return expirationMillis;
  }

  public boolean isExpired() {
    return System.currentTimeMillis() > (createdAt + expirationMillis);
  }

  public Player getTargetPlayer() {
    return Bukkit.getPlayer(targetUuid);
  }

  public Player getInviterPlayer() {
    return Bukkit.getPlayer(inviterUuid);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TeamInvite that)) return false;
    return teamId.equals(that.teamId) && targetUuid.equals(that.targetUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(teamId, targetUuid);
  }

  @Override
  public String toString() {
    return "TeamInvite{"
        + "teamId='"
        + teamId
        + '\''
        + ", targetUuid="
        + targetUuid
        + ", inviterUuid="
        + inviterUuid
        + ", expired="
        + isExpired()
        + '}';
  }
}
