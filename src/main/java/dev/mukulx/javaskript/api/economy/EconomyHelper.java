package dev.mukulx.javaskript.api.economy;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

/**
 * Universal Economy Engine for JavaSkript.
 *
 * <p>Seamlessly integrates with:
 *
 * <ul>
 *   <li><b>Vault Economy</b> (EssentialsX, CMI, UltraEconomy, etc.)
 *   <li><b>Built-in SQLite Persistent Economy</b> (zero-setup native economy)
 *   <li><b>Custom Currencies</b> (Gems, Tokens, Credits, Souls)
 * </ul>
 *
 * <p>Usable via injection ({@code private EconomyHelper economy;}) and statically via {@link
 * Economy}.
 */
public class EconomyHelper {

  private final JavaSkriptPlugin plugin;
  private final Map<String, EconomyProvider> customCurrencies;
  private EconomyProvider primaryProvider;
  private BuiltinEconomyProvider builtinProvider;

  public EconomyHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.customCurrencies = new ConcurrentHashMap<>();
    initProvider();
  }

  private void initProvider() {
    String mode = plugin.getConfig().getString("economy.mode", "auto").toLowerCase();
    String symbol = plugin.getConfig().getString("economy.currency.symbol", "$");
    String singular = plugin.getConfig().getString("economy.currency.name-singular", "Coin");
    String plural = plugin.getConfig().getString("economy.currency.name-plural", "Coins");
    double startBal = plugin.getConfig().getDouble("economy.currency.starting-balance", 0.0);
    boolean registerVaultService =
        plugin.getConfig().getBoolean("economy.register-vault-service", true);

    if ("disabled".equals(mode)) {
      plugin.getLogger().info("Economy subsystem is disabled in config.yml");
      return;
    }

    // Initialize built-in provider
    this.builtinProvider = new BuiltinEconomyProvider(plugin, symbol, singular, plural, startBal);

    if ("builtin".equals(mode)) {
      this.primaryProvider = builtinProvider;
      plugin.getLogger().info("Using built-in SQLite persistent economy engine");
      if (registerVaultService) {
        registerAsVaultService();
      }
      return;
    }

    // Try Vault (for mode "auto" or "vault")
    boolean vaultHooked = hookVault();

    if (vaultHooked) {
      plugin.getLogger().info("Successfully hooked into " + primaryProvider.getName());
    } else {
      if ("vault".equals(mode)) {
        plugin
            .getLogger()
            .warning(
                "Vault mode was forced in config.yml, but no active Vault provider was found!");
      }
      // Fallback to built-in SQLite engine
      this.primaryProvider = builtinProvider;
      plugin
          .getLogger()
          .info("No external economy found — using built-in SQLite persistent economy");
      if (registerVaultService) {
        registerAsVaultService();
      }
    }
  }

  private boolean hookVault() {
    if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
      return false;
    }
    try {
      RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
          Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
      if (rsp != null && rsp.getProvider() != null) {
        this.primaryProvider = new VaultEconomyProvider(rsp.getProvider());
        return true;
      }
    } catch (Throwable t) {
      plugin.debug("Error checking Vault registration: " + t.getMessage());
    }
    return false;
  }

  private void registerAsVaultService() {
    if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
      return;
    }
    try {
      JavaSkriptVaultHook hook = new JavaSkriptVaultHook(builtinProvider);
      Bukkit.getServicesManager()
          .register(net.milkbowl.vault.economy.Economy.class, hook, plugin, ServicePriority.Normal);
      plugin.getLogger().info("Registered JavaSkript as the server's Vault Economy provider!");
    } catch (Throwable t) {
      plugin.getLogger().warning("Could not register JavaSkript Vault service: " + t.getMessage());
    }
  }

  /** Get the active primary economy provider. */
  public EconomyProvider getProvider() {
    return primaryProvider != null ? primaryProvider : builtinProvider;
  }

  /** Get the built-in SQLite provider instance (if needed directly). */
  public BuiltinEconomyProvider getBuiltinProvider() {
    return builtinProvider;
  }

  // ==========================================
  // Custom Currencies (Multi-Economy)
  // ==========================================

  /** Register a custom currency provider (e.g. "gems", "tokens", "credits"). */
  public void registerCurrency(String name, EconomyProvider provider) {
    if (name != null && provider != null) {
      customCurrencies.put(name.toLowerCase(), provider);
      plugin.getLogger().info("Registered custom currency: " + name);
    }
  }

  /** Access a specific currency provider by name. Returns empty if not registered. */
  public Optional<EconomyProvider> currency(String name) {
    if (name == null) return Optional.empty();
    return Optional.ofNullable(customCurrencies.get(name.toLowerCase()));
  }

  // ==========================================
  // Core Economy Operations
  // ==========================================

  /** Get a player's balance. */
  public double getBalance(UUID uuid) {
    return getProvider().getBalance(uuid);
  }

  /** Get a player's balance. */
  public double getBalance(OfflinePlayer player) {
    return getProvider().getBalance(player);
  }

  /** Check if a player has at least the specified balance. */
  public boolean has(UUID uuid, double amount) {
    return getProvider().has(uuid, amount);
  }

  /** Check if a player has at least the specified balance. */
  public boolean has(OfflinePlayer player, double amount) {
    return getProvider().has(player, amount);
  }

  /** Withdraw money from a player. */
  public EconomyResult withdraw(UUID uuid, double amount) {
    return getProvider().withdraw(uuid, amount);
  }

  /** Withdraw money from a player. */
  public EconomyResult withdraw(OfflinePlayer player, double amount) {
    return getProvider().withdraw(player, amount);
  }

  /** Deposit money to a player. */
  public EconomyResult deposit(UUID uuid, double amount) {
    return getProvider().deposit(uuid, amount);
  }

  /** Deposit money to a player. */
  public EconomyResult deposit(OfflinePlayer player, double amount) {
    return getProvider().deposit(player, amount);
  }

  /** Set a player's balance. */
  public EconomyResult set(UUID uuid, double amount) {
    return getProvider().set(uuid, amount);
  }

  /** Set a player's balance. */
  public EconomyResult set(OfflinePlayer player, double amount) {
    return getProvider().set(player, amount);
  }

  /**
   * Safely transfer money between two players atomically. If the sender doesn't have sufficient
   * funds, no money is moved.
   */
  public synchronized EconomyResult transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
    if (from == null || to == null) {
      return EconomyResult.fail("Sender and recipient must not be null", 0.0);
    }
    if (amount <= 0) {
      return EconomyResult.fail("Transfer amount must be positive", getBalance(from));
    }

    // 1. Withdraw from sender
    EconomyResult withdrawResult = withdraw(from, amount);
    if (!withdrawResult.isSuccess()) {
      return withdrawResult;
    }

    // 2. Deposit to recipient
    EconomyResult depositResult = deposit(to, amount);
    if (!depositResult.isSuccess()) {
      // Rollback
      deposit(from, amount);
      return EconomyResult.fail(
          "Failed to deposit to recipient: " + depositResult.getErrorMessage(), getBalance(from));
    }

    return EconomyResult.success(amount, withdrawResult.getBalance());
  }

  /** Format amount with currency symbol (e.g. "$1,500.00"). */
  public String format(double amount) {
    return getProvider().format(amount);
  }

  /** Format amount with compact suffix (e.g. "$1.5k", "$2.3M", "$5.0B"). */
  public String formatShort(double amount) {
    return getProvider().formatShort(amount);
  }

  /** Get top richest players if using built-in economy engine. */
  public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
    if (builtinProvider != null) {
      return builtinProvider.getTopBalances(limit);
    }
    return Collections.emptyList();
  }

  public void shutdown() {
    if (builtinProvider != null) {
      builtinProvider.close();
    }
    customCurrencies.clear();
  }
}
