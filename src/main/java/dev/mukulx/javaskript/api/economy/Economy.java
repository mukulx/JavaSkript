package dev.mukulx.javaskript.api.economy;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.OfflinePlayer;

/**
 * Static shorthand facade for {@link EconomyHelper}.
 *
 * <p>Enables 1-line economy operations anywhere without injecting any fields:
 *
 * <pre>{@code
 * if (Economy.has(player, 500)) {
 *     Economy.withdraw(player, 500);
 *     Players.msg(player, "<green>Paid $500!");
 * }
 * }</pre>
 */
public final class Economy {

  private static EconomyHelper instance;

  private Economy() {}

  public static void setInstance(EconomyHelper helper) {
    instance = helper;
  }

  private static EconomyHelper get() {
    if (instance == null) {
      instance = JavaSkriptPlugin.getInstance().getAPI().getEconomyHelper();
    }
    return instance;
  }

  /** Check if the economy subsystem is enabled and has an active provider. */
  public static boolean isEnabled() {
    return instance != null && instance.isEnabled();
  }

  /** Get active economy provider name (e.g. "Vault (Essentials)", "JavaSkript"). */
  public static String getProviderName() {
    return get().getProvider().getName();
  }

  /** Get a player's balance. */
  public static double getBalance(UUID uuid) {
    return get().getBalance(uuid);
  }

  /** Get a player's balance. */
  public static double getBalance(OfflinePlayer player) {
    return get().getBalance(player);
  }

  /** Check if a player has at least the specified balance. */
  public static boolean has(UUID uuid, double amount) {
    return get().has(uuid, amount);
  }

  /** Check if a player has at least the specified balance. */
  public static boolean has(OfflinePlayer player, double amount) {
    return get().has(player, amount);
  }

  /** Withdraw money from a player. */
  public static EconomyResult withdraw(UUID uuid, double amount) {
    return get().withdraw(uuid, amount);
  }

  /** Withdraw money from a player. */
  public static EconomyResult withdraw(OfflinePlayer player, double amount) {
    return get().withdraw(player, amount);
  }

  /** Deposit money to a player. */
  public static EconomyResult deposit(UUID uuid, double amount) {
    return get().deposit(uuid, amount);
  }

  /** Deposit money to a player. */
  public static EconomyResult deposit(OfflinePlayer player, double amount) {
    return get().deposit(player, amount);
  }

  /** Set a player's balance. */
  public static EconomyResult set(UUID uuid, double amount) {
    return get().set(uuid, amount);
  }

  /** Set a player's balance. */
  public static EconomyResult set(OfflinePlayer player, double amount) {
    return get().set(player, amount);
  }

  /** Atomically transfer money between two players. */
  public static EconomyResult transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
    return get().transfer(from, to, amount);
  }

  /** Format amount with currency symbol (e.g. "$1,500.00"). */
  public static String format(double amount) {
    return get().format(amount);
  }

  /** Format amount with compact suffix (e.g. "$1.5k", "$2.3M", "$5.0B"). */
  public static String formatShort(double amount) {
    return get().formatShort(amount);
  }

  /** Access a custom registered currency (e.g. "gems", "tokens"). */
  public static Optional<EconomyProvider> currency(String name) {
    return get().currency(name);
  }

  /** Register a custom currency provider. */
  public static void registerCurrency(String name, EconomyProvider provider) {
    get().registerCurrency(name, provider);
  }

  /** Get top richest players if using built-in engine. */
  public static List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
    return get().getTopBalances(limit);
  }
}
