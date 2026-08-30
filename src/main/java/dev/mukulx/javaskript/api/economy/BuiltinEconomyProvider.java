package dev.mukulx.javaskript.api.economy;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** High-performance, persistent SQLite-backed built-in Economy provider. */
public class BuiltinEconomyProvider implements EconomyProvider {

  private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.00");
  private static final DecimalFormat SHORT_FORMAT = new DecimalFormat("#,##0.#");

  private final JavaSkriptPlugin plugin;
  private final String symbol;
  private final String singular;
  private final String plural;
  private final double startingBalance;
  private final File dbFile;
  private final Map<UUID, Double> cache;
  private Connection connection;

  public BuiltinEconomyProvider(
      JavaSkriptPlugin plugin,
      String symbol,
      String singular,
      String plural,
      double startingBalance) {
    this.plugin = plugin;
    this.symbol = symbol != null ? symbol : "$";
    this.singular = singular != null ? singular : "Coin";
    this.plural = plural != null ? plural : "Coins";
    this.startingBalance = Math.max(0.0, startingBalance);
    this.cache = new ConcurrentHashMap<>();

    File dataDir = new File(plugin.getDataFolder(), "data");
    if (!dataDir.exists()) {
      dataDir.mkdirs();
    }
    this.dbFile = new File(dataDir, "economy.db");
    initDatabase();
  }

  private synchronized void initDatabase() {
    try {
      String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
      this.connection = DriverManager.getConnection(url);

      try (Statement stmt = connection.createStatement()) {
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS economy ("
                + "uuid VARCHAR(36) PRIMARY KEY, "
                + "balance DOUBLE NOT NULL DEFAULT 0.0);");
        // Enable WAL mode for performance
        stmt.execute("PRAGMA journal_mode=WAL;");
        stmt.execute("PRAGMA synchronous=NORMAL;");
      }
      plugin.debug("Built-in SQLite economy initialized at: " + dbFile.getName());
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to initialize built-in SQLite economy", e);
    }
  }

  private synchronized Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
      initDatabase();
    }
    return connection;
  }

  @Override
  public String getName() {
    return "JavaSkript";
  }

  @Override
  public String getCurrencySymbol() {
    return symbol;
  }

  @Override
  public String getCurrencySingular() {
    return singular;
  }

  @Override
  public String getCurrencyPlural() {
    return plural;
  }

  @Override
  public double getBalance(UUID uuid) {
    if (uuid == null) return 0.0;

    // Check fast memory cache
    Double cached = cache.get(uuid);
    if (cached != null) {
      return cached;
    }

    // Read from SQLite
    try {
      Connection conn = getConnection();
      try (PreparedStatement ps =
          conn.prepareStatement("SELECT balance FROM economy WHERE uuid = ?;")) {
        ps.setString(1, uuid.toString());
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            double bal = rs.getDouble("balance");
            cache.put(uuid, bal);
            return bal;
          }
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Error loading balance for " + uuid, e);
    }

    // Default starting balance
    cache.put(uuid, startingBalance);
    saveBalance(uuid, startingBalance);
    return startingBalance;
  }

  @Override
  public synchronized EconomyResult withdraw(UUID uuid, double amount) {
    if (uuid == null) {
      return EconomyResult.fail("UUID cannot be null", 0.0);
    }
    if (amount < 0) {
      return EconomyResult.fail("Cannot withdraw negative amount", getBalance(uuid));
    }

    double current = getBalance(uuid);
    if (current < amount) {
      return EconomyResult.fail("Insufficient funds", current);
    }

    double newBalance = current - amount;
    cache.put(uuid, newBalance);
    saveBalance(uuid, newBalance);
    return EconomyResult.success(amount, newBalance);
  }

  @Override
  public synchronized EconomyResult deposit(UUID uuid, double amount) {
    if (uuid == null) {
      return EconomyResult.fail("UUID cannot be null", 0.0);
    }
    if (amount < 0) {
      return EconomyResult.fail("Cannot deposit negative amount", getBalance(uuid));
    }

    double current = getBalance(uuid);
    double newBalance = current + amount;
    cache.put(uuid, newBalance);
    saveBalance(uuid, newBalance);
    return EconomyResult.success(amount, newBalance);
  }

  @Override
  public synchronized EconomyResult set(UUID uuid, double amount) {
    if (uuid == null) {
      return EconomyResult.fail("UUID cannot be null", 0.0);
    }
    if (amount < 0) {
      return EconomyResult.fail("Balance cannot be negative", getBalance(uuid));
    }

    cache.put(uuid, amount);
    saveBalance(uuid, amount);
    return EconomyResult.success(amount, amount);
  }

  private void saveBalance(UUID uuid, double balance) {
    try {
      Connection conn = getConnection();
      try (PreparedStatement ps =
          conn.prepareStatement(
              "INSERT INTO economy (uuid, balance) VALUES (?, ?) "
                  + "ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance;")) {
        ps.setString(1, uuid.toString());
        ps.setDouble(2, balance);
        ps.executeUpdate();
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Error saving balance for " + uuid, e);
    }
  }

  /** Get top players by balance for /baltop. */
  public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
    List<Map.Entry<UUID, Double>> top = new ArrayList<>();
    try {
      Connection conn = getConnection();
      try (PreparedStatement ps =
          conn.prepareStatement(
              "SELECT uuid, balance FROM economy ORDER BY balance DESC LIMIT ?;")) {
        ps.setInt(1, Math.max(1, limit));
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            UUID id = UUID.fromString(rs.getString("uuid"));
            double bal = rs.getDouble("balance");
            top.add(Map.entry(id, bal));
          }
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Error querying top balances", e);
    }
    return top;
  }

  @Override
  public String format(double amount) {
    return symbol + FORMAT.format(amount);
  }

  @Override
  public String formatShort(double amount) {
    if (amount >= 1_000_000_000_000L) {
      return symbol + SHORT_FORMAT.format(amount / 1_000_000_000_000.0) + "T";
    }
    if (amount >= 1_000_000_000L) {
      return symbol + SHORT_FORMAT.format(amount / 1_000_000_000.0) + "B";
    }
    if (amount >= 1_000_000L) {
      return symbol + SHORT_FORMAT.format(amount / 1_000_000.0) + "M";
    }
    if (amount >= 1_000L) {
      return symbol + SHORT_FORMAT.format(amount / 1_000.0) + "k";
    }
    return format(amount);
  }

  public void close() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException ignored) {
    }
  }
}
