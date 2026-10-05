package examples;

import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.api.player.Players;
import dev.mukulx.javaskript.api.team.Team;
import dev.mukulx.javaskript.api.team.TeamHelper;
import dev.mukulx.javaskript.api.team.TeamRole;
import dev.mukulx.javaskript.api.team.Teams;
import dev.mukulx.javaskript.event.team.TeamDamageTeammateEvent;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Universal Team & Clan System Example for JavaSkript.
 *
 * <p>Demonstrates:
 *
 * <ul>
 *   <li>Native Team API and static Teams facade
 *   <li>Team creation, invitation, and membership management
 *   <li>Automatic friendly fire protection
 *   <li>Team private chat channel
 *   <li>Shared team home waypoint and Folia-safe teleportation
 *   <li>Team shared bank account
 *   <li>Vanilla nametag / tab list integration
 * </ul>
 */
public class TeamExample implements Listener {

  // Auto-injected by JavaSkript
  private TeamHelper teams;
  private CommandHelper commands;

  public void onEnable() {
    Bukkit.getLogger().info("[TeamExample] Team management script loaded.");

    // Command: /team
    commands
        .create("team")
        .aliases("clan", "party", "t")
        .description("Team management command")
        .playerOnly()
        .optionalArgument(
            dev.mukulx.javaskript.api.command.CommandArgs.choice(
                "action",
                () ->
                    java.util.List.of(
                        "create",
                        "invite",
                        "join",
                        "leave",
                        "kick",
                        "sethome",
                        "home",
                        "chat",
                        "deposit",
                        "withdraw",
                        "ff",
                        "info")))
        .executesPlayer(
            (player, ctx) -> {
              if (ctx.args().length == 0) {
                sendHelp(player);
                return;
              }

              String sub = ctx.arg(0).toLowerCase();

              switch (sub) {
                case "create" -> handleCreate(player, ctx.args());
                case "invite" -> handleInvite(player, ctx.args());
                case "join" -> handleJoin(player, ctx.args());
                case "leave" -> handleLeave(player);
                case "kick" -> handleKick(player, ctx.args());
                case "sethome" -> handleSetHome(player);
                case "home" -> handleHome(player);
                case "chat", "c" -> handleChat(player, ctx.args());
                case "deposit", "dep" -> handleDeposit(player, ctx.args());
                case "withdraw", "with" -> handleWithdraw(player, ctx.args());
                case "ff", "friendlyfire" -> handleToggleFriendlyFire(player);
                case "info" -> handleInfo(player);
                default -> sendHelp(player);
              }
            })
        .register();
  }

  private void sendHelp(Player player) {
    Players.msg(player, "<gold><bold>=== Team Commands ===</bold></gold>");
    Players.msg(
        player, "<yellow>/team create <id> <name></yellow> <gray>- Create a new team</gray>");
    Players.msg(player, "<yellow>/team invite <player></yellow> <gray>- Invite a player</gray>");
    Players.msg(player, "<yellow>/team join <id></yellow> <gray>- Accept invite and join</gray>");
    Players.msg(player, "<yellow>/team leave</yellow> <gray>- Leave your team</gray>");
    Players.msg(player, "<yellow>/team kick <player></yellow> <gray>- Kick a member</gray>");
    Players.msg(player, "<yellow>/team sethome</yellow> <gray>- Set team home waypoint</gray>");
    Players.msg(player, "<yellow>/team home</yellow> <gray>- Teleport to team home</gray>");
    Players.msg(player, "<yellow>/team chat <msg></yellow> <gray>- Send team chat message</gray>");
    Players.msg(
        player, "<yellow>/team deposit <amount></yellow> <gray>- Deposit into team bank</gray>");
    Players.msg(
        player, "<yellow>/team withdraw <amount></yellow> <gray>- Withdraw from bank</gray>");
    Players.msg(player, "<yellow>/team ff</yellow> <gray>- Toggle friendly fire</gray>");
    Players.msg(player, "<yellow>/team info</yellow> <gray>- View team information</gray>");
  }

  private void handleCreate(Player player, String[] args) {
    if (args.length < 2) {
      Players.msg(player, "<red>Usage: /team create <id> <name></red>");
      return;
    }
    String id = args[1].toLowerCase();
    String name =
        args.length >= 3
            ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
            : id;

    if (Teams.exists(id)) {
      Players.msg(player, "<red>A team with ID '" + id + "' already exists.</red>");
      return;
    }

    try {
      Team team = Teams.create(id, name, player);
      if (team != null) {
        Players.msg(
            player,
            "<green>Team <aqua>" + team.getName() + "</aqua> created successfully!</green>");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
      }
    } catch (IllegalArgumentException e) {
      Players.msg(player, "<red>" + e.getMessage() + "</red>");
    }
  }

  private void handleInvite(Player player, String[] args) {
    if (args.length < 2) {
      Players.msg(player, "<red>Usage: /team invite <player></red>");
      return;
    }
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      Players.msg(player, "<red>Only captains and leaders can invite players.</red>");
      return;
    }

