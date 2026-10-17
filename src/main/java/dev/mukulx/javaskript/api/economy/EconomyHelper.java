package dev.mukulx.javaskript.api.economy;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
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
  private volatile EconomyProvider overrideProvider;
  // Object, not JavaSkriptVaultHook: Vault is optional and must not be needed to load this class
  private Object vaultHook;
  private boolean enabled;

  public EconomyHelper(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.customCurrencies = new ConcurrentHashMap<>();
    initProvider();
  }

  /** Check if the economy subsystem is enabled in configuration and active. */
  public boolean isEnabled() {
    return overrideProvider != null
        || (enabled && !(primaryProvider instanceof DisabledEconomyProvider));
  }

  /**
   * Reload the configured economy from config.yml. A provider set with {@link #setProvider} and
   * registered currencies are kept.
   */
  public synchronized void reload() {
    closeConfiguredProvider();
    initProvider();
  }

  /**
   * Replace the main economy with a custom one. Everything that uses the economy follows it: {@link
   * Economy}, team banks and, when JavaSkript is registered with Vault, other plugins. It works
   * even when the economy module is disabled in config.yml.
   */
  public void setProvider(EconomyProvider provider) {
    this.overrideProvider = Objects.requireNonNull(provider, "provider cannot be null");
    plugin.getLogger().info("Using custom economy provider: " + provider.getName());
  }

  /** Go back to the economy configured in config.yml. */
  public void resetProvider() {
    if (overrideProvider != null) {
      overrideProvider = null;
      plugin.getLogger().info("Custom economy provider removed; using the configured economy");
    }
  }

  /** Whether a custom provider replaced the configured economy. */
  public boolean hasCustomProvider() {
    return overrideProvider != null;
  }

  /**
   * Remove every provider and currency defined by an unloading script, so nothing keeps calling its
   * classes.
   */
  public void releaseProvidersFrom(ClassLoader scriptLoader) {
    EconomyProvider override = overrideProvider;
    if (override != null && override.getClass().getClassLoader() == scriptLoader) {
      resetProvider();
    }
    customCurrencies
        .entrySet()
        .removeIf(entry -> entry.getValue().getClass().getClassLoader() == scriptLoader);
  }

  private void initProvider() {
    boolean isConfigEnabled = plugin.getConfig().getBoolean("modules.economy.enabled", false);
    String mode = plugin.getConfig().getString("modules.economy.mode", "vault").toLowerCase();

    if (!isConfigEnabled || "disabled".equals(mode)) {
      this.enabled = false;
      this.primaryProvider =
          new DisabledEconomyProvider(
              "Economy subsystem is disabled in config.yml (set economy.enabled: true to enable)");
      this.builtinProvider = null;
      plugin
          .getLogger()
          .info(
              "Economy subsystem is disabled (set economy.enabled: true in config.yml to enable)");
      return;
    }

    this.enabled = true;
    String symbol = plugin.getConfig().getString("modules.economy.currency.symbol", "$");
    String singular =
        plugin.getConfig().getString("modules.economy.currency.name-singular", "Coin");
    String plural = plugin.getConfig().getString("modules.economy.currency.name-plural", "Coins");
    double startBal =
        plugin.getConfig().getDouble("modules.economy.currency.starting-balance", 0.0);
    boolean registerVaultService =
        plugin.getConfig().getBoolean("modules.economy.register-vault-service", false);

    if ("builtin".equals(mode)) {
      this.builtinProvider = new BuiltinEconomyProvider(plugin, symbol, singular, plural, startBal);
      this.primaryProvider = builtinProvider;
      plugin.getLogger().info("Using built-in SQLite persistent economy engine");
      if (registerVaultService) {
        registerAsVaultService();
      }
      return;
    }

    // Try Vault (for mode "vault" or "auto")
    boolean vaultHooked = hookVault();

    if (vaultHooked) {
      plugin.getLogger().info("Successfully hooked into " + primaryProvider.getName());
    } else {
      if ("vault".equals(mode)) {
        this.primaryProvider =
            new DisabledEconomyProvider(
                "No active Vault economy provider found on the server (e.g. EssentialsX, CMI)");
        plugin
            .getLogger()
            .warning(
                "Economy is enabled with mode 'vault', but no external Vault economy provider was"
                    + " found! JavaSkript will hook into Vault once an economy plugin becomes"
                    + " available.");
      } else {
        // Fallback to built-in SQLite engine (for mode "auto")
        this.builtinProvider =
            new BuiltinEconomyProvider(plugin, symbol, singular, plural, startBal);
        this.primaryProvider = builtinProvider;
        plugin
            .getLogger()
            .info("No external economy found — using built-in SQLite persistent economy");
        if (registerVaultService) {
          registerAsVaultService();
        }
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
      // Follows getProvider(), so a custom provider set later is what Vault sees
      Supplier<EconomyProvider> current = this::getProvider;
      JavaSkriptVaultHook hook = new JavaSkriptVaultHook(current);
      Bukkit.getServicesManager()
          .register(net.milkbowl.vault.economy.Economy.class, hook, plugin, ServicePriority.Normal);
      vaultHook = hook;
      plugin.getLogger().info("Registered JavaSkript as the server's Vault Economy provider!");
    } catch (Throwable t) {
      plugin.getLogger().warning("Could not register JavaSkript Vault service: " + t.getMessage());
    }
  }

  /** Get the active primary economy provider. */
  public EconomyProvider getProvider() {
    EconomyProvider override = overrideProvider;
    if (override != null) {
      return override;
    }
    if (enabled && primaryProvider instanceof DisabledEconomyProvider) {
      // Lazy attempt to hook Vault if an external economy plugin registered after startup
      String mode = plugin.getConfig().getString("modules.economy.mode", "vault").toLowerCase();
      if ("vault".equals(mode) || "auto".equals(mode)) {
        if (hookVault()) {
          plugin.getLogger().info("Successfully hooked into " + primaryProvider.getName());
        }
      }
    }
    return primaryProvider != null ? primaryProvider : new DisabledEconomyProvider();
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

  /** Remove a registered currency. Returns whether one was registered under that name. */
  public boolean unregisterCurrency(String name) {
    return name != null && customCurrencies.remove(name.toLowerCase()) != null;
  }

  /** Names of every registered currency, lower case. */
  public Set<String> getCurrencyNames() {
    return Collections.unmodifiableSet(customCurrencies.keySet());
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
    if (!Double.isFinite(amount) || amount <= 0) {
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

  /** Richest accounts of the active provider; empty when it cannot list them (e.g. Vault). */
  public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
    return getProvider().getTopBalances(limit);
  }

  /** Called when the plugin disables. */
  public synchronized void shutdown() {
    closeConfiguredProvider();
    overrideProvider = null;
    customCurrencies.clear();
  }

  private void closeConfiguredProvider() {
    if (vaultHook != null) {
      try {
        Bukkit.getServicesManager().unregister(net.milkbowl.vault.economy.Economy.class, vaultHook);
      } catch (Throwable t) {
        plugin.debug("Could not unregister the Vault economy service: " + t.getMessage());
      }
      vaultHook = null;
    }
    if (builtinProvider != null) {
      builtinProvider.close();
      builtinProvider = null;
    }
  }
}
