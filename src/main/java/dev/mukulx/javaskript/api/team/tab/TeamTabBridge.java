package dev.mukulx.javaskript.api.team.tab;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.PlaceholderHelper;
import dev.mukulx.javaskript.api.team.Team;
import dev.mukulx.javaskript.api.team.TeamRole;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Handles Tab list and Nametag visual integration for teams.
 *
 * <p>Supports vanilla Minecraft scoreboards out of the box and seamlessly bridges with the TAB
 * plugin (by NEZNAMY) and scoreboard plugins via PlaceholderAPI.
 */
public class TeamTabBridge {

  private static final String SCOREBOARD_PREFIX = "jst_";

  private final JavaSkriptPlugin plugin;
  private final Function<UUID, Optional<Team>> teamLookup;
  private final PlaceholderHelper placeholderHelper;
  private final boolean tabPluginPresent;

  public TeamTabBridge(JavaSkriptPlugin plugin, Function<UUID, Optional<Team>> teamLookup) {
    this.plugin = plugin;
    this.teamLookup = teamLookup;
    this.tabPluginPresent = Bukkit.getPluginManager().isPluginEnabled("TAB");
    this.placeholderHelper = new PlaceholderHelper(plugin, "javaskript");

    registerPlaceholders();
  }

  /** Registers rich team placeholders for PlaceholderAPI, TAB, and custom scoreboard plugins. */
  private void registerPlaceholders() {
    // PlaceholderAPI is an optional dependency. Skip quietly instead of warning once per
    // placeholder on servers that don't have it.
    if (!placeholderHelper.isPlaceholderAPIAvailable()) {
      plugin.debug("PlaceholderAPI not found, skipping built-in team placeholders");
      return;
    }

    // %javaskript_team% -> Team ID or empty
    placeholderHelper.registerPlaceholder(
        "team",
        (player, params) -> {
          if (player == null) return "";
          return teamLookup.apply(player.getUniqueId()).map(Team::getId).orElse("");
        });

    // %javaskript_team_name% -> Team display name or empty
    placeholderHelper.registerPlaceholder(
        "team_name",
        (player, params) -> {
          if (player == null) return "";
          return teamLookup.apply(player.getUniqueId()).map(Team::getName).orElse("");
        });

    // %javaskript_team_tag% -> Team tag or empty
    placeholderHelper.registerPlaceholder(
        "team_tag",
        (player, params) -> {
          if (player == null) return "";
          return teamLookup.apply(player.getUniqueId()).map(Team::getTag).orElse("");
        });

    // %javaskript_team_prefix% -> Configured prefix (empty string by default)
    placeholderHelper.registerPlaceholder(
        "team_prefix",
        (player, params) -> {
          if (player == null) return "";
          return teamLookup.apply(player.getUniqueId()).map(Team::getPrefix).orElse("");
        });

    // %javaskript_team_suffix% -> Configured suffix (empty string by default)
    placeholderHelper.registerPlaceholder(
        "team_suffix",
        (player, params) -> {
          if (player == null) return "";
          return teamLookup.apply(player.getUniqueId()).map(Team::getSuffix).orElse("");
        });

    // %javaskript_team_role% -> Role name (Leader, Captain, Member, or empty)
    placeholderHelper.registerPlaceholder(
        "team_role",
        (player, params) -> {
          if (player == null) return "";
          return teamLookup
              .apply(player.getUniqueId())
              .flatMap(t -> t.getRole(player.getUniqueId()))
              .map(TeamRole::getDisplayName)
              .orElse("");
        });

    // %javaskript_team_color% -> Color name or empty
    placeholderHelper.registerPlaceholder(
        "team_color",
        (player, params) -> {
          if (player == null) return "";
          return teamLookup
              .apply(player.getUniqueId())
              .map(t -> NamedTextColor.NAMES.key(t.getColor()))
              .orElse("");
        });

    // %javaskript_team_size% -> Total team size
    placeholderHelper.registerPlaceholder(
        "team_size",
        (player, params) -> {
          if (player == null) return "0";
          return teamLookup
              .apply(player.getUniqueId())
              .map(t -> String.valueOf(t.getSize()))
              .orElse("0");
        });

    // %javaskript_team_max_size% -> Max size or unlimited
    placeholderHelper.registerPlaceholder(
        "team_max_size",
        (player, params) -> {
          if (player == null) return "0";
          return teamLookup
              .apply(player.getUniqueId())
              .map(t -> t.getMaxSize() <= 0 ? "unlimited" : String.valueOf(t.getMaxSize()))
              .orElse("0");
        });

    // %javaskript_team_balance% -> Team bank balance
    placeholderHelper.registerPlaceholder(
        "team_balance",
        (player, params) -> {
          if (player == null) return "0.0";
          return teamLookup
              .apply(player.getUniqueId())
              .map(t -> String.format("%.2f", t.getBalance()))
              .orElse("0.0");
        });

    // %javaskript_team_leader% -> Team leader name
    placeholderHelper.registerPlaceholder(
        "team_leader",
        (player, params) -> {
          if (player == null) return "";
          return teamLookup
              .apply(player.getUniqueId())
              .map(
                  t -> {
                    OfflinePlayer leader = Bukkit.getOfflinePlayer(t.getLeader());
                    return leader.getName() != null ? leader.getName() : t.getLeader().toString();
                  })
              .orElse("");
        });

    // %javaskript_is_team_leader% -> "true" or "false"
    placeholderHelper.registerPlaceholder(
        "is_team_leader",
        (player, params) -> {
          if (player == null) return "false";
          return teamLookup
              .apply(player.getUniqueId())
              .map(t -> String.valueOf(t.isLeader(player.getUniqueId())))
              .orElse("false");
        });

    // %javaskript_has_team% -> "true" or "false"
    placeholderHelper.registerPlaceholder(
        "has_team",
        (player, params) -> {
          if (player == null) return "false";
          return teamLookup.apply(player.getUniqueId()).isPresent() ? "true" : "false";
        });
  }

