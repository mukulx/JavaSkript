package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import java.util.Objects;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when a message is sent in team chat channel. */
public class TeamChatEvent extends TeamEvent implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final Player sender;
  private String message;
  private final Set<Player> recipients;
  private boolean cancelled = false;

  public TeamChatEvent(Team team, Player sender, String message, Set<Player> recipients) {
    super(team);
    this.sender = Objects.requireNonNull(sender, "sender cannot be null");
    this.message = Objects.requireNonNull(message, "message cannot be null");
    this.recipients = Objects.requireNonNull(recipients, "recipients cannot be null");
  }

  @NotNull
  public Player getSender() {
    return sender;
  }

  @NotNull
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = Objects.requireNonNull(message, "message cannot be null");
  }

  @NotNull
  public Set<Player> getRecipients() {
    return recipients;
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
