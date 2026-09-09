package dev.mukulx.javaskript.api.team;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.team.storage.TeamStorage;
import dev.mukulx.javaskript.api.team.tab.TeamTabBridge;
import dev.mukulx.javaskript.event.team.TeamBankTransactionEvent;
import dev.mukulx.javaskript.event.team.TeamChatEvent;
import dev.mukulx.javaskript.event.team.TeamCreateEvent;
import dev.mukulx.javaskript.event.team.TeamDamageTeammateEvent;
import dev.mukulx.javaskript.event.team.TeamDisbandEvent;
import dev.mukulx.javaskript.event.team.TeamJoinEvent;
import dev.mukulx.javaskript.event.team.TeamKickEvent;
import dev.mukulx.javaskript.event.team.TeamLeaveEvent;
import dev.mukulx.javaskript.event.team.TeamRoleChangeEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Native Team, Clan, and Party manager for JavaSkript.
 *
 * <p>Provides zero-boilerplate access to team querying, lifecycle events, friendly fire prevention,
 * SQLite persistence, and visual tab/nametag synchronization. Fully resilient against nulls, edge
 * conditions, and dynamic reloads.
 */
public class TeamHelper implements Listener {

  private final JavaSkriptPlugin plugin;
  private final TeamStorage storage;
  private final TeamTabBridge tabBridge;
  private final Map<String, Team> teamsById;
  private final Map<UUID, String> playerToTeamId;
  private final Map<String, TeamInvite> pendingInvites;
  private volatile boolean enabled = true;

  public TeamHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.storage = new TeamStorage(plugin);
    this.teamsById = new ConcurrentHashMap<>();
    this.playerToTeamId = new ConcurrentHashMap<>();
    this.pendingInvites = new ConcurrentHashMap<>();

    // Load persisted teams from database
    Map<String, Team> loaded = storage.loadAll();
    this.teamsById.putAll(loaded);
    for (Team team : loaded.values()) {
      for (UUID memberUuid : team.getMemberUuids()) {
        playerToTeamId.put(memberUuid, team.getId());
      }
    }

    // Initialize visual integration bridge
    this.tabBridge = new TeamTabBridge(plugin, this::getTeam);

    // Register event listener for friendly fire and player join/quit
    Bukkit.getPluginManager().registerEvents(this, plugin);

    // Synchronize any currently online players
    for (Player online : Bukkit.getOnlinePlayers()) {
      tabBridge.updatePlayer(online);
    }

