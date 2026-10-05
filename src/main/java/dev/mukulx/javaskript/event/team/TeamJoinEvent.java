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

/** Fired when a player is joining a team. */
public class TeamJoinEvent extends TeamEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final UUID playerUuid;
  private final TeamRole role;
  private boolean cancelled = false;

  public TeamJoinEvent(Team team, UUID playerUuid, TeamRole role) {
    super(team);
    this.playerUuid = playerUuid;
    this.role = role;
  }

  public UUID getPlayerUuid() {
    return playerUuid;
  }

  @Nullable
  public Player getPlayer() {
    return Bukkit.getPlayer(playerUuid);
  }

  public TeamRole getRole() {
    return role;
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
