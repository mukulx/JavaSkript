package dev.mukulx.javaskript.command;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.message.MessageManager;
import dev.mukulx.javaskript.api.team.Team;
import dev.mukulx.javaskript.api.team.TeamRole;
import dev.mukulx.javaskript.api.team.Teams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** Built-in command handler for /team (aliases: /clan, /party, /t). */
public class TeamCommand implements CommandExecutor, TabCompleter {

  private final JavaSkriptPlugin plugin;
  private final MessageManager messages;

  private static final List<String> SUBCOMMANDS =
      List.of(
          "create",
          "invite",
          "join",
          "leave",
          "kick",
          "disband",
          "transfer",
          "sethome",
          "home",
          "chat",
          "deposit",
          "withdraw",
          "ff",
          "info",
          "help");

  public TeamCommand(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.messages = plugin.getMessageManager();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players can execute team commands."));
      return true;
    }

    if (!Teams.isEnabled()) {
      player.sendMessage(messages.parse("<red>The Team subsystem is currently disabled.</red>"));
      return true;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      sendHelp(player, label);
      return true;
    }

    String sub = args[0].toLowerCase();
    switch (sub) {
      case "create" -> handleCreate(player, args);
      case "invite" -> handleInvite(player, args);
      case "join" -> handleJoin(player, args);
      case "leave" -> handleLeave(player);
      case "kick" -> handleKick(player, args);
      case "disband" -> handleDisband(player);
      case "transfer" -> handleTransfer(player, args);
      case "sethome" -> handleSetHome(player);
      case "home" -> handleHome(player);
      case "chat", "c" -> handleChat(player, args);
      case "deposit", "dep" -> handleDeposit(player, args);
      case "withdraw", "with" -> handleWithdraw(player, args);
      case "ff", "friendlyfire" -> handleToggleFriendlyFire(player);
      case "info" -> handleInfo(player, args);
      default -> sendHelp(player, label);
    }