  /**
   * Updates a player's tab list display name and nametag scoreboard.
   *
   * @param player The player to update
   */
  public void updatePlayer(Player player) {
    if (player == null || !player.isOnline()) {
      return;
    }

    boolean tabFormattingEnabled = plugin.getConfig().getBoolean("modules.teams.tab.enabled", true);
    // auto: with the TAB plugin present, leave scoreboards to it
    boolean syncScoreboard =
        switch (plugin
            .getConfig()
            .getString("modules.teams.tab.vanilla-scoreboard", "auto")
            .toLowerCase()) {
          case "never" -> false;
          case "always" -> true;
          default -> !tabPluginPresent;
        };

    Optional<Team> teamOpt = teamLookup.apply(player.getUniqueId());

    if (teamOpt.isEmpty()) {
      // Remove from scoreboard team
      if (syncScoreboard) {
        removeFromScoreboard(player);
      }
      // Reset tab list name
      if (tabFormattingEnabled) {
        player.playerListName(null);
      }
      return;
    }

    Team team = teamOpt.get();
    String prefix = team.getPrefix();
    String suffix = team.getSuffix();

    // 1. Vanilla Scoreboard Nametag Sync
    if (syncScoreboard) {
      updateScoreboardTeam(player, team);
    } else {
      removeFromScoreboard(player);
    }

    // 2. Tab List Formatting (empty by default; only applied when prefix/suffix is configured)
    if (tabFormattingEnabled) {
      if ((prefix == null || prefix.isEmpty()) && (suffix == null || suffix.isEmpty())) {
        // Default: No prefix or suffix configured, keep standard vanilla player name in tab
        player.playerListName(null);
      } else {
        String format =
            plugin
                .getConfig()
                .getString("modules.teams.tab.player-format", "{prefix}{player}{suffix}");
        String formatted =
            format
                .replace("{prefix}", prefix != null ? prefix : "")
                .replace("{suffix}", suffix != null ? suffix : "")
                .replace("{tag}", team.getTag())
                .replace("{team}", team.getName())
                .replace("{player}", player.getName())
                .replace(
                    "{role}",
                    team.getRole(player.getUniqueId()).map(TeamRole::getDisplayName).orElse(""));

        Component tabComponent = plugin.getMessageManager().parse(formatted);
        player.playerListName(tabComponent);
      }
    } else {
      player.playerListName(null);
    }
  }

