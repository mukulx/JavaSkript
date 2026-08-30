package dev.mukulx.javaskript.api.economy;

import java.util.UUID;
import org.bukkit.OfflinePlayer;

/** Universal Economy Provider interface for JavaSkript. */
public interface EconomyProvider {

  /** Provider name (e.g. "Vault", "JavaSkript", "Gems"). */
  String getName();

  /** Currency symbol (e.g. "$", "✦", "💎"). */
  String getCurrencySymbol();

  /** Singular currency name (e.g. "Coin", "Gem", "Dollar"). */
  String getCurrencySingular();

  /** Plural currency name (e.g. "Coins", "Gems", "Dollars"). */
  String getCurrencyPlural();

  /** Get player balance by UUID. */
  double getBalance(UUID uuid);

  /** Get player balance by OfflinePlayer or Player. */
  default double getBalance(OfflinePlayer player) {
    return player != null ? getBalance(player.getUniqueId()) : 0.0;
  }

  /** Check if a player has at least the specified amount. */
  default boolean has(UUID uuid, double amount) {
    return getBalance(uuid) >= amount;
  }

  /** Check if a player has at least the specified amount. */
  default boolean has(OfflinePlayer player, double amount) {
    return player != null && has(player.getUniqueId(), amount);
  }

  /** Withdraw amount from a player. */
  EconomyResult withdraw(UUID uuid, double amount);

  /** Withdraw amount from a player. */
  default EconomyResult withdraw(OfflinePlayer player, double amount) {
    return player != null
        ? withdraw(player.getUniqueId(), amount)
        : EconomyResult.fail("Player cannot be null", 0.0);
  }

  /** Deposit amount to a player. */
  EconomyResult deposit(UUID uuid, double amount);

  /** Deposit amount to a player. */
  default EconomyResult deposit(OfflinePlayer player, double amount) {
    return player != null
        ? deposit(player.getUniqueId(), amount)
        : EconomyResult.fail("Player cannot be null", 0.0);
  }

  /** Set player balance to a specific amount. */
  EconomyResult set(UUID uuid, double amount);

  /** Set player balance to a specific amount. */
  default EconomyResult set(OfflinePlayer player, double amount) {
    return player != null
        ? set(player.getUniqueId(), amount)
        : EconomyResult.fail("Player cannot be null", 0.0);
  }

  /** Format amount with currency symbol and standard comma separation (e.g. "$1,500.00"). */
  String format(double amount);

  /** Format amount with compact suffix (e.g. "$1.5k", "$2.3M", "$5.0B"). */
  String formatShort(double amount);
}
