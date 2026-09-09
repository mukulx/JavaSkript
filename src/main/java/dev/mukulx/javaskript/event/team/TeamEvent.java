package dev.mukulx.javaskript.event.team;

import dev.mukulx.javaskript.api.team.Team;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/** Abstract base event for all team-related actions and lifecycle occurrences. */
public abstract class TeamEvent extends Event {

  private final Team team;

  public TeamEvent(Team team) {
    this(team, Bukkit.getServer() != null && !Bukkit.isPrimaryThread());
  }

  public TeamEvent(Team team, boolean async) {
    super(async);
    this.team = Objects.requireNonNull(team, "team cannot be null");
  }

  /**
   * Retrieves the team associated with this event.
   *
   * @return Team instance
   */
  public Team getTeam() {
    return team;
  }
}
