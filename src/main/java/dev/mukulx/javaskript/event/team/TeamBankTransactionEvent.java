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

/** Fired when money is deposited into or withdrawn from a team bank account. */
public class TeamBankTransactionEvent extends TeamEvent implements Cancellable {

  public enum TransactionType {
    DEPOSIT,
    WITHDRAW
  }

  private static final HandlerList HANDLERS = new HandlerList();
  private final UUID playerUuid;
  private double amount;
  private final TransactionType type;
  private boolean cancelled = false;

  public TeamBankTransactionEvent(Team team, UUID playerUuid, double amount, TransactionType type) {
    super(team);
    this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
    this.amount = amount;
    this.type = Objects.requireNonNull(type, "type cannot be null");
  }

  public UUID getPlayerUuid() {
    return playerUuid;
  }

  @Nullable
  public Player getPlayer() {
    return Bukkit.getPlayer(playerUuid);
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public TransactionType getType() {
    return type;
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