    plugin.getLogger().info("Team engine initialized (" + teamsById.size() + " active teams)");
  }

  /** Checks if the team subsystem is active. */
  public boolean isEnabled() {
    return enabled;
  }

  // -------------------------------------------------------------
  // Query Methods (O(1) in-memory lookups)
  // -------------------------------------------------------------

  /**
   * Retrieves the team of a player by their UUID.
   *
   * @param uuid Player UUID
   * @return Optional containing the Team if player is a member
   */
  public Optional<Team> getTeam(UUID uuid) {
    if (!enabled || uuid == null) return Optional.empty();
    String teamId = playerToTeamId.get(uuid);
    if (teamId == null) return Optional.empty();
    return Optional.ofNullable(teamsById.get(teamId));
  }

  /**
   * Retrieves the team of a player.
   *
   * @param player Player instance
   * @return Optional containing the Team if player is a member
   */
  public Optional<Team> getTeam(Player player) {
    return player != null ? getTeam(player.getUniqueId()) : Optional.empty();
  }

  /**
   * Retrieves a team by its unique ID or display name.
   *
   * @param idOrName The team ID or display name
   * @return Optional containing the Team if found
   */
  public Optional<Team> getByName(String idOrName) {
    if (!enabled || idOrName == null || idOrName.isBlank()) return Optional.empty();
    String key = idOrName.trim().toLowerCase();
    Team team = teamsById.get(key);
    if (team != null) {
      return Optional.of(team);
    }
    for (Team candidate : teamsById.values()) {
      if (candidate.getName().equalsIgnoreCase(idOrName.trim())) {
        return Optional.of(candidate);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieves an unmodifiable collection of all active teams.
   *
   * @return Collection of all teams
   */
  public Collection<Team> getAll() {
    if (!enabled) return Collections.emptyList();
    return Collections.unmodifiableCollection(teamsById.values());
  }

  /**
   * Checks whether a team with the given ID exists.
   *
   * @param teamId Team ID
   * @return true if exists
   */
  public boolean exists(String teamId) {
    if (!enabled || teamId == null) return false;
    return teamsById.containsKey(teamId.trim().toLowerCase());
  }

  /**
   * Ultra-fast O(1) teammate check. Ideal for high-frequency PvP listeners.
   *
   * @param a UUID of first player
   * @param b UUID of second player
   * @return true if both players are in the same team
   */
  public boolean areTeammates(UUID a, UUID b) {
    if (!enabled || a == null || b == null || a.equals(b)) return false;
    String teamA = playerToTeamId.get(a);
    if (teamA == null) return false;
    String teamB = playerToTeamId.get(b);
    return teamA.equals(teamB);
  }

  /**
   * Checks whether two players are on the same team.
   *
   * @param a First player
   * @param b Second player
   * @return true if teammates
   */
  public boolean areTeammates(Player a, Player b) {
    if (a == null || b == null) return false;
    return areTeammates(a.getUniqueId(), b.getUniqueId());
  }

  // -------------------------------------------------------------
  // Team Lifecycle Methods
  // -------------------------------------------------------------

  /**
   * Creates a new team with the given ID, display name, and leader.
   *
   * @param id Unique lowercase identifier
   * @param name Display name
   * @param leader Leader player UUID
   * @return The newly created Team, or null if disabled or cancelled by event
   */
  public Team create(String id, String name, UUID leader) {
    if (!enabled) return null;
    Objects.requireNonNull(id, "Team ID cannot be null");
    Objects.requireNonNull(leader, "Team leader cannot be null");

    String cleanId = id.trim().toLowerCase();
    if (cleanId.isEmpty() || cleanId.contains(" ")) {
      throw new IllegalArgumentException("Team ID must be non-empty and cannot contain spaces");
    }
    if (teamsById.containsKey(cleanId)) {
      throw new IllegalArgumentException("Team with ID '" + cleanId + "' already exists");
    }

    // Leave current team if leader is already in one
    getTeam(leader).ifPresent(oldTeam -> removeMember(oldTeam, leader));

    int defaultMaxSize = plugin.getConfig().getInt("teams.defaults.max-size", 0);
    boolean defaultFf = plugin.getConfig().getBoolean("teams.defaults.friendly-fire", false);
    boolean defaultOpen = plugin.getConfig().getBoolean("teams.defaults.open", false);

    Team team =
        new Team(
            cleanId,
            name != null && !name.isBlank() ? name : cleanId,
            cleanId.length() > 4
                ? "[" + cleanId.substring(0, 4).toUpperCase() + "]"
                : "[" + cleanId.toUpperCase() + "]",
            "",
            "",
            net.kyori.adventure.text.format.NamedTextColor.WHITE,
            leader,
            defaultFf,
            defaultOpen,
            defaultMaxSize,
            null,
            0.0,
            System.currentTimeMillis());

    // Fire Bukkit Event
    TeamCreateEvent event = new TeamCreateEvent(team, leader);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return null;
    }

    teamsById.put(cleanId, team);
    playerToTeamId.put(leader, cleanId);

    // Persist to database
    storage.saveTeamAsync(team);

    // Update visuals
    Player leaderPlayer = Bukkit.getPlayer(leader);
    if (leaderPlayer != null) {
      tabBridge.updatePlayer(leaderPlayer);
      plugin
          .getMessageManager()
          .send(
              leaderPlayer,
              "team.created",
              "<green>Team <aqua>{team}</aqua> created successfully!</green>",
              "{team}",
              team.getName());
    }

    return team;
  }

  /**
   * Creates a new team with the given leader player.
   *
   * @param id Team ID
   * @param name Display name
   * @param leader Leader player
   * @return The created Team
   */
  public Team create(String id, String name, Player leader) {
    if (!enabled || leader == null) return null;
    return create(id, name, leader.getUniqueId());
  }

  /**
   * Disbands an existing team.
   *
   * @param team The team to disband
   * @param initiator Initiator UUID (nullable)
   * @return true if disbanded successfully
   */
  public boolean disband(Team team, UUID initiator) {
    if (!enabled || team == null) return false;

    // Fire Bukkit Event
    TeamDisbandEvent event = new TeamDisbandEvent(team, initiator);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return false;
    }

    // Unmap all members
    for (UUID memberUuid : team.getMemberUuids()) {
      playerToTeamId.remove(memberUuid);
      Player p = Bukkit.getPlayer(memberUuid);
      if (p != null) {
        tabBridge.updatePlayer(p);
        plugin.getMessageManager().send(p, "team.disbanded", "<red>Your team was disbanded.</red>");
      }
    }

    // Clean up scoreboard team
    tabBridge.removeScoreboardTeam(team.getId());

    // Remove from memory and database
    teamsById.remove(team.getId());
    storage.deleteTeamAsync(team.getId());

    return true;
  }

  /**
   * Disbands a team by ID or name.
   *
   * @param idOrName The team ID or name
   * @param initiator Initiator UUID (nullable)
   * @return true if disbanded
   */
  public boolean disband(String idOrName, UUID initiator) {
    if (!enabled || idOrName == null) return false;
    return getByName(idOrName).map(team -> disband(team, initiator)).orElse(false);
  }

  // -------------------------------------------------------------
  // Member Management
  // -------------------------------------------------------------

  /**
   * Adds a member to a team.
   *
   * @param team Target team
   * @param playerUuid Player UUID
   * @param role Assigned role
   * @return true if member added successfully
   */
  public boolean addMember(Team team, UUID playerUuid, TeamRole role) {
    if (!enabled || team == null || playerUuid == null) return false;

    if (team.isFull()) {
      Player p = Bukkit.getPlayer(playerUuid);
      if (p != null) {
        plugin.getMessageManager().send(p, "team.full", "<red>This team is full!</red>");
      }
      return false;
    }

    // Leave existing team if present
    getTeam(playerUuid).ifPresent(oldTeam -> removeMember(oldTeam, playerUuid));

    TeamRole assignedRole = role != null ? role : TeamRole.MEMBER;

    // Fire Bukkit Event
    TeamJoinEvent event = new TeamJoinEvent(team, playerUuid, assignedRole);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return false;
    }

    TeamMember member = new TeamMember(playerUuid, event.getRole());
    team.addMember(member);
    playerToTeamId.put(playerUuid, team.getId());

    // Persist
    storage.saveMemberAsync(team.getId(), member);

    // Visuals & Notifications
    Player player = Bukkit.getPlayer(playerUuid);
    if (player != null) {
      tabBridge.updatePlayer(player);
      plugin
          .getMessageManager()
          .send(
              player,
              "team.joined",
              "<green>You have joined <aqua>{team}</aqua>!</green>",
              "{team}",
              team.getName());
    }

    String name = player != null ? player.getName() : playerUuid.toString();
    broadcast(
        team,
        plugin
            .getMessageManager()
            .get(
                "team.member-joined",
                "<green>{player} has joined the team!</green>",
                "{player}",
                name));

    return true;
  }

  /** Adds an online player to a team. */
  public boolean addMember(Team team, Player player, TeamRole role) {
    return player != null && addMember(team, player.getUniqueId(), role);
  }

  /**
   * Removes a member from a team.
   *
   * @param team The team
   * @param playerUuid Player UUID
   * @return true if removed successfully
   */
  public boolean removeMember(Team team, UUID playerUuid) {
    if (!enabled || team == null || playerUuid == null || !team.hasMember(playerUuid)) return false;

    // Fire Bukkit Event
    TeamLeaveEvent event = new TeamLeaveEvent(team, playerUuid);
    Bukkit.getPluginManager().callEvent(event);

    team.removeMember(playerUuid);
    playerToTeamId.remove(playerUuid);

    // Persist
    storage.removeMemberAsync(playerUuid);

    // Visuals
    Player player = Bukkit.getPlayer(playerUuid);
    if (player != null) {
      tabBridge.updatePlayer(player);
      plugin
          .getMessageManager()
          .send(
              player,
              "team.left",
              "<yellow>You have left <aqua>{team}</aqua>.</yellow>",
              "{team}",
              team.getName());
    }

    String name = player != null ? player.getName() : playerUuid.toString();
    broadcast(
        team,
        plugin
            .getMessageManager()
            .get(
                "team.member-left",
                "<yellow>{player} has left the team.</yellow>",
                "{player}",
                name));

    // If team has no members left or leader left, handle disband or promotion
    if (team.getSize() == 0) {
      disband(team, null);
    } else if (team.isLeader(playerUuid)) {
      // Auto-promote highest ranking remaining member
      TeamMember highest = null;
      for (TeamMember m : team.getMembers().values()) {
        if (highest == null || m.getRole().isHigherThan(highest.getRole())) {
          highest = m;
        }
      }
      if (highest != null) {
        team.setLeader(highest.getUniqueId());
        storage.saveTeamAsync(team);
        Player newLead = Bukkit.getPlayer(highest.getUniqueId());
        if (newLead != null) {
          tabBridge.updatePlayer(newLead);
        }
        broadcast(
            team,
            plugin
                .getMessageManager()
                .get(
                    "team.role-changed",
                    "<green>{player} is the new team leader!</green>",
                    "{player}",
                    highest.getName(),
                    "{role}",
                    "Leader"));
      }
    }

    return true;
  }

  /**
   * Kicks a member from a team.
   *
   * @param team The team
   * @param targetUuid Member UUID to kick
   * @param kickerUuid Kicker UUID (nullable)
   * @param reason Reason string (nullable)
   * @return true if kicked
   */
  public boolean kick(Team team, UUID targetUuid, UUID kickerUuid, String reason) {
    if (!enabled || team == null || targetUuid == null || !team.hasMember(targetUuid)) return false;
    if (team.isLeader(targetUuid)) return false; // Leader cannot be kicked

    // Fire Bukkit Event
    TeamKickEvent event = new TeamKickEvent(team, targetUuid, kickerUuid, reason);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return false;
    }

    team.removeMember(targetUuid);
    playerToTeamId.remove(targetUuid);
    storage.removeMemberAsync(targetUuid);

    Player target = Bukkit.getPlayer(targetUuid);
    if (target != null) {
      tabBridge.updatePlayer(target);
      plugin
          .getMessageManager()
          .send(
              target,
              "team.kicked",
              "<red>You were kicked from {team}. Reason: {reason}</red>",
              "{team}",
              team.getName(),
              "{reason}",
              event.getReason() != null ? event.getReason() : "No reason provided");
    }

    String targetName = target != null ? target.getName() : targetUuid.toString();
    broadcast(
        team,
        plugin
            .getMessageManager()
            .get(
                "team.member-kicked",
                "<red>{player} was kicked from the team.</red>",
                "{player}",
                targetName));

    return true;
  }

  /**
   * Promotes or demotes a team member's role.
   *
   * @param team The team
   * @param memberUuid The member UUID
   * @param newRole The new role
   * @param actor The player modifying the role (nullable)
   * @return true if role changed successfully
   */
  public boolean setRole(Team team, UUID memberUuid, TeamRole newRole, UUID actor) {
    if (!enabled || team == null || memberUuid == null || newRole == null) return false;
    Optional<TeamMember> memberOpt = team.getMember(memberUuid);
    if (memberOpt.isEmpty()) return false;

    TeamMember member = memberOpt.get();
    TeamRole oldRole = member.getRole();
    if (oldRole == newRole) return true;

    // Fire Bukkit Event
    TeamRoleChangeEvent event = new TeamRoleChangeEvent(team, memberUuid, oldRole, newRole, actor);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return false;
    }

    member.setRole(event.getNewRole());
    storage.saveMemberAsync(team.getId(), member);

    Player player = Bukkit.getPlayer(memberUuid);
    if (player != null) {
      tabBridge.updatePlayer(player);
      plugin
          .getMessageManager()
          .send(
              player,
              "team.role-changed",
              "<green>Your team role was changed to <aqua>{role}</aqua>.</green>",
              "{role}",
              event.getNewRole().getDisplayName());
    }

    return true;
  }

  // -------------------------------------------------------------
  // Invites
  // -------------------------------------------------------------

  /**
   * Sends an invitation to a player to join a team.
   *
   * @param team The team
   * @param target Target player
   * @param sender Inviting player
   * @param timeoutMillis Expiration duration in milliseconds
   */
  public void invite(Team team, Player target, Player sender, long timeoutMillis) {
    if (!enabled || team == null || target == null || sender == null) return;

    if (target.equals(sender)) {
      plugin
          .getMessageManager()
          .send(sender, "team.invite-self", "<red>You cannot invite yourself!</red>");
      return;
    }

    if (team.hasMember(target.getUniqueId())) {
      plugin
          .getMessageManager()
          .send(
              sender,
              "team.invite-already-member",
              "<red>{target} is already in your team!</red>",
              "{target}",
              target.getName());
      return;
    }

    if (team.isFull()) {
      plugin.getMessageManager().send(sender, "team.full", "<red>Your team is full!</red>");
      return;
    }

    String inviteKey = team.getId() + ":" + target.getUniqueId();
    TeamInvite invite =
        new TeamInvite(team.getId(), target.getUniqueId(), sender.getUniqueId(), timeoutMillis);
    pendingInvites.put(inviteKey, invite);

    // Notify sender
    plugin
        .getMessageManager()
        .send(
            sender,
            "team.invite-sent",
            "<green>Invited <white>{target}</white> to <aqua>{team}</aqua>!</green>",
            "{target}",
            target.getName(),
            "{team}",
            team.getName());

    // Notify target
    plugin
        .getMessageManager()
        .send(
            target,
            "team.invite-received",
            "<yellow>{player} invited you to join <aqua>{team}</aqua>! Type /team join {team}</yellow>",
            "{player}",
            sender.getName(),
            "{team}",
            team.getId());
  }

  /** Checks if a player has an active, non-expired invite to a team. */
  public boolean hasInvite(Team team, UUID targetUuid) {
    if (!enabled || team == null || targetUuid == null) return false;
    String inviteKey = team.getId() + ":" + targetUuid;
    TeamInvite invite = pendingInvites.get(inviteKey);
    if (invite == null) return false;
    if (invite.isExpired()) {
      pendingInvites.remove(inviteKey);
      return false;
    }
    return true;
  }

  /** Accepts an active team invite. */
  public boolean acceptInvite(Team team, Player target) {
    if (!enabled || team == null || target == null) return false;

    if (team.isFull()) {
      plugin.getMessageManager().send(target, "team.full", "<red>This team is full!</red>");
      return false;
    }

    String inviteKey = team.getId() + ":" + target.getUniqueId();
    TeamInvite invite = pendingInvites.remove(inviteKey);
    if (invite == null || invite.isExpired()) {
      plugin
          .getMessageManager()
          .send(
              target,
              "team.invite-expired",
              "<red>This team invitation has expired or does not exist.</red>");
      return false;
    }
    return addMember(team, target.getUniqueId(), TeamRole.MEMBER);
  }

  /** Declines an active team invite. */
  public void declineInvite(Team team, Player target) {
    if (!enabled || team == null || target == null) return;
    String inviteKey = team.getId() + ":" + target.getUniqueId();
    pendingInvites.remove(inviteKey);
    plugin
        .getMessageManager()
        .send(target, "team.invite-declined", "<gray>Team invitation declined.</gray>");
  }

  // -------------------------------------------------------------
  // Broadcasts & Messaging
  // -------------------------------------------------------------

  /** Broadcasts an Adventure Component to all online team members. */
  public void broadcast(Team team, Component message) {
    if (!enabled || team == null || message == null) return;
    for (Player player : team.getOnlinePlayers()) {
      player.sendMessage(message);
    }
  }

  /** Broadcasts a formatted MiniMessage or legacy string to all online team members. */
  public void broadcast(Team team, String message) {
    if (!enabled || team == null || message == null) return;
    broadcast(team, plugin.getMessageManager().parse(message));
  }

  /** Broadcasts an action bar message to all online team members. */
  public void broadcastActionBar(Team team, Component message) {
    if (!enabled || team == null || message == null) return;
    for (Player player : team.getOnlinePlayers()) {
      player.sendActionBar(message);
    }
  }

  /** Broadcasts an action bar MiniMessage or legacy string to all online team members. */
  public void broadcastActionBar(Team team, String message) {
    if (!enabled || team == null || message == null) return;
    broadcastActionBar(team, plugin.getMessageManager().parse(message));
  }

  /** Broadcasts a Title to all online team members. */
  public void broadcastTitle(Team team, Title title) {
    if (!enabled || team == null || title == null) return;
    for (Player player : team.getOnlinePlayers()) {
      player.showTitle(title);
    }
  }

  /** Plays a sound effect for all online team members at their respective locations. */
  public void playSound(Team team, Sound sound, float volume, float pitch) {
    if (!enabled || team == null || sound == null) return;
    for (Player player : team.getOnlinePlayers()) {
      player.playSound(player.getLocation(), sound, volume, pitch);
    }
  }

  /**
   * Sends a message into the private team chat channel.
   *
   * @param sender The player sending the message
   * @param message The chat text
   * @return true if chat delivered
   */
  public boolean sendTeamChat(Player sender, String message) {
    if (!enabled || sender == null || message == null || message.isBlank()) return false;
    Optional<Team> teamOpt = getTeam(sender);
    if (teamOpt.isEmpty()) {
      plugin
          .getMessageManager()
          .send(sender, "team.not-in-team", "<red>You are not in a team!</red>");
      return false;
    }

    Team team = teamOpt.get();
    Set<Player> recipients = new HashSet<>(team.getOnlinePlayers());

    // Fire Bukkit Event
    TeamChatEvent event = new TeamChatEvent(team, sender, message, recipients);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return false;
    }

    String roleName =
        team.getRole(sender.getUniqueId()).map(TeamRole::getDisplayName).orElse("Member");
    String format =
        plugin
            .getMessageManager()
            .getRaw(
                "team.chat-format",
                "<dark_gray>[<aqua>Team</aqua>]</dark_gray> <gray>{role}</gray> <white>{player}</white><gray>:</gray> <white>{message}</white>");

    String formatted =
        format
            .replace("{role}", roleName)
            .replace("{player}", sender.getName())
            .replace("{team}", team.getName())
            .replace("{tag}", team.getTag())
            .replace("{message}", event.getMessage());

    Component component = plugin.getMessageManager().parse(formatted);
    for (Player recipient : event.getRecipients()) {
      recipient.sendMessage(component);
    }

    return true;
  }

  // -------------------------------------------------------------
  // Bank & Home
  // -------------------------------------------------------------

  /**
   * Deposits money into the team bank.
   *
   * @param team The team
   * @param player The depositing player
   * @param amount The deposit amount
   * @return true if deposited successfully
   */
  public boolean deposit(Team team, Player player, double amount) {
    if (!enabled || team == null || player == null || amount <= 0) return false;

    // Fire Bukkit Event
    TeamBankTransactionEvent event =
        new TeamBankTransactionEvent(
            team, player.getUniqueId(), amount, TeamBankTransactionEvent.TransactionType.DEPOSIT);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return false;
    }

    team.deposit(event.getAmount());
    storage.saveTeamAsync(team);

    plugin
        .getMessageManager()
        .send(
            player,
            "team.bank-deposit",
            "<green>Deposited <gold>{amount}</gold> into the team bank!</green>",
            "{amount}",
            String.format("%.2f", event.getAmount()));
    return true;
  }

  /**
   * Withdraws money from the team bank.
   *
   * @param team The team
   * @param player The withdrawing player
   * @param amount The withdrawal amount
   * @return true if withdrawn successfully
   */
  public boolean withdraw(Team team, Player player, double amount) {
    if (!enabled || team == null || player == null || amount <= 0) return false;
    if (team.getBalance() < amount) {
      plugin
          .getMessageManager()
          .send(
              player,
              "team.bank-insufficient",
              "<red>Insufficient team funds! Current balance: {balance}</red>",
              "{balance}",
              String.format("%.2f", team.getBalance()));
      return false;
    }

    // Fire Bukkit Event
    TeamBankTransactionEvent event =
        new TeamBankTransactionEvent(
            team, player.getUniqueId(), amount, TeamBankTransactionEvent.TransactionType.WITHDRAW);
    Bukkit.getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return false;
    }

    if (team.withdraw(event.getAmount())) {
      storage.saveTeamAsync(team);
      plugin
          .getMessageManager()
          .send(
              player,
              "team.bank-withdraw",
              "<green>Withdrew <gold>{amount}</gold> from the team bank.</green>",
              "{amount}",
              String.format("%.2f", event.getAmount()));
      return true;
    }
    return false;
  }

  /** Sets the team home location. */
  public void setHome(Team team, Location location) {
    if (!enabled || team == null || location == null) return;
    team.setHome(location);
    storage.saveTeamAsync(team);
  }

  /**
   * Teleports a player to their team home location (Folia-safe).
   *
   * @param team The team
   * @param player The player
   * @return true if teleport initiated
   */
  public boolean teleportHome(Team team, Player player) {
    if (!enabled || team == null || player == null) return false;
    Location home = team.getHome();
    if (home == null || home.getWorld() == null) {
      plugin
          .getMessageManager()
          .send(player, "team.home-not-set", "<red>Your team does not have a home set!</red>");
      return false;
    }

    try {
      player.teleportAsync(home);
    } catch (Throwable t) {
      player.teleport(home);
    }

    plugin
        .getMessageManager()
        .send(player, "team.teleported-home", "<green>Teleported to team home!</green>");
    return true;
  }

  /** Returns all online players in a team. */
  public List<Player> getOnlinePlayers(Team team) {
    if (!enabled || team == null) return Collections.emptyList();
    return team.getOnlinePlayers();
  }

  /** Saves a team to persistent storage. */
  public void save(Team team) {
    if (enabled && team != null) {
      storage.saveTeamAsync(team);
    }
  }

  /** Reloads visual configurations and message bridges. */
  public void reload() {
    if (!enabled) return;
    for (Player player : Bukkit.getOnlinePlayers()) {
      tabBridge.updatePlayer(player);
    }
  }

  /** Shuts down the team manager, visual bridges, and SQLite connections. */
  public void shutdown() {
    this.enabled = false;
    HandlerList.unregisterAll(this);
    tabBridge.shutdown();
    storage.close();
    teamsById.clear();
    playerToTeamId.clear();
    pendingInvites.clear();
  }

  public TeamTabBridge getTabBridge() {
    return tabBridge;
  }

  // -------------------------------------------------------------
  // Bukkit Listeners (Friendly Fire, Tab Synchronization)
  // -------------------------------------------------------------

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!enabled) return;
    if (!(event.getEntity() instanceof Player victim)) {
      return;
    }

    Player damager = null;
    Entity attacker = event.getDamager();

    if (attacker instanceof Player p) {
      damager = p;
    } else if (attacker instanceof Projectile proj && proj.getShooter() instanceof Player p) {
      damager = p;
    } else if (attacker instanceof Tameable tameable
        && tameable.getOwner() instanceof Player owner) {
      damager = owner;
    }

    if (damager == null || damager.equals(victim)) {
      return;
    }

    // Check if damager and victim are teammates
    if (!areTeammates(damager, victim)) {
      return;
    }

    Team team = getTeam(damager).orElse(null);
    if (team == null) {
      return;
    }

    // If friendly fire is disabled for this team, prevent damage
    if (!team.isFriendlyFireEnabled()) {
      // Fire Bukkit cancellable event so scripts can override if desired
      TeamDamageTeammateEvent ffEvent =
          new TeamDamageTeammateEvent(team, damager, victim, event.getDamage());
      Bukkit.getPluginManager().callEvent(ffEvent);

      if (!ffEvent.isCancelled()) {
        event.setCancelled(true);
        plugin
            .getMessageManager()
            .send(
                damager, "team.friendly-fire-blocked", "<red>You cannot hurt your teammate!</red>");
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    if (!enabled) return;
    Player player = event.getPlayer();
    getTeam(player)
        .ifPresent(
            team -> {
              team.getMember(player.getUniqueId()).ifPresent(TeamMember::updateLastActive);
            });
    tabBridge.updatePlayer(player);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (!enabled) return;
    Player player = event.getPlayer();
    getTeam(player)
        .ifPresent(
            team -> {
              team.getMember(player.getUniqueId())
                  .ifPresent(
                      member -> {
                        member.updateLastActive();
                        storage.saveMemberAsync(team.getId(), member);
                      });
            });
    tabBridge.removeFromScoreboard(player);
  }
}
