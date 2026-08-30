package dev.mukulx.javaskript.api.economy;

/** Result of an economy transaction (withdraw, deposit, set, transfer). */
public class EconomyResult {

  private final boolean success;
  private final double amount;
  private final double balance;
  private final String errorMessage;

  public EconomyResult(boolean success, double amount, double balance, String errorMessage) {
    this.success = success;
    this.amount = amount;
    this.balance = balance;
    this.errorMessage = errorMessage;
  }

  public static EconomyResult success(double amount, double balance) {
    return new EconomyResult(true, amount, balance, null);
  }

  public static EconomyResult fail(String errorMessage, double currentBalance) {
    return new EconomyResult(false, 0.0, currentBalance, errorMessage);
  }

  /** Whether the transaction succeeded. */
  public boolean isSuccess() {
    return success;
  }

  /** The amount transacted. */
  public double getAmount() {
    return amount;
  }

  /** The resulting balance after the transaction. */
  public double getBalance() {
    return balance;
  }

  /** Error message if transaction failed, or empty string. */
  public String getErrorMessage() {
    return errorMessage != null ? errorMessage : "";
  }

  @Override
  public String toString() {
    return "EconomyResult{success="
        + success
        + ", amount="
        + amount
        + ", balance="
        + balance
        + (errorMessage != null ? ", error='" + errorMessage + '\'' : "")
        + '}';
  }
}
