package dev.mukulx.javaskript.api.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamTest {

  @BeforeEach
  void setUp() {
    Teams.setInstance(null);
  }

  @Test
  void testRoleHierarchyAndPermissions() {
    assertTrue(TeamRole.LEADER.isAtLeast(TeamRole.CAPTAIN));
    assertTrue(TeamRole.CAPTAIN.isAtLeast(TeamRole.MEMBER));
    assertTrue(TeamRole.MEMBER.isAtLeast(TeamRole.MEMBER));
    assertFalse(TeamRole.MEMBER.isAtLeast(TeamRole.CAPTAIN));

    assertTrue(TeamRole.LEADER.isHigherThan(TeamRole.CAPTAIN));
    assertFalse(TeamRole.CAPTAIN.isHigherThan(TeamRole.CAPTAIN));

    // Permission checks
    assertTrue(TeamRole.LEADER.canDisband());
    assertFalse(TeamRole.CAPTAIN.canDisband());

    assertTrue(TeamRole.CAPTAIN.canInvite());
    assertFalse(TeamRole.MEMBER.canInvite());

    assertTrue(TeamRole.CAPTAIN.canKick(TeamRole.MEMBER));
    assertFalse(TeamRole.CAPTAIN.canKick(TeamRole.CAPTAIN));
    assertFalse(TeamRole.CAPTAIN.canKick(TeamRole.LEADER));

    assertTrue(TeamRole.MEMBER.canDeposit());
    assertFalse(TeamRole.MEMBER.canWithdraw());
    assertTrue(TeamRole.CAPTAIN.canWithdraw());

    assertEquals(TeamRole.LEADER, TeamRole.fromString("leader", TeamRole.MEMBER));
    assertEquals(TeamRole.CAPTAIN, TeamRole.fromString("CAPTAIN", TeamRole.MEMBER));
    assertEquals(TeamRole.MEMBER, TeamRole.fromString("invalid_role", TeamRole.MEMBER));
  }

  @Test
  void testTeamModelDefaultsAndPrefixSuffix() {
    UUID leader = UUID.randomUUID();
    Team team = new Team("dragons", "The Dragons", leader);

    assertEquals("dragons", team.getId());
    assertEquals("The Dragons", team.getName());
    assertEquals(leader, team.getLeader());

    // Prefix and Suffix must be empty by default
    assertEquals("", team.getPrefix());
    assertEquals("", team.getSuffix());

    // Leader is auto-added as member
    assertTrue(team.hasMember(leader));
    assertEquals(1, team.getSize());
    assertTrue(team.isLeader(leader));
    assertTrue(team.isCaptain(leader));

    // Friendly fire defaults to false
    assertFalse(team.isFriendlyFireEnabled());
    team.setFriendlyFire(true);
    assertTrue(team.isFriendlyFireEnabled());

    // Max size and full check
    assertFalse(team.isFull());
    team.setMaxSize(1);
    assertTrue(team.isFull());
    team.setMaxSize(5);
    assertFalse(team.isFull());
  }

  @Test
  void testMemberManagementAndRoles() {
    UUID leader = UUID.randomUUID();
    UUID member1 = UUID.randomUUID();
    UUID member2 = UUID.randomUUID();

    Team team = new Team("knights", "Knights", leader);
    team.addMember(member1, TeamRole.MEMBER);
    team.addMember(member2, TeamRole.CAPTAIN);

    assertEquals(3, team.getSize());
    assertTrue(team.hasMember(member1));
    assertTrue(team.hasMember(member2));

    assertEquals(TeamRole.MEMBER, team.getRole(member1).orElse(null));
    assertEquals(TeamRole.CAPTAIN, team.getRole(member2).orElse(null));

    assertFalse(team.isCaptain(member1));
    assertTrue(team.isCaptain(member2));

    // Promote member1
    team.setRole(member1, TeamRole.CAPTAIN);
    assertTrue(team.isCaptain(member1));

    // Remove member
    assertNotNull(team.removeMember(member1));
    assertFalse(team.hasMember(member1));
    assertEquals(2, team.getSize());
  }

  @Test
  void testTeamBankDepositsAndWithdrawals() {
    UUID leader = UUID.randomUUID();
    Team team = new Team("merchants", "Merchants", leader);

    assertEquals(0.0, team.getBalance());

    team.deposit(100.0);
    assertEquals(100.0, team.getBalance());

    team.deposit(50.50);
    assertEquals(150.50, team.getBalance());

    // Successful withdraw
    assertTrue(team.withdraw(50.0));
    assertEquals(100.50, team.getBalance());

    // Overdraft prevented
    assertFalse(team.withdraw(200.0));
    assertEquals(100.50, team.getBalance());
  }

  @Test
  void testStaticFacadeSafelyHandlesNullAndDisabledState() {
    // When no TeamHelper is active (subsystem disabled or not initialized)
    assertFalse(Teams.isEnabled());

    Player a = null;
    Player b = null;
    assertFalse(Teams.areTeammates(a, b));

    UUID u1 = UUID.randomUUID();
    UUID u2 = UUID.randomUUID();
    assertFalse(Teams.areTeammates(u1, u2));
    assertFalse(Teams.areTeammates(null, u2));
    assertFalse(Teams.areTeammates(u1, null));
    assertFalse(Teams.areTeammates(u1, u1));

    assertTrue(Teams.get(a).isEmpty());
    assertTrue(Teams.get(u1).isEmpty());
    assertTrue(Teams.getByName("dragons").isEmpty());
    assertTrue(Teams.getByName(null).isEmpty());
    assertTrue(Teams.getAll().isEmpty());
    assertFalse(Teams.exists("dragons"));
    assertFalse(Teams.exists(null));

    // Mutating methods return false or null without throwing any exceptions
    assertFalse(Teams.disband("dragons"));
    assertFalse(Teams.removeMember("dragons", (Player) null));
    assertFalse(Teams.removeMember("dragons", (UUID) null));
    assertFalse(Teams.sendTeamChat(null, "hello"));
    assertFalse(Teams.deposit(null, null, 100));
    assertFalse(Teams.withdraw(null, null, 100));
    assertFalse(Teams.teleportHome(null, null));
    assertTrue(Teams.getOnlinePlayers(null).isEmpty());

    // Convenience methods
    assertFalse(Teams.isInTeam((Player) null));
    assertFalse(Teams.isInTeam((UUID) null));
    assertFalse(Teams.isInTeam(u1));
    assertFalse(Teams.join(null, null));
    assertFalse(Teams.leave(null, null));
    assertFalse(Teams.setFriendlyFire(null, true));
    assertFalse(Teams.transferLeadership(null, null));
  }
}
