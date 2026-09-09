package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import dev.mukulx.javaskript.api.team.TeamRole;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired when a team member's role is being promoted or demoted. */
public class TeamRoleChangeEvent extends TeamEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final UUID memberUuid;
  private final TeamRole oldRole;
  private TeamRole newRole;
  private final UUID actorUuid;
  private boolean cancelled = false;

  public TeamRoleChangeEvent(
      Team team, UUID memberUuid, TeamRole oldRole, TeamRole newRole, UUID actorUuid) {
    super(team);
    this.memberUuid = memberUuid;
    this.oldRole = oldRole;
    this.newRole = newRole;
    this.actorUuid = actorUuid;
  }

  public UUID getMemberUuid() {
    return memberUuid;
  }

  @Nullable
  public Player getMemberPlayer() {
    return Bukkit.getPlayer(memberUuid);
  }

  public TeamRole getOldRole() {
    return oldRole;
  }

  public TeamRole getNewRole() {
    return newRole;
  }

  public void setNewRole(TeamRole newRole) {
    this.newRole = newRole;
  }

  @Nullable
  public UUID getActorUuid() {
    return actorUuid;
  }

  @Nullable
  public Player getActor() {
    return actorUuid != null ? Bukkit.getPlayer(actorUuid) : null;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
