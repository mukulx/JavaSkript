package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired when a team member is being kicked from the team. */
public class TeamKickEvent extends TeamEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final UUID kickedUuid;
  private final UUID kickerUuid;
  private String reason;
  private boolean cancelled = false;

  public TeamKickEvent(Team team, UUID kickedUuid, UUID kickerUuid, String reason) {
    super(team);
    this.kickedUuid = kickedUuid;
    this.kickerUuid = kickerUuid;
    this.reason = reason;
  }

  public UUID getKickedUuid() {
    return kickedUuid;
  }

  @Nullable
  public Player getKickedPlayer() {
    return Bukkit.getPlayer(kickedUuid);
  }

  @Nullable
  public UUID getKickerUuid() {
    return kickerUuid;
  }

  @Nullable
  public Player getKicker() {
    return kickerUuid != null ? Bukkit.getPlayer(kickerUuid) : null;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
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
