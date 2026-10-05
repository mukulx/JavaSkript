package dev.mukulx.javaskript.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigMigratorTest {

  private static final Logger LOG = Logger.getLogger("ConfigMigratorTest");

  @TempDir Path temp;
  private File file;
  private String bundled;

  @BeforeEach
  void setUp() throws Exception {
    file = temp.resolve("config.yml").toFile();
    try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
      bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private YamlConfiguration migrate(String oldYaml) throws Exception {
    Files.writeString(file.toPath(), oldYaml);
    ConfigMigrator.migrate(file, bundled, LOG);
    return YamlConfiguration.loadConfiguration(file);
  }

  @Test
  void bundledConfigIsAtTheCurrentVersion() throws Exception {
    assertEquals(
        ConfigMigrator.CURRENT_VERSION,
        YamlConfiguration.loadConfiguration(new java.io.StringReader(bundled))
            .getInt("config-version"));
  }

  @Test
  void v1LayoutMovesToTheNewPathsKeepingUserValues() throws Exception {
    YamlConfiguration c =
        migrate(
            """
            file-watcher:
              enabled: false
              reload-delay: 900
            debug:
              enabled: true
            errors:
              clean-stack-traces: false
            economy:
              enabled: true
              mode: builtin
              currency:
                symbol: "€"
            teams:
              defaults:
                max-size: 8
              tab:
                player-format: "[{team}] {player}"
            """);

    assertFalse(c.getBoolean("scripts.hot-reload.enabled"));
    assertEquals(900, c.getInt("scripts.hot-reload.delay-ms"));
    assertTrue(c.getBoolean("general.debug"));
    assertFalse(c.getBoolean("general.clean-stack-traces"));
    assertTrue(c.getBoolean("modules.economy.enabled"));
    assertEquals("builtin", c.getString("modules.economy.mode"));
    assertEquals("€", c.getString("modules.economy.currency.symbol"));
    assertEquals(8, c.getInt("modules.teams.defaults.max-size"));
    assertEquals("[{team}] {player}", c.getString("modules.teams.tab.player-format"));
    // keys the old file lacked come from the defaults
    assertEquals("Coins", c.getString("modules.economy.currency.name-plural"));
    assertEquals(ConfigMigrator.CURRENT_VERSION, c.getInt("config-version"));
    assertNull(c.get("file-watcher"));
    assertNull(c.get("economy"));
    assertTrue(new File(temp.toFile(), "config.yml.bak").exists());
  }

  @Test
  void scoreboardBooleansBecomeOneSetting() throws Exception {
    assertEquals(
        "never",
        migrate("teams:\n  tab:\n    sync-vanilla-scoreboard: false\n")
            .getString("modules.teams.tab.vanilla-scoreboard"));
    assertEquals(
        "always",
        migrate("teams:\n  tab:\n    force-vanilla-with-tab: true\n")
            .getString("modules.teams.tab.vanilla-scoreboard"));
    assertEquals(
        "auto",
        migrate("teams:\n  enabled: true\n").getString("modules.teams.tab.vanilla-scoreboard"));
  }

  @Test
  void rewrittenFileKeepsTheBundledComments() throws Exception {
    migrate("debug:\n  enabled: true\n");
    assertTrue(Files.readString(file.toPath()).contains("wait after the last save"));
  }

  @Test
  void currentConfigIsLeftUntouched() throws Exception {
    Files.writeString(file.toPath(), bundled);
    long before = file.lastModified();

    assertFalse(ConfigMigrator.migrate(file, bundled, LOG));
    assertEquals(bundled, Files.readString(file.toPath()));
    assertEquals(before, file.lastModified());
    assertFalse(new File(temp.toFile(), "config.yml.bak").exists());
  }

  @Test
  void missingKeysAreFilledWithoutChangingTheRest() throws Exception {
    YamlConfiguration c =
        migrate(
            "config-version: " + ConfigMigrator.CURRENT_VERSION + "\ngeneral:\n  debug: true\n");

    assertTrue(c.getBoolean("general.debug"));
    assertEquals(500, c.getInt("scripts.hot-reload.delay-ms"));
  }

  @Test
  void wrongTypeFallsBackToTheDefault() throws Exception {
    YamlConfiguration c = migrate("debug:\n  enabled: banana\n");
    assertFalse(c.getBoolean("general.debug"));
  }

  @Test
  void newerConfigIsNotTouched() throws Exception {
    Files.writeString(file.toPath(), "config-version: 99\nfoo: bar\n");
    assertFalse(ConfigMigrator.migrate(file, bundled, LOG));
    assertEquals("config-version: 99\nfoo: bar\n", Files.readString(file.toPath()));
  }
}