    Player target = Bukkit.getPlayer(args[1]);
    if (target == null || !target.isOnline()) {
      Players.msg(player, "<red>Player not found or offline.</red>");
      return;
    }

    if (team.hasMember(target.getUniqueId())) {
      Players.msg(player, "<red>" + target.getName() + " is already in your team.</red>");
      return;
    }

    Teams.invite(team, target, player);
  }

  private void handleJoin(Player player, String[] args) {
    if (args.length < 2) {
      Players.msg(player, "<red>Usage: /team join <id></red>");
      return;
    }
    String teamId = args[1].toLowerCase();
    Optional<Team> teamOpt = Teams.getByName(teamId);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>Team not found.</red>");
      return;
    }
    Team team = teamOpt.get();

    if (team.isOpen()) {
      Teams.addMember(team, player, TeamRole.MEMBER);
    } else if (Teams.hasInvite(team, player)) {
      Teams.acceptInvite(team, player);
    } else {
      Players.msg(
          player, "<red>You do not have a pending invite to join " + team.getName() + ".</red>");
    }
  }

  private void handleLeave(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    Teams.removeMember(teamOpt.get(), player);
  }

  private void handleKick(Player player, String[] args) {
    if (args.length < 2) {
      Players.msg(player, "<red>Usage: /team kick <player></red>");
      return;
    }
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      Players.msg(player, "<red>Only captains and leaders can kick members.</red>");
      return;
    }

    Player target = Bukkit.getPlayer(args[1]);
    if (target != null) {
      Teams.kick(team, target, player, "Kicked by captain");
    } else {
      Players.msg(player, "<red>Player not found.</red>");
    }
  }

  private void handleSetHome(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      Players.msg(player, "<red>Only captains and leaders can set the team home.</red>");
      return;
    }
    Teams.setHome(team, player.getLocation());
    Teams.broadcast(team, "<green>Team home waypoint set by " + player.getName() + "!</green>");
  }

  private void handleHome(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    Teams.teleportHome(teamOpt.get(), player);
  }

  private void handleChat(Player player, String[] args) {
    if (args.length < 2) {
      Players.msg(player, "<red>Usage: /team chat <message></red>");
      return;
    }
    String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
    Teams.sendTeamChat(player, message);
  }

  private void handleDeposit(Player player, String[] args) {
    if (args.length < 2) {
      Players.msg(player, "<red>Usage: /team deposit <amount></red>");
      return;
    }
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    try {
      double amount = Double.parseDouble(args[1]);
      Teams.deposit(teamOpt.get(), player, amount);
    } catch (NumberFormatException e) {
      Players.msg(player, "<red>Invalid amount.</red>");
    }
  }

  private void handleWithdraw(Player player, String[] args) {
    if (args.length < 2) {
      Players.msg(player, "<red>Usage: /team withdraw <amount></red>");
      return;
    }
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      Players.msg(player, "<red>Only captains and leaders can withdraw team funds.</red>");
      return;
    }
    try {
      double amount = Double.parseDouble(args[1]);
      Teams.withdraw(team, player, amount);
    } catch (NumberFormatException e) {
      Players.msg(player, "<red>Invalid amount.</red>");
    }
  }

  private void handleToggleFriendlyFire(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      Players.msg(player, "<red>Only captains and leaders can toggle friendly fire.</red>");
      return;
    }
    boolean newState = !team.isFriendlyFireEnabled();
    team.setFriendlyFire(newState);
    Teams.save(team);
    Teams.broadcast(
        team,
        "<yellow>Friendly fire has been "
            + (newState ? "<green>ENABLED</green>" : "<red>DISABLED</red>")
            + " by "
            + player.getName()
            + ".</yellow>");
  }

  private void handleInfo(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      Players.msg(player, "<red>You are not in a team.</red>");
      return;
    }
    Team team = teamOpt.get();
    Players.msg(
        player,
        "<gold><bold>=== Team: " + team.getName() + " (" + team.getId() + ") ===</bold></gold>");
    Players.msg(
        player,
        "<yellow>Tag:</yellow> <white>"
            + (team.getTag().isEmpty() ? "None" : team.getTag())
            + "</white>");
    Players.msg(player, "<yellow>Members:</yellow> <white>" + team.getSize() + "</white>");
    Players.msg(
        player,
        "<yellow>Bank Balance:</yellow> <gold>$"
            + String.format("%.2f", team.getBalance())
            + "</gold>");
    Players.msg(
        player,
        "<yellow>Friendly Fire:</yellow> <white>"
            + (team.isFriendlyFireEnabled() ? "Allowed" : "Blocked")
            + "</white>");
    Players.msg(
        player,
        "<yellow>Home Set:</yellow> <white>"
            + (team.getHome() != null ? "Yes" : "No")
            + "</white>");
  }

  // Example Event Listener: Custom Friendly Fire handling
  @EventHandler
  public void onFriendlyDamage(TeamDamageTeammateEvent event) {
    // Optionally perform custom minigame or combat logic
    Bukkit.getLogger()
        .info(
            "[TeamExample] Prevented damage between teammates: "
                + event.getDamager().getName()
                + " -> "
                + event.getVictim().getName());
  }
}
