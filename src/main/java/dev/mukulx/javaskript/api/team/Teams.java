package dev.mukulx.javaskript.api.team;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Static shorthand facade for {@link TeamHelper}.
 *
 * <p>Enables 1-line team operations anywhere without injecting fields. Fully safe against null,
 * disabled states, and dynamic configuration reloads.
 */
public final class Teams {

  private static TeamHelper instance;

  private Teams() {}

  public static void setInstance(TeamHelper helper) {
    instance = helper;
  }

  private static TeamHelper get() {
    if (instance == null) {
      JavaSkriptPlugin plugin = JavaSkriptPlugin.getInstance();
      if (plugin != null) {
        instance = plugin.getTeamHelper();
      }
    }
    return instance;
  }

  /** Checks if the team subsystem is active and initialized. */
  public static boolean isEnabled() {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled();
  }

  /**
   * Fast O(1) teammate check between two players.
   *
   * @param a First player
   * @param b Second player
   * @return true if teammates and subsystem is enabled
   */
  public static boolean areTeammates(Player a, Player b) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.areTeammates(a, b);
  }

  /**
   * Fast O(1) teammate check between two UUIDs.
   *
   * @param a First player UUID
   * @param b Second player UUID
   * @return true if teammates and subsystem is enabled
   */
  public static boolean areTeammates(UUID a, UUID b) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.areTeammates(a, b);
  }

  /**
   * Retrieves a player's team.
   *
   * @param player Player
   * @return Optional containing the team
   */
  public static Optional<Team> get(Player player) {
    TeamHelper helper = get();
    return (helper != null && helper.isEnabled() && player != null)
        ? helper.getTeam(player)
        : Optional.empty();
  }

  /**
   * Retrieves a player's team by UUID.
   *
   * @param uuid Player UUID
   * @return Optional containing the team
   */
  public static Optional<Team> get(UUID uuid) {
    TeamHelper helper = get();
    return (helper != null && helper.isEnabled() && uuid != null)
        ? helper.getTeam(uuid)
        : Optional.empty();
  }

  /**
   * Retrieves a team by ID or display name.
   *
   * @param idOrName Team ID or name
   * @return Optional containing the team
   */
  public static Optional<Team> getByName(String idOrName) {
    TeamHelper helper = get();
    return (helper != null && helper.isEnabled() && idOrName != null)
        ? helper.getByName(idOrName)
        : Optional.empty();
  }

  /** Retrieves all active teams. */
  public static Collection<Team> getAll() {
    TeamHelper helper = get();
    return (helper != null && helper.isEnabled()) ? helper.getAll() : Collections.emptyList();
  }

  /** Checks if a team ID exists. */
  public static boolean exists(String teamId) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && teamId != null && helper.exists(teamId);
  }

  /**
   * Creates a new team.
   *
   * @param id Team ID
   * @param name Display name
   * @param leader Leader player
   * @return Created Team or null if disabled or cancelled
   */
  public static Team create(String id, String name, Player leader) {
    TeamHelper helper = get();
    return (helper != null && helper.isEnabled() && leader != null)
        ? helper.create(id, name, leader)
        : null;
  }

  /** Creates a new team with leader UUID. */
  public static Team create(String id, String name, UUID leader) {
    TeamHelper helper = get();
    return (helper != null && helper.isEnabled() && leader != null)
        ? helper.create(id, name, leader)
        : null;
  }

  /** Disbands a team. */
  public static boolean disband(Team team) {
    return disband(team, null);
  }

  /** Disbands a team with initiator UUID. */
  public static boolean disband(Team team, UUID initiator) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.disband(team, initiator);
  }

  /** Disbands a team by ID or name. */
  public static boolean disband(String idOrName) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.disband(idOrName, null);
  }

  /** Adds a player to a team. */
  public static boolean addMember(Team team, Player player, TeamRole role) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.addMember(team, player, role);
  }

  /** Adds a player UUID to a team. */
  public static boolean addMember(Team team, UUID player, TeamRole role) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.addMember(team, player, role);
  }

  /** Removes a player from a team. */
  public static boolean removeMember(Team team, Player player) {
    return player != null && removeMember(team, player.getUniqueId());
  }

  /** Removes a player UUID from a team. */
  public static boolean removeMember(Team team, UUID player) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.removeMember(team, player);
  }

  /** Removes a player from a team by team name/ID. */
  public static boolean removeMember(String idOrName, Player player) {
    return getByName(idOrName).map(t -> removeMember(t, player)).orElse(false);
  }

  /** Removes a player UUID from a team by team name/ID. */
  public static boolean removeMember(String idOrName, UUID player) {
    return getByName(idOrName).map(t -> removeMember(t, player)).orElse(false);
  }

  /** Kicks a player from a team. */
  public static boolean kick(Team team, Player target, Player kicker, String reason) {
    TeamHelper helper = get();
    UUID kickerId = kicker != null ? kicker.getUniqueId() : null;
    return helper != null
        && helper.isEnabled()
        && target != null
        && helper.kick(team, target.getUniqueId(), kickerId, reason);
  }

  /** Sets a member's role. */
  public static boolean setRole(Team team, UUID player, TeamRole role, UUID actor) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.setRole(team, player, role, actor);
  }

  /** Sends an invite to a player. */
  public static void invite(Team team, Player target, Player sender, long timeoutMillis) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.invite(team, target, sender, timeoutMillis);
    }
  }

  /** Sends an invite with default 60-second timeout. */
  public static void invite(Team team, Player target, Player sender) {
    invite(team, target, sender, 60000L);
  }

  /** Checks if player has an active invite to a team. */
  public static boolean hasInvite(Team team, Player target) {
    TeamHelper helper = get();
    return helper != null
        && helper.isEnabled()
        && target != null
        && helper.hasInvite(team, target.getUniqueId());
  }

  /** Accepts a pending invite. */
  public static boolean acceptInvite(Team team, Player target) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.acceptInvite(team, target);
  }

  /** Declines a pending invite. */
  public static void declineInvite(Team team, Player target) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.declineInvite(team, target);
    }
  }

  /** Broadcasts a component to team members. */
  public static void broadcast(Team team, Component message) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.broadcast(team, message);
    }
  }

  /** Broadcasts a MiniMessage or legacy string to team members. */
  public static void broadcast(Team team, String message) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.broadcast(team, message);
    }
  }

  /** Broadcasts an action bar to team members. */
  public static void broadcastActionBar(Team team, Component message) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.broadcastActionBar(team, message);
    }
  }

  /** Broadcasts an action bar string to team members. */
  public static void broadcastActionBar(Team team, String message) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.broadcastActionBar(team, message);
    }
  }

  /** Broadcasts a title to team members. */
  public static void broadcastTitle(Team team, Title title) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.broadcastTitle(team, title);
    }
  }

  /** Plays a sound effect for all team members. */
  public static void playSound(Team team, Sound sound, float volume, float pitch) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.playSound(team, sound, volume, pitch);
    }
  }

  /** Plays a sound effect with volume=1, pitch=1. */
  public static void playSound(Team team, Sound sound) {
    playSound(team, sound, 1.0f, 1.0f);
  }

  /** Sends a message into the team private chat. */
  public static boolean sendTeamChat(Player sender, String message) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.sendTeamChat(sender, message);
  }

  /** Deposits money into the team bank. */
  public static boolean deposit(Team team, Player player, double amount) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.deposit(team, player, amount);
  }

  /** Withdraws money from the team bank. */
  public static boolean withdraw(Team team, Player player, double amount) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.withdraw(team, player, amount);
  }

  /** Sets team home. */
  public static void setHome(Team team, Location location) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.setHome(team, location);
    }
  }

  /** Teleports a player to their team home. */
  public static boolean teleportHome(Team team, Player player) {
    TeamHelper helper = get();
    return helper != null && helper.isEnabled() && helper.teleportHome(team, player);
  }

  /** Gets all online players in a team. */
  public static List<Player> getOnlinePlayers(Team team) {
    TeamHelper helper = get();
    return (helper != null && helper.isEnabled() && team != null)
        ? helper.getOnlinePlayers(team)
        : Collections.emptyList();
  }

  /** Saves team state. */
  public static void save(Team team) {
    TeamHelper helper = get();
    if (helper != null && helper.isEnabled()) {
      helper.save(team);
    }
  }
}
