package dev.mukulx.javaskript.api.economy;

import java.util.Collections;
import java.util.List;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * Vault Economy Provider implementation registered by JavaSkript when built-in economy is active.
 * Extends AbstractEconomy from Vault for seamless backward-compatible string/UUID handling.
 */
@SuppressWarnings("deprecation")
public class JavaSkriptVaultHook extends AbstractEconomy {

  private final BuiltinEconomyProvider provider;

  public JavaSkriptVaultHook(BuiltinEconomyProvider provider) {
    this.provider = provider;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String getName() {
    return "JavaSkript";
  }

  @Override
  public boolean hasBankSupport() {
    return false;
  }

  @Override
  public int fractionalDigits() {
    return 2;
  }

  @Override
  public String format(double amount) {
    return provider.format(amount);
  }

  @Override
  public String currencyNamePlural() {
    return provider.getCurrencyPlural();
  }

  @Override
  public String currencyNameSingular() {
    return provider.getCurrencySingular();
  }

  @Override
  public boolean hasAccount(String playerName) {
    return true;
  }

  @Override
  public boolean hasAccount(OfflinePlayer player) {
    return true;
  }

  @Override
  public boolean hasAccount(String playerName, String worldName) {
    return hasAccount(playerName);
  }

  @Override
  public boolean hasAccount(OfflinePlayer player, String worldName) {
    return hasAccount(player);
  }

  @Override
  @SuppressWarnings("deprecation")
  public double getBalance(String playerName) {
    if (playerName == null) return 0.0;
    OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
    return getBalance(p);
  }

  @Override
  public double getBalance(OfflinePlayer player) {
    if (player == null) return 0.0;
    return provider.getBalance(player.getUniqueId());
  }

  @Override
  public double getBalance(String playerName, String world) {
    return getBalance(playerName);
  }

  @Override
  public double getBalance(OfflinePlayer player, String world) {
    return getBalance(player);
  }

  @Override
  public boolean has(String playerName, double amount) {
    return getBalance(playerName) >= amount;
  }

  @Override
  public boolean has(OfflinePlayer player, double amount) {
    return getBalance(player) >= amount;
  }

  @Override
  public boolean has(String playerName, String worldName, double amount) {
    return has(playerName, amount);
  }

  @Override
  public boolean has(OfflinePlayer player, String worldName, double amount) {
    return has(player, amount);
  }

  @Override
  @SuppressWarnings("deprecation")
  public EconomyResponse withdrawPlayer(String playerName, double amount) {
    if (playerName == null) {
      return new EconomyResponse(
          0, 0, EconomyResponse.ResponseType.FAILURE, "Player cannot be null");
    }
    OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
    return withdrawPlayer(p, amount);
  }

  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
    if (player == null) {
      return new EconomyResponse(
          0, 0, EconomyResponse.ResponseType.FAILURE, "Player cannot be null");
    }
    EconomyResult res = provider.withdraw(player.getUniqueId(), amount);
    if (res.isSuccess()) {
      return new EconomyResponse(
          res.getAmount(), res.getBalance(), EconomyResponse.ResponseType.SUCCESS, null);
    } else {
      return new EconomyResponse(
          0, res.getBalance(), EconomyResponse.ResponseType.FAILURE, res.getErrorMessage());
    }
  }

  @Override
  public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
    return withdrawPlayer(playerName, amount);
  }

  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
    return withdrawPlayer(player, amount);
  }

  @Override
  @SuppressWarnings("deprecation")
  public EconomyResponse depositPlayer(String playerName, double amount) {
    if (playerName == null) {
      return new EconomyResponse(
          0, 0, EconomyResponse.ResponseType.FAILURE, "Player cannot be null");
    }
    OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
    return depositPlayer(p, amount);
  }

  @Override
  public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
    if (player == null) {
      return new EconomyResponse(
          0, 0, EconomyResponse.ResponseType.FAILURE, "Player cannot be null");
    }
    EconomyResult res = provider.deposit(player.getUniqueId(), amount);
    if (res.isSuccess()) {
      return new EconomyResponse(
          res.getAmount(), res.getBalance(), EconomyResponse.ResponseType.SUCCESS, null);
    } else {
      return new EconomyResponse(
          0, res.getBalance(), EconomyResponse.ResponseType.FAILURE, res.getErrorMessage());
    }
  }

  @Override
  public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
    return depositPlayer(playerName, amount);
  }

  @Override
  public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
    return depositPlayer(player, amount);
  }

  @Override
  public EconomyResponse createBank(String name, String player) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse createBank(String name, OfflinePlayer player) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse deleteBank(String name) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse bankBalance(String name) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse bankHas(String name, double amount) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse bankWithdraw(String name, double amount) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse bankDeposit(String name, double amount) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse isBankOwner(String name, String playerName) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse isBankMember(String name, String playerName) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public EconomyResponse isBankMember(String name, OfflinePlayer player) {
    return new EconomyResponse(
        0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
  }

  @Override
  public List<String> getBanks() {
    return Collections.emptyList();
  }

  @Override
  public boolean createPlayerAccount(String playerName) {
    return true;
  }

  @Override
  public boolean createPlayerAccount(OfflinePlayer player) {
    return true;
  }

  @Override
  public boolean createPlayerAccount(String playerName, String worldName) {
    return true;
  }

  @Override
  public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
    return true;
  }
}