    return true;
  }

  private void sendHelp(Player player, String label) {
    player.sendMessage(messages.parse("<color:#FF8C00><bold>=== Team Commands ===</bold></color>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " create <name></color> <gray>- Create a new team</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " invite <player></color> <gray>- Invite player to team (Captain+)</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " join <team></color> <gray>- Join an open or invited team</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/" + label + " leave</color> <gray>- Leave your current team</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " kick <player></color> <gray>- Kick a member (Captain+)</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " disband</color> <gray>- Disband the team (Leader only)</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " transfer <player></color> <gray>- Transfer leadership (Leader only)</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " sethome</color> <gray>- Set team home waypoint (Captain+)</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/" + label + " home</color> <gray>- Teleport to team home</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " chat <msg></color> <gray>- Send message to teammates</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " deposit <amount></color> <gray>- Deposit funds into team bank</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " withdraw <amount></color> <gray>- Withdraw funds from team bank (Captain+)</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " ff</color> <gray>- Toggle friendly fire (Captain+)</gray>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>/"
                + label
                + " info [team]</color> <gray>- View team information and roster</gray>"));
  }

  private void handleCreate(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(messages.parse("<red>Usage: /team create <name></red>"));
      return;
    }

    if (Teams.isInTeam(player)) {
      messages.send(
          player, "teams.already-in-team", "<red>You are already in a team! Leave first.</red>");
      return;
    }

    String name;
    String id;
    if (args.length == 2) {
      name = args[1];
      id = name.toLowerCase().replaceAll("[^a-z0-9_]", "");
    } else {
      id = args[1].toLowerCase().replaceAll("[^a-z0-9_]", "");
      name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
    }

    if (id.isEmpty()) {
      player.sendMessage(
          messages.parse("<red>Team name must contain alphanumeric characters.</red>"));
      return;
    }

    if (Teams.exists(id)) {
      messages.send(
          player,
          "teams.team-already-exists",
          "<red>A team named <color:#FF8C00>{team}</color> already exists!</red>",
          "{team}",
          id);
      return;
    }

    Team team = Teams.create(id, name, player);
    if (team != null) {
      messages.send(
          player,
          "teams.created",
          "<green>Team <color:#FF8C00>{team}</color> has been created!</green>",
          "{team}",
          team.getName());
    } else {
      player.sendMessage(messages.parse("<red>Failed to create team.</red>"));
    }
  }

  private void handleInvite(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(messages.parse("<red>Usage: /team invite <player></red>"));
      return;
    }

    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      messages.send(
          player,
          "teams.no-permission",
          "<red>Only captains and leaders can invite players.</red>");
      return;
    }

    Player target = Bukkit.getPlayer(args[1]);
    if (target == null || !target.isOnline()) {
      player.sendMessage(messages.parse("<red>Player not found or offline.</red>"));
      return;
    }

    Teams.invite(team, target, player);
  }

  private void handleJoin(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(messages.parse("<red>Usage: /team join <team></red>"));
      return;
    }

    if (Teams.isInTeam(player)) {
      messages.send(player, "teams.already-in-team", "<red>You are already in a team!</red>");
      return;
    }

    String teamId = args[1].toLowerCase();
    Optional<Team> teamOpt = Teams.getByName(teamId);
    if (teamOpt.isEmpty()) {
      messages.send(
          player,
          "teams.team-not-found",
          "<red>Team <color:#FF8C00>{team}</color> was not found.</red>",
          "{team}",
          teamId);
      return;
    }

    Team team = teamOpt.get();
    if (team.isFull()) {
      messages.send(
          player,
          "teams.team-full",
          "<red>Team <color:#FF8C00>{team}</color> is full.</red>",
          "{team}",
          team.getName());
      return;
    }

    if (Teams.hasInvite(team, player)) {
      Teams.acceptInvite(team, player);
    } else if (team.isOpen()) {
      Teams.join(team, player);
    } else {
      player.sendMessage(
          messages.parse("<red>This team is invite-only. You must be invited first.</red>"));
    }
  }

  private void handleLeave(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    if (team.isLeader(player.getUniqueId())) {
      if (team.getSize() > 1) {
        player.sendMessage(
            messages.parse(
                "<red>As leader, you must transfer leadership (/team transfer <player>) before leaving, or disband (/team disband).</red>"));
        return;
      }
      // Sole member leaving disbands the team
      Teams.disband(team);
      return;
    }

    Teams.leave(team, player);
  }

  private void handleKick(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(messages.parse("<red>Usage: /team kick <player></red>"));
      return;
    }

    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    TeamRole kickerRole = team.getRole(player.getUniqueId()).orElse(TeamRole.MEMBER);
    if (!kickerRole.canInvite()) { // Captain or Leader
      messages.send(
          player, "teams.no-permission", "<red>Only captains and leaders can kick members.</red>");
      return;
    }

    Player target = Bukkit.getPlayer(args[1]);
    UUID targetUuid = null;
    String targetName = args[1];

    if (target != null) {
      targetUuid = target.getUniqueId();
      targetName = target.getName();
    } else {
      // Check offline members
      for (UUID uuid : team.getMemberUuids()) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (args[1].equalsIgnoreCase(op.getName())) {
          targetUuid = uuid;
          targetName = op.getName();
          break;
        }
      }
    }

    if (targetUuid == null || !team.hasMember(targetUuid)) {
      player.sendMessage(messages.parse("<red>Player is not a member of your team.</red>"));
      return;
    }

    if (targetUuid.equals(player.getUniqueId())) {
      player.sendMessage(
          messages.parse("<red>You cannot kick yourself! Use /team leave instead.</red>"));
      return;
    }

    TeamRole targetRole = team.getRole(targetUuid).orElse(TeamRole.MEMBER);
    if (!kickerRole.canKick(targetRole)) {
      player.sendMessage(
          messages.parse("<red>You cannot kick a member with an equal or higher role!</red>"));
      return;
    }

    Teams.kick(team, targetUuid, player.getUniqueId(), "Kicked by " + player.getName());
  }

  private void handleDisband(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    if (!team.isLeader(player.getUniqueId())) {
      messages.send(
          player, "teams.no-permission", "<red>Only the team leader can disband the team.</red>");
      return;
    }

    Teams.disband(team);
  }

  private void handleTransfer(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(messages.parse("<red>Usage: /team transfer <player></red>"));
      return;
    }

    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    if (!team.isLeader(player.getUniqueId())) {
      messages.send(
          player,
          "teams.no-permission",
          "<red>Only the team leader can transfer leadership.</red>");
      return;
    }

    Player target = Bukkit.getPlayer(args[1]);
    if (target == null || !team.hasMember(target)) {
      player.sendMessage(
          messages.parse("<red>Target player must be an online member of your team.</red>"));
      return;
    }

    if (target.getUniqueId().equals(player.getUniqueId())) {
      player.sendMessage(messages.parse("<red>You are already the leader.</red>"));
      return;
    }

    Teams.transferLeadership(team, target.getUniqueId());
  }

  private void handleSetHome(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      messages.send(
          player,
          "teams.no-permission",
          "<red>Only captains and leaders can set the team home.</red>");
      return;
    }

    Teams.setHome(team, player.getLocation());
    messages.send(player, "teams.home-set", "<green>Team home has been successfully set!</green>");
  }

  private void handleHome(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    if (team.getHome() == null) {
      messages.send(player, "teams.home-not-set", "<red>Your team does not have a home set.</red>");
      return;
    }

    messages.send(player, "teams.home-teleport", "<green>Teleporting to team home...</green>");
    Teams.teleportHome(team, player);
  }

  private void handleChat(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(messages.parse("<red>Usage: /team chat <message></red>"));
      return;
    }

    if (!Teams.isInTeam(player)) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
    Teams.sendTeamChat(player, message);
  }

  private void handleDeposit(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(messages.parse("<red>Usage: /team deposit <amount></red>"));
      return;
    }

    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    try {
      double amount = Double.parseDouble(args[1]);
      if (amount <= 0) {
        player.sendMessage(messages.parse("<red>Amount must be positive.</red>"));
        return;
      }
      Teams.deposit(teamOpt.get(), player, amount);
    } catch (NumberFormatException e) {
      player.sendMessage(messages.parse("<red>Invalid numeric amount.</red>"));
    }
  }

  private void handleWithdraw(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(messages.parse("<red>Usage: /team withdraw <amount></red>"));
      return;
    }

    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      messages.send(
          player,
          "teams.no-permission",
          "<red>Only captains and leaders can withdraw team funds.</red>");
      return;
    }

    try {
      double amount = Double.parseDouble(args[1]);
      if (amount <= 0) {
        player.sendMessage(messages.parse("<red>Amount must be positive.</red>"));
        return;
      }
      Teams.withdraw(team, player, amount);
    } catch (NumberFormatException e) {
      player.sendMessage(messages.parse("<red>Invalid numeric amount.</red>"));
    }
  }

  private void handleToggleFriendlyFire(Player player) {
    Optional<Team> teamOpt = Teams.get(player);
    if (teamOpt.isEmpty()) {
      messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
      return;
    }

    Team team = teamOpt.get();
    if (!team.isCaptain(player.getUniqueId())) {
      messages.send(
          player,
          "teams.no-permission",
          "<red>Only captains and leaders can toggle friendly fire.</red>");
      return;
    }

    boolean newState = !team.isFriendlyFireEnabled();
    Teams.setFriendlyFire(team, newState);
    String stateMsg =
        newState
            ? "<green>Friendly fire is now <bold>ENABLED</bold>.</green>"
            : "<red>Friendly fire is now <bold>DISABLED</bold>.</red>";
    player.sendMessage(messages.parse(stateMsg));
  }

  private void handleInfo(Player player, String[] args) {
    Team team;
    if (args.length >= 2) {
      Optional<Team> targetOpt = Teams.getByName(args[1]);
      if (targetOpt.isEmpty()) {
        messages.send(
            player,
            "teams.team-not-found",
            "<red>Team <color:#FF8C00>{team}</color> was not found.</red>",
            "{team}",
            args[1]);
        return;
      }
      team = targetOpt.get();
    } else {
      Optional<Team> ownOpt = Teams.get(player);
      if (ownOpt.isEmpty()) {
        messages.send(player, "teams.not-in-team", "<red>You are not in a team!</red>");
        return;
      }
      team = ownOpt.get();
    }

    OfflinePlayer leaderPlayer = Bukkit.getOfflinePlayer(team.getLeader());
    String leaderName = leaderPlayer.getName() != null ? leaderPlayer.getName() : "Unknown";
    String safeTeamName = MessageManager.escapeMiniMessage(team.getName());
    String safeTeamId = MessageManager.escapeMiniMessage(team.getId());
    String safeLeader = MessageManager.escapeMiniMessage(leaderName);

    player.sendMessage(
        messages.parse("<color:#FF8C00><bold>=== Team: " + safeTeamName + " ===</bold></color>"));
    player.sendMessage(
        messages.parse("<color:#FFA726>ID:</color> <white>" + safeTeamId + "</white>"));
    player.sendMessage(
        messages.parse("<color:#FFA726>Leader:</color> <white>" + safeLeader + "</white>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>Members:</color> <white>"
                + team.getSize()
                + (team.getMaxSize() > 0 ? " / " + team.getMaxSize() : "")
                + "</white>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>Bank Balance:</color> <green>$"
                + String.format("%.2f", team.getBalance())
                + "</green>"));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>Friendly Fire:</color> "
                + (team.isFriendlyFireEnabled()
                    ? "<green>Enabled</green>"
                    : "<red>Disabled</red>")));
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>Open Join:</color> "
                + (team.isOpen() ? "<green>Yes</green>" : "<red>No</red>")));

    // Online players (escape names so they cannot inject formatting)
    List<String> onlineNames =
        team.getOnlinePlayers().stream()
            .map(p -> MessageManager.escapeMiniMessage(p.getName()))
            .toList();
    player.sendMessage(
        messages.parse(
            "<color:#FFA726>Online ("
                + onlineNames.size()
                + "):</color> <white>"
                + (onlineNames.isEmpty() ? "None" : String.join(", ", onlineNames))
                + "</white>"));
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      return Collections.emptyList();
    }

    if (args.length == 1) {
      String prefix = args[0].toLowerCase();
      return SUBCOMMANDS.stream()
          .filter(sub -> sub.startsWith(prefix))
          .collect(Collectors.toList());
    }

    if (args.length == 2) {
      String sub = args[0].toLowerCase();
      String prefix = args[1].toLowerCase();

      switch (sub) {
        case "invite" -> {
          return Bukkit.getOnlinePlayers().stream()
              .map(Player::getName)
              .filter(name -> name.toLowerCase().startsWith(prefix))
              .filter(name -> !name.equalsIgnoreCase(player.getName()))
              .collect(Collectors.toList());
        }
        case "join", "info" -> {
          return Teams.getAll().stream()
              .map(Team::getId)
              .filter(id -> id.toLowerCase().startsWith(prefix))
              .collect(Collectors.toList());
        }
        case "kick", "transfer" -> {
          Optional<Team> teamOpt = Teams.get(player);
          if (teamOpt.isEmpty()) return Collections.emptyList();
          List<String> members = new ArrayList<>();
          for (UUID uuid : teamOpt.get().getMemberUuids()) {
            if (!uuid.equals(player.getUniqueId())) {
              OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
              if (op.getName() != null && op.getName().toLowerCase().startsWith(prefix)) {
                members.add(op.getName());
              }
            }
          }
          return members;
        }
        case "deposit", "withdraw" -> {
          return List.of("10", "50", "100", "500", "1000").stream()
              .filter(s -> s.startsWith(prefix))
              .collect(Collectors.toList());
        }
      }
    }

    return Collections.emptyList();
  }
}
