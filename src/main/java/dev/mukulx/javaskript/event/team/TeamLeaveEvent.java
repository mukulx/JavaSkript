package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired when a player voluntarily leaves a team. */
public class TeamLeaveEvent extends TeamEvent {

  private static final HandlerList HANDLERS = new HandlerList();
  private final UUID playerUuid;

  public TeamLeaveEvent(Team team, UUID playerUuid) {
    super(team);
    this.playerUuid = playerUuid;
  }

  public UUID getPlayerUuid() {
    return playerUuid;
  }

  @Nullable
  public Player getPlayer() {
    return Bukkit.getPlayer(playerUuid);
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
