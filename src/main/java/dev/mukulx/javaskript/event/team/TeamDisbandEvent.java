package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired when a team is being disbanded. */
public class TeamDisbandEvent extends TeamEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final UUID initiatorUuid;
  private boolean cancelled = false;

  public TeamDisbandEvent(Team team, UUID initiatorUuid) {
    super(team);
    this.initiatorUuid = initiatorUuid;
  }

  @Nullable
  public UUID getInitiatorUuid() {
    return initiatorUuid;
  }

  @Nullable
  public Player getInitiator() {
    return initiatorUuid != null ? Bukkit.getPlayer(initiatorUuid) : null;
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
