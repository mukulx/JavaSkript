package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** Easy database access for scripts Supports SQLite out of the box */
public class DatabaseHelper {

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private Connection connection;
  private final File dbFile;

  public DatabaseHelper(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName = scriptName.replace(".java", "");

    File dataFolder = new File(plugin.getDataFolder(), "script-data/" + this.scriptName);
    this.dbFile = new File(dataFolder, "database.db");

    // Don't create folder until connect() is called
  }

  /**
   * Connect to the SQLite database
   *
   * @return true if successful
   */
  public boolean connect() {
    try {
      if (connection != null && !connection.isClosed()) {
        return true;
      }

      // Create folder only when actually connecting to database
      File parentFolder = dbFile.getParentFile();
      if (!parentFolder.exists()) {
        parentFolder.mkdirs();
      }

      String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
      connection = DriverManager.getConnection(url);

      plugin.getLogger().info("[" + scriptName + "] Connected to database");
      return true;

    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "[" + scriptName + "] Failed to connect to database", e);
      return false;
    }
  }

  /** Close the database connection */
  public void disconnect() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
        plugin.getLogger().info("[" + scriptName + "] Disconnected from database");
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.WARNING, "[" + scriptName + "] Error closing database", e);
    }
  }

  /**
   * Execute an update query (INSERT, UPDATE, DELETE, CREATE TABLE, etc.)
   *
   * @param sql The SQL query
   * @param params Parameters for the query
   * @return Number of rows affected
   */
  public int executeUpdate(String sql, Object... params) {
    connect();

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      setParameters(stmt, params);
      return stmt.executeUpdate();
    } catch (SQLException e) {
      plugin
          .getLogger()
          .log(Level.SEVERE, "[" + scriptName + "] Error executing update: " + sql, e);
      return -1;
    }
  }

  /**
   * Execute a query and return results
   *
   * @param sql The SQL query
   * @param params Parameters for the query
   * @return List of rows (each row is a Map of column name to value)
   */
  public List<Map<String, Object>> executeQuery(String sql, Object... params) {
    connect();
    List<Map<String, Object>> results = new ArrayList<>();

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      setParameters(stmt, params);

      try (ResultSet rs = stmt.executeQuery()) {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        while (rs.next()) {
          Map<String, Object> row = new HashMap<>();
          for (int i = 1; i <= columnCount; i++) {
            row.put(meta.getColumnName(i), rs.getObject(i));
          }
          results.add(row);
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "[" + scriptName + "] Error executing query: " + sql, e);
    }

    return results;
  }

  /**
   * Execute a query and return a single value
   *
   * @param sql The SQL query
   * @param params Parameters for the query
   * @return The first column of the first row, or null
   */
  public Object querySingle(String sql, Object... params) {
    List<Map<String, Object>> results = executeQuery(sql, params);
    if (results.isEmpty()) {
      return null;
    }

    Map<String, Object> firstRow = results.get(0);
    return firstRow.values().iterator().next();
  }

  /**
   * Check if a table exists
   *
   * @param tableName The table name
   * @return true if exists
   */
  public boolean tableExists(String tableName) {
    connect();

    try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
      return rs.next();
    } catch (SQLException e) {
      plugin
          .getLogger()
          .log(Level.WARNING, "[" + scriptName + "] Error checking table existence", e);
      return false;
    }
  }

  /**
   * Create a table if it doesn't exist
   *
   * @param tableName The table name
   * @param columns Column definitions (e.g., "id INTEGER PRIMARY KEY", "name TEXT")
   * @return true if successful
   */
  public boolean createTable(String tableName, String... columns) {
    if (tableExists(tableName)) {
      return true;
    }

    String sql = "CREATE TABLE " + tableName + " (" + String.join(", ", columns) + ")";
    return executeUpdate(sql) >= 0;
  }

  /**
   * Insert a row into a table
   *
   * @param tableName The table name
   * @param data Map of column names to values
   * @return true if successful
   */
  public boolean insert(String tableName, Map<String, Object> data) {
    if (data.isEmpty()) {
      return false;
    }

    List<String> columns = new ArrayList<>(data.keySet());
    List<Object> values = new ArrayList<>(data.values());

    String columnStr = String.join(", ", columns);
    String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());

    String sql = "INSERT INTO " + tableName + " (" + columnStr + ") VALUES (" + placeholders + ")";
    return executeUpdate(sql, values.toArray()) > 0;
  }

  /**
   * Update rows in a table
   *
   * @param tableName The table name
   * @param data Map of column names to new values
   * @param where WHERE clause (e.g., "id = ?")
   * @param whereParams Parameters for the WHERE clause
   * @return Number of rows updated
   */
  public int update(
      String tableName, Map<String, Object> data, String where, Object... whereParams) {
    if (data.isEmpty()) {
      return 0;
    }

    List<String> setClauses = new ArrayList<>();
    List<Object> allParams = new ArrayList<>();

    for (Map.Entry<String, Object> entry : data.entrySet()) {
      setClauses.add(entry.getKey() + " = ?");
      allParams.add(entry.getValue());
    }

    for (Object param : whereParams) {
      allParams.add(param);
    }

    String sql =
        "UPDATE " + tableName + " SET " + String.join(", ", setClauses) + " WHERE " + where;
    return executeUpdate(sql, allParams.toArray());
  }

  /**
   * Delete rows from a table
   *
   * @param tableName The table name
   * @param where WHERE clause (e.g., "id = ?")
   * @param params Parameters for the WHERE clause
   * @return Number of rows deleted
   */
  public int delete(String tableName, String where, Object... params) {
    String sql = "DELETE FROM " + tableName + " WHERE " + where;
    return executeUpdate(sql, params);
  }

  private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      stmt.setObject(i + 1, params[i]);
    }
  }

  /**
   * Get the database file
   *
   * @return The database file
   */
  public File getDatabaseFile() {
    return dbFile;
  }
}
