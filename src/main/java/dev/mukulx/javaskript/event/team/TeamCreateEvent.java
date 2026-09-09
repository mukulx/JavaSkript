package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired when a new team is being created. */
public class TeamCreateEvent extends TeamEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final UUID creatorUuid;
  private boolean cancelled = false;

  public TeamCreateEvent(Team team, UUID creatorUuid) {
    super(team);
    this.creatorUuid = creatorUuid;
  }

  public UUID getCreatorUuid() {
    return creatorUuid;
  }

  @Nullable
  public Player getCreator() {
    return Bukkit.getPlayer(creatorUuid);
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
