package dev.mukulx.javaskript.api.team.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.team.Team;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TeamStorageTest {

  @TempDir Path temp;

  @Test
  void closeWritesEveryQueuedSave() {
    JavaSkriptPlugin plugin = mock(JavaSkriptPlugin.class);
    when(plugin.getDataFolder()).thenReturn(temp.toFile());
    when(plugin.getLogger()).thenReturn(Logger.getLogger("TeamStorageTest"));

    TeamStorage storage = new TeamStorage(plugin);
    for (int i = 0; i < 50; i++) {
      storage.saveTeamAsync(new Team("team" + i, "Team " + i, UUID.randomUUID()));
    }
    storage.close();

    TeamStorage reopened = new TeamStorage(plugin);
    assertEquals(50, reopened.loadAll().size());
    reopened.close();
  }

  @Test
  void metadataSurvivesARestart() {
    JavaSkriptPlugin plugin = mock(JavaSkriptPlugin.class);
    when(plugin.getDataFolder()).thenReturn(temp.toFile());
    when(plugin.getLogger()).thenReturn(Logger.getLogger("TeamStorageTest"));

    TeamStorage storage = new TeamStorage(plugin);
    Team team = new Team("dragons", "The Dragons", UUID.randomUUID());
    team.setMetadata("kills", 42);
    storage.saveTeamSync(team);
    team.removeMetadata("kills");
    team.setMetadata("motto", "fly");
    storage.saveTeamSync(team);
    storage.close();

    TeamStorage reopened = new TeamStorage(plugin);
    Team loaded = reopened.loadAll().get("dragons");
    assertEquals(Map.of("motto", "fly"), loaded.getMetadata());
    reopened.close();
  }
}
