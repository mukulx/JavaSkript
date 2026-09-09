package dev.mukulx.javaskript.api.economy;

import java.util.UUID;
import org.bukkit.OfflinePlayer;

/**
 * Fallback provider used when the economy subsystem is disabled in config.yml or when no external
 * provider is available.
 *
 * <p>Prevents NullPointerExceptions while ensuring economy operations fail gracefully with clear
 * explanatory messages.
 */
public class DisabledEconomyProvider implements EconomyProvider {

  private final String message;

  public DisabledEconomyProvider() {
    this("Economy subsystem is disabled in config.yml (economy.enabled is false)");
  }

  public DisabledEconomyProvider(String message) {
    this.message = message != null ? message : "Economy subsystem is disabled in config.yml";
  }

  @Override
  public String getName() {
    return "Disabled";
  }

  @Override
  public String getCurrencySymbol() {
    return "$";
  }

  @Override
  public String getCurrencySingular() {
    return "Coin";
  }

  @Override
  public String getCurrencyPlural() {
    return "Coins";
  }

  @Override
  public double getBalance(UUID uuid) {
    return 0.0;
  }

  @Override
  public double getBalance(OfflinePlayer player) {
    return 0.0;
  }

  @Override
  public boolean has(UUID uuid, double amount) {
    return false;
  }

  @Override
  public boolean has(OfflinePlayer player, double amount) {
    return false;
  }

  @Override
  public EconomyResult withdraw(UUID uuid, double amount) {
    return EconomyResult.fail(message, 0.0);
  }

  @Override
  public EconomyResult withdraw(OfflinePlayer player, double amount) {
    return EconomyResult.fail(message, 0.0);
  }

  @Override
  public EconomyResult deposit(UUID uuid, double amount) {
    return EconomyResult.fail(message, 0.0);
  }

  @Override
  public EconomyResult deposit(OfflinePlayer player, double amount) {
    return EconomyResult.fail(message, 0.0);
  }

  @Override
  public EconomyResult set(UUID uuid, double amount) {
    return EconomyResult.fail(message, 0.0);
  }

  @Override
  public EconomyResult set(OfflinePlayer player, double amount) {
    return EconomyResult.fail(message, 0.0);
  }

  @Override
  public String format(double amount) {
    return "$" + String.format("%.2f", amount);
  }

  @Override
  public String formatShort(double amount) {
    return "$" + String.format("%.1f", amount);
  }
}
