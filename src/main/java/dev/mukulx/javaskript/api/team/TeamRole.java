package dev.mukulx.javaskript.api.team;

/** Hierarchical roles for team members with built-in permission evaluation. */
public enum TeamRole {
  MEMBER(1, "Member"),
  CAPTAIN(2, "Captain"),
  LEADER(3, "Leader");

  private final int priority;
  private final String displayName;

  TeamRole(int priority, String displayName) {
    this.priority = priority;
    this.displayName = displayName;
  }

  public int getPriority() {
    return priority;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this role has at least the priority of the target role.
   *
   * @param other The role to compare against
   * @return true if this role priority is greater than or equal to other
   */
  public boolean isAtLeast(TeamRole other) {
    return other != null && this.priority >= other.priority;
  }

  /**
   * Checks if this role has strictly higher priority than the target role.
   *
   * @param other The role to compare against
   * @return true if this role priority is strictly higher than other
   */
  public boolean isHigherThan(TeamRole other) {
    return other != null && this.priority > other.priority;
  }

  /**
   * Checks whether this role can invite new players to the team.
   *
   * @return true if captain or leader
   */
  public boolean canInvite() {
    return isAtLeast(CAPTAIN);
  }

  /**
   * Checks whether this role can kick a member of the target role.
   *
   * @param targetRole The role of the member to be kicked
   * @return true if captain or leader and strictly higher priority than target
   */
  public boolean canKick(TeamRole targetRole) {
    return isAtLeast(CAPTAIN) && isHigherThan(targetRole);
  }

  /**
   * Checks whether this role can set or update the team home location.
   *
   * @return true if captain or leader
   */
  public boolean canSetHome() {
    return isAtLeast(CAPTAIN);
  }

  /**
   * Checks whether this role can toggle team settings (e.g. friendly fire, open).
   *
   * @return true if captain or leader
   */
  public boolean canToggleSettings() {
    return isAtLeast(CAPTAIN);
  }

  /**
   * Checks whether this role can withdraw money from the team bank account.
   *
   * @return true if captain or leader
   */
  public boolean canWithdraw() {
    return isAtLeast(CAPTAIN);
  }

  /**
   * Checks whether this role can deposit money into the team bank account.
   *
   * @return true if member or above
   */
  public boolean canDeposit() {
    return isAtLeast(MEMBER);
  }

  /**
   * Checks whether this role can disband the team.
   *
   * @return true only if leader
   */
  public boolean canDisband() {
    return this == LEADER;
  }

  /**
   * Checks whether this role can transfer leadership of the team.
   *
   * @return true only if leader
   */
  public boolean canTransfer() {
    return this == LEADER;
  }

  /**
   * Parses a role name into a TeamRole with a fallback.
   *
   * @param name The role name string
   * @param fallback The fallback role if parsing fails
   * @return parsed TeamRole or fallback
   */
  public static TeamRole fromString(String name, TeamRole fallback) {
    if (name == null || name.isBlank()) {
      return fallback;
    }
    try {
      return TeamRole.valueOf(name.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return fallback;
    }
  }
}
