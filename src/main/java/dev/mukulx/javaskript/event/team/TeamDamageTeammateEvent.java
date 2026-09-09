package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when a player attempts to damage a teammate. */
public class TeamDamageTeammateEvent extends TeamEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final Player damager;
  private final Player victim;
  private double damage;
  private boolean cancelled = false;

  public TeamDamageTeammateEvent(Team team, Player damager, Player victim, double damage) {
    super(team);
    this.damager = Objects.requireNonNull(damager, "damager cannot be null");
    this.victim = Objects.requireNonNull(victim, "victim cannot be null");
    this.damage = damage;
  }

  @NotNull
  public Player getDamager() {
    return damager;
  }

  @NotNull
  public Player getVictim() {
    return victim;
  }

  public double getDamage() {
    return damage;
  }

  public void setDamage(double damage) {
    this.damage = damage;
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
