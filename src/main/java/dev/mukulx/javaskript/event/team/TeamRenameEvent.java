package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired when a team's display name or tag is modified. */
public class TeamRenameEvent extends TeamEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final String oldName;
  private String newName;
  private final UUID actorUuid;
  private boolean cancelled = false;

  public TeamRenameEvent(Team team, String oldName, String newName, UUID actorUuid) {
    super(team);
    this.oldName = oldName;
    this.newName = Objects.requireNonNull(newName, "newName cannot be null");
    this.actorUuid = actorUuid;
  }

  public String getOldName() {
    return oldName;
  }

  public String getNewName() {
    return newName;
  }

  public void setNewName(String newName) {
    this.newName = Objects.requireNonNull(newName, "newName cannot be null");
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
