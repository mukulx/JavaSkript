package dev.mukulx.javaskript.api.economy;

import java.text.DecimalFormat;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Economy provider delegating to the server's registered Vault Economy service (e.g. EssentialsX,
 * CMI, UltraEconomy, etc.).
 */
public class VaultEconomyProvider implements EconomyProvider {

  private static final DecimalFormat SHORT_FORMAT = new DecimalFormat("#,##0.#");

  private final Economy vault;

  public VaultEconomyProvider(Economy vault) {
    if (vault == null) {
      throw new IllegalArgumentException("Vault Economy instance cannot be null");
    }
    this.vault = vault;
  }

  public Economy getVaultEconomy() {
    return vault;
  }

  @Override
  public String getName() {
    return "Vault (" + vault.getName() + ")";
  }

  @Override
  public String getCurrencySymbol() {
    String formatted = vault.format(1.0);
    if (formatted != null && !formatted.isEmpty()) {
      char first = formatted.charAt(0);
      if (!Character.isDigit(first) && first != ' ') {
        return String.valueOf(first);
      }
    }
    return "$";
  }

  @Override
  public String getCurrencySingular() {
    try {
      String s = vault.currencyNameSingular();
      return s != null && !s.isEmpty() ? s : "Dollar";
    } catch (Throwable ignored) {
      return "Dollar";
    }
  }

  @Override
  public String getCurrencyPlural() {
    try {
      String p = vault.currencyNamePlural();
      return p != null && !p.isEmpty() ? p : "Dollars";
    } catch (Throwable ignored) {
      return "Dollars";
    }
  }

  @Override
  public double getBalance(UUID uuid) {
    if (uuid == null) return 0.0;
    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
    return vault.getBalance(player);
  }

  @Override
  public double getBalance(OfflinePlayer player) {
    if (player == null) return 0.0;
    return vault.getBalance(player);
  }

  @Override
  public boolean has(UUID uuid, double amount) {
    if (uuid == null) return false;
    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
    return vault.has(player, amount);
  }

  @Override
  public boolean has(OfflinePlayer player, double amount) {
    if (player == null) return false;
    return vault.has(player, amount);
  }

  @Override
  public EconomyResult withdraw(UUID uuid, double amount) {
    if (uuid == null) {
      return EconomyResult.fail("UUID cannot be null", 0.0);
    }
    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
    return withdraw(player, amount);
  }

  @Override
  public EconomyResult withdraw(OfflinePlayer player, double amount) {
    if (player == null) {
      return EconomyResult.fail("Player cannot be null", 0.0);
    }
    if (!Double.isFinite(amount) || amount < 0) {
      return EconomyResult.fail("Cannot withdraw negative amount", vault.getBalance(player));
    }
    EconomyResponse resp = vault.withdrawPlayer(player, amount);
    if (resp.transactionSuccess()) {
      return EconomyResult.success(resp.amount, resp.balance);
    } else {
      return EconomyResult.fail(resp.errorMessage, resp.balance);
    }
  }

  @Override
  public EconomyResult deposit(UUID uuid, double amount) {
    if (uuid == null) {
      return EconomyResult.fail("UUID cannot be null", 0.0);
    }
    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
    return deposit(player, amount);
  }

  @Override
  public EconomyResult deposit(OfflinePlayer player, double amount) {
    if (player == null) {
      return EconomyResult.fail("Player cannot be null", 0.0);
    }
    if (!Double.isFinite(amount) || amount < 0) {
      return EconomyResult.fail("Cannot deposit negative amount", vault.getBalance(player));
    }
    EconomyResponse resp = vault.depositPlayer(player, amount);
    if (resp.transactionSuccess()) {
      return EconomyResult.success(resp.amount, resp.balance);
    } else {
      return EconomyResult.fail(resp.errorMessage, resp.balance);
    }
  }

  @Override
  public EconomyResult set(UUID uuid, double amount) {
    if (uuid == null) {
      return EconomyResult.fail("UUID cannot be null", 0.0);
    }
    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
    return set(player, amount);
  }

  @Override
  public EconomyResult set(OfflinePlayer player, double amount) {
    if (player == null) {
      return EconomyResult.fail("Player cannot be null", 0.0);
    }
    if (!Double.isFinite(amount) || amount < 0) {
      return EconomyResult.fail("Balance cannot be negative", vault.getBalance(player));
    }
    double current = vault.getBalance(player);
    if (amount > current) {
      return deposit(player, amount - current);
    } else if (amount < current) {
      return withdraw(player, current - amount);
    }
    return EconomyResult.success(0.0, current);
  }

  @Override
  public String format(double amount) {
    try {
      return vault.format(amount);
    } catch (Throwable t) {
      return getCurrencySymbol() + amount;
    }
  }

  @Override
  public String formatShort(double amount) {
    String sym = getCurrencySymbol();
    if (amount >= 1_000_000_000_000L) {
      return sym + SHORT_FORMAT.format(amount / 1_000_000_000_000.0) + "T";
    }
    if (amount >= 1_000_000_000L) {
      return sym + SHORT_FORMAT.format(amount / 1_000_000_000.0) + "B";
    }
    if (amount >= 1_000_000L) {
      return sym + SHORT_FORMAT.format(amount / 1_000_000.0) + "M";
    }
    if (amount >= 1_000L) {
      return sym + SHORT_FORMAT.format(amount / 1_000.0) + "k";
    }
    return format(amount);
  }
}