  /**
   * Synchronizes Paper/Bukkit Scoreboard Team for nametag prefix/suffix.
   *
   * @param player The player
   * @param team The team
   */
  private void updateScoreboardTeam(Player player, Team team) {
    try {
      Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
      String sbName = getScoreboardTeamName(team.getId());
      org.bukkit.scoreboard.Team sbTeam = scoreboard.getTeam(sbName);

      if (sbTeam == null) {
        sbTeam = scoreboard.registerNewTeam(sbName);
      }

      // Configure prefix and suffix (empty by default)
      if (team.getPrefix() != null && !team.getPrefix().isEmpty()) {
        sbTeam.prefix(plugin.getMessageManager().parse(team.getPrefix()));
      } else {
        sbTeam.prefix(Component.empty());
      }

      if (team.getSuffix() != null && !team.getSuffix().isEmpty()) {
        sbTeam.suffix(plugin.getMessageManager().parse(team.getSuffix()));
      } else {
        sbTeam.suffix(Component.empty());
      }

      if (team.getColor() != null) {
        sbTeam.color(team.getColor());
      }

      if (!sbTeam.hasEntry(player.getName())) {
        sbTeam.addEntry(player.getName());
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.FINE, "Could not update scoreboard team for " + player.getName(), e);
    }
  }

  /**
   * Removes a player from any team scoreboard entry.
   *
   * @param player The player
   */
  public void removeFromScoreboard(Player player) {
    if (player == null) return;
    try {
      Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
      for (org.bukkit.scoreboard.Team sbTeam : scoreboard.getTeams()) {
        if (sbTeam.getName().startsWith(SCOREBOARD_PREFIX) && sbTeam.hasEntry(player.getName())) {
          sbTeam.removeEntry(player.getName());
        }
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.FINE, "Could not remove player from scoreboard team", e);
    }
  }

  /**
   * Removes an entire team from the Scoreboard.
   *
   * @param teamId The team ID
   */
  public void removeScoreboardTeam(String teamId) {
    if (teamId == null) return;
    try {
      Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
      String sbName = getScoreboardTeamName(teamId);
      org.bukkit.scoreboard.Team sbTeam = scoreboard.getTeam(sbName);
      if (sbTeam != null) {
        sbTeam.unregister();
      }
    } catch (Exception e) {
      plugin.getLogger().log(Level.FINE, "Could not unregister scoreboard team", e);
    }
  }

  /**
   * Updates all online members of a team.
   *
   * @param team The team
   */
  public void updateTeam(Team team) {
    if (team == null) return;
    for (Player player : team.getOnlinePlayers()) {
      updatePlayer(player);
    }
  }

  /** Shuts down bridge and cleans up placeholders and scoreboard teams. */
  public void shutdown() {
    placeholderHelper.unregisterAll();
    for (Player player : Bukkit.getOnlinePlayers()) {
      try {
        player.playerListName(null);
      } catch (Exception ignored) {
      }
    }
    try {
      Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
      for (org.bukkit.scoreboard.Team sbTeam : scoreboard.getTeams()) {
        if (sbTeam.getName().startsWith(SCOREBOARD_PREFIX)) {
          sbTeam.unregister();
        }
      }
    } catch (Exception ignored) {
    }
  }

  private String getScoreboardTeamName(String teamId) {
    String cleanId = teamId.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
    String fullName = SCOREBOARD_PREFIX + cleanId;
    return fullName.length() > 16 ? fullName.substring(0, 16) : fullName;
  }
}
