package dev.mukulx.javaskript.api.team.storage;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.team.Team;
import dev.mukulx.javaskript.api.team.TeamMember;
import dev.mukulx.javaskript.api.team.TeamRole;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/** High-performance SQLite persistence storage for teams, members, and metadata. */
public class TeamStorage {

  private final JavaSkriptPlugin plugin;
  private final File dbFile;
  private Connection connection;
  private final ExecutorService asyncExecutor;

  public TeamStorage(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    File dataDir = new File(plugin.getDataFolder(), "data");
    if (!dataDir.exists()) {
      dataDir.mkdirs();
    }
    this.dbFile = new File(dataDir, "teams.db");
    this.asyncExecutor =
        Executors.newSingleThreadExecutor(
            r -> {
              Thread thread = new Thread(r, "JavaSkript-TeamStorage");
              thread.setDaemon(true);
              return thread;
            });
    initDatabase();
  }

  private synchronized void initDatabase() {
    try {
      String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
      this.connection = DriverManager.getConnection(url);

      try (Statement stmt = connection.createStatement()) {
        // Enable WAL mode for high concurrency
        stmt.execute("PRAGMA journal_mode=WAL;");
        stmt.execute("PRAGMA synchronous=NORMAL;");
        stmt.execute("PRAGMA foreign_keys=ON;");

        // Teams table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS teams ("
                + "id VARCHAR(64) PRIMARY KEY, "
                + "name VARCHAR(128) NOT NULL, "
                + "tag VARCHAR(32) NOT NULL DEFAULT '', "
                + "prefix VARCHAR(64) NOT NULL DEFAULT '', "
                + "suffix VARCHAR(64) NOT NULL DEFAULT '', "
                + "color VARCHAR(32) NOT NULL DEFAULT 'white', "
                + "leader VARCHAR(36) NOT NULL, "
                + "friendly_fire INTEGER NOT NULL DEFAULT 0, "
                + "open INTEGER NOT NULL DEFAULT 0, "
                + "max_size INTEGER NOT NULL DEFAULT 0, "
                + "home_world VARCHAR(128), "
                + "home_x DOUBLE, "
                + "home_y DOUBLE, "
                + "home_z DOUBLE, "
                + "home_yaw REAL, "
                + "home_pitch REAL, "
                + "balance DOUBLE NOT NULL DEFAULT 0.0, "
                + "created_at BIGINT NOT NULL);");

        // Team members table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS team_members ("
                + "team_id VARCHAR(64) NOT NULL, "
                + "uuid VARCHAR(36) PRIMARY KEY, "
                + "role VARCHAR(32) NOT NULL, "
                + "join_timestamp BIGINT NOT NULL, "
                + "last_active_timestamp BIGINT NOT NULL, "
                + "FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE);");

        // Team metadata table
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS team_metadata ("
                + "team_id VARCHAR(64) NOT NULL, "
                + "meta_key VARCHAR(128) NOT NULL, "
                + "meta_value TEXT NOT NULL, "
                + "PRIMARY KEY (team_id, meta_key), "
                + "FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE);");
      }
      plugin.debug("Team SQLite storage initialized at: " + dbFile.getName());
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to initialize Team SQLite database", e);
    }
  }

  private synchronized Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
      initDatabase();
    }
    return connection;
  }

  /**
   * Loads all teams, members, and metadata from SQLite into memory.
   *
   * @return Map of teamId to Team instance
   */
  public Map<String, Team> loadAll() {
    Map<String, Team> teams = new ConcurrentHashMap<>();

    try {
      Connection conn = getConnection();

      // Load teams
      String selectTeams = "SELECT * FROM teams";
      try (PreparedStatement stmt = conn.prepareStatement(selectTeams);
          ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          String id = rs.getString("id");
          String name = rs.getString("name");
          String tag = rs.getString("tag");
          String prefix = rs.getString("prefix");
          String suffix = rs.getString("suffix");
          String colorStr = rs.getString("color");
          UUID leader = UUID.fromString(rs.getString("leader"));
          boolean friendlyFire = rs.getInt("friendly_fire") == 1;
          boolean open = rs.getInt("open") == 1;
          int maxSize = rs.getInt("max_size");
          double balance = rs.getDouble("balance");
          long createdAt = rs.getLong("created_at");

          NamedTextColor color = NamedTextColor.WHITE;
          if (colorStr != null && !colorStr.isBlank()) {
            NamedTextColor parsed = NamedTextColor.NAMES.value(colorStr.toLowerCase());
            if (parsed != null) {
              color = parsed;
            }
          }

          Location home = null;
          String worldName = rs.getString("home_world");
          if (worldName != null && !worldName.isBlank()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
              home =
                  new Location(
                      world,
                      rs.getDouble("home_x"),
                      rs.getDouble("home_y"),
                      rs.getDouble("home_z"),
                      rs.getFloat("home_yaw"),
                      rs.getFloat("home_pitch"));
            }
          }

          Team team =
              new Team(
                  id,
                  name,
                  tag,
                  prefix,
                  suffix,
                  color,
                  leader,
                  friendlyFire,
                  open,
                  maxSize,
                  home,
                  balance,
                  createdAt);
          teams.put(id, team);
        }
      }

      // Load members
      String selectMembers = "SELECT * FROM team_members";
      try (PreparedStatement stmt = conn.prepareStatement(selectMembers);
          ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          String teamId = rs.getString("team_id");
          Team team = teams.get(teamId);
          if (team != null) {
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            TeamRole role = TeamRole.fromString(rs.getString("role"), TeamRole.MEMBER);
            long joinTimestamp = rs.getLong("join_timestamp");
            long lastActive = rs.getLong("last_active_timestamp");

            TeamMember member = new TeamMember(uuid, role, joinTimestamp, lastActive);
            team.addMember(member);
          }
        }
      }

      // Load metadata
      String selectMeta = "SELECT * FROM team_metadata";
      try (PreparedStatement stmt = conn.prepareStatement(selectMeta);
          ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          String teamId = rs.getString("team_id");
          Team team = teams.get(teamId);
          if (team != null) {
            String key = rs.getString("meta_key");
            String value = rs.getString("meta_value");
            team.setMetadata(key, value);
          }
        }
      }

      plugin.debug("Loaded " + teams.size() + " teams from SQLite database.");
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to load teams from SQLite", e);
    }

    return teams;
  }

  /**
   * Saves a team synchronously to SQLite.
   *
   * @param team The team to save
   */
  public synchronized void saveTeamSync(Team team) {
    if (team == null) return;
    try {
      Connection conn = getConnection();
      String query =
          "INSERT INTO teams (id, name, tag, prefix, suffix, color, leader, friendly_fire, open, max_size, "
              + "home_world, home_x, home_y, home_z, home_yaw, home_pitch, balance, created_at) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
              + "ON CONFLICT(id) DO UPDATE SET "
              + "name = excluded.name, "
              + "tag = excluded.tag, "
              + "prefix = excluded.prefix, "
              + "suffix = excluded.suffix, "
              + "color = excluded.color, "
              + "leader = excluded.leader, "
              + "friendly_fire = excluded.friendly_fire, "
              + "open = excluded.open, "
              + "max_size = excluded.max_size, "
              + "home_world = excluded.home_world, "
              + "home_x = excluded.home_x, "
              + "home_y = excluded.home_y, "
              + "home_z = excluded.home_z, "
              + "home_yaw = excluded.home_yaw, "
              + "home_pitch = excluded.home_pitch, "
              + "balance = excluded.balance;";

      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, team.getId());
        stmt.setString(2, team.getName());
        stmt.setString(3, team.getTag());
        stmt.setString(4, team.getPrefix());
        stmt.setString(5, team.getSuffix());
        stmt.setString(
            6, team.getColor() != null ? NamedTextColor.NAMES.key(team.getColor()) : "white");
        stmt.setString(7, team.getLeader().toString());
        stmt.setInt(8, team.isFriendlyFireEnabled() ? 1 : 0);
        stmt.setInt(9, team.isOpen() ? 1 : 0);
        stmt.setInt(10, team.getMaxSize());

        Location home = team.getHome();
        if (home != null && home.getWorld() != null) {
          stmt.setString(11, home.getWorld().getName());
          stmt.setDouble(12, home.getX());
          stmt.setDouble(13, home.getY());
          stmt.setDouble(14, home.getZ());
          stmt.setFloat(15, home.getYaw());
          stmt.setFloat(16, home.getPitch());
        } else {
          stmt.setNull(11, java.sql.Types.VARCHAR);
          stmt.setNull(12, java.sql.Types.DOUBLE);
          stmt.setNull(13, java.sql.Types.DOUBLE);
          stmt.setNull(14, java.sql.Types.DOUBLE);
          stmt.setNull(15, java.sql.Types.FLOAT);
          stmt.setNull(16, java.sql.Types.FLOAT);
        }

        stmt.setDouble(17, team.getBalance());
        stmt.setLong(18, team.getCreatedAt());

        stmt.executeUpdate();
      }

      // Save members
      for (TeamMember member : team.getMembers().values()) {
        saveMemberSync(team.getId(), member);
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to save team: " + team.getId(), e);
    }
  }

  /**
   * Saves a team asynchronously.
   *
   * @param team The team to save
   */
  public void saveTeamAsync(Team team) {
    if (team == null) return;
    asyncExecutor.execute(() -> saveTeamSync(team));
  }

  /**
   * Saves or updates a team member synchronously.
   *
   * @param teamId The team ID
   * @param member The team member
   */
  public synchronized void saveMemberSync(String teamId, TeamMember member) {
    if (teamId == null || member == null) return;
    try {
      Connection conn = getConnection();
      String query =
          "INSERT INTO team_members (team_id, uuid, role, join_timestamp, last_active_timestamp) "
              + "VALUES (?, ?, ?, ?, ?) "
              + "ON CONFLICT(uuid) DO UPDATE SET "
              + "team_id = excluded.team_id, "
              + "role = excluded.role, "
              + "last_active_timestamp = excluded.last_active_timestamp;";

      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, teamId.toLowerCase());
        stmt.setString(2, member.getUniqueId().toString());
        stmt.setString(3, member.getRole().name());
        stmt.setLong(4, member.getJoinTimestamp());
        stmt.setLong(5, member.getLastActiveTimestamp());
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to save member: " + member.getUniqueId(), e);
    }
  }

  /**
   * Saves or updates a team member asynchronously.
   *
   * @param teamId The team ID
   * @param member The team member
   */
  public void saveMemberAsync(String teamId, TeamMember member) {
    if (teamId == null || member == null) return;
    asyncExecutor.execute(() -> saveMemberSync(teamId, member));
  }

  /**
   * Removes a team member synchronously.
   *
   * @param uuid The player UUID
   */
  public synchronized void removeMemberSync(UUID uuid) {
    if (uuid == null) return;
    try {
      Connection conn = getConnection();
      String query = "DELETE FROM team_members WHERE uuid = ?";
      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, uuid.toString());
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to delete member: " + uuid, e);
    }
  }

  /**
   * Removes a team member asynchronously.
   *
   * @param uuid The player UUID
   */
  public void removeMemberAsync(UUID uuid) {
    if (uuid == null) return;
    asyncExecutor.execute(() -> removeMemberSync(uuid));
  }

  /**
   * Deletes a team and cascades to all its members and metadata synchronously.
   *
   * @param teamId The team ID
   */
  public synchronized void deleteTeamSync(String teamId) {
    if (teamId == null) return;
    String id = teamId.toLowerCase();
    try {
      Connection conn = getConnection();
      try (PreparedStatement stmt =
          conn.prepareStatement("DELETE FROM team_members WHERE team_id = ?")) {
        stmt.setString(1, id);
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt =
          conn.prepareStatement("DELETE FROM team_metadata WHERE team_id = ?")) {
        stmt.setString(1, id);
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM teams WHERE id = ?")) {
        stmt.setString(1, id);
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to delete team: " + id, e);
    }
  }

  /**
   * Deletes a team asynchronously.
   *
   * @param teamId The team ID
   */
  public void deleteTeamAsync(String teamId) {
    if (teamId == null) return;
    asyncExecutor.execute(() -> deleteTeamSync(teamId));
  }

  /**
   * Saves a metadata key-value pair asynchronously.
   *
   * @param teamId The team ID
   * @param key The metadata key
   * @param value The metadata value
   */
  public void saveMetadataAsync(String teamId, String key, Object value) {
    if (teamId == null || key == null) return;
    asyncExecutor.execute(
        () -> {
          try {
            Connection conn = getConnection();
            if (value == null) {
              try (PreparedStatement stmt =
                  conn.prepareStatement(
                      "DELETE FROM team_metadata WHERE team_id = ? AND meta_key = ?")) {
                stmt.setString(1, teamId.toLowerCase());
                stmt.setString(2, key);
                stmt.executeUpdate();
              }
            } else {
              String query =
                  "INSERT INTO team_metadata (team_id, meta_key, meta_value) VALUES (?, ?, ?) "
                      + "ON CONFLICT(team_id, meta_key) DO UPDATE SET meta_value = excluded.meta_value;";
              try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, teamId.toLowerCase());
                stmt.setString(2, key);
                stmt.setString(3, value.toString());
                stmt.executeUpdate();
              }
            }
          } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save metadata for team: " + teamId, e);
          }
        });
  }

  /** Closes database connection and shuts down background executor service. */
  public synchronized void close() {
    try {
      asyncExecutor.shutdown();
      if (!asyncExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
        asyncExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      asyncExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "Error closing Team SQLite connection", e);
    }
  }
}
