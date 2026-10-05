package dev.mukulx.javaskript.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Brings an existing config.yml up to the layout and defaults of the bundled one.
 *
 * <p>To change the config in a later release: edit the bundled config.yml, append a step to {@link
 * #STEPS} that moves or converts the old keys, and bump {@code config-version} in the bundled file
 * to {@code CURRENT_VERSION}. Adding a plain new key needs no step; it is filled in from the
 * defaults. The old file is kept as config.yml.bak whenever it is rewritten.
 */
public final class ConfigMigrator {

  private static final String VERSION_KEY = "config-version";

  /** Step {@code i} converts a version {@code i + 1} config to version {@code i + 2}. */
  private static final List<Consumer<YamlConfiguration>> STEPS = List.of(ConfigMigrator::v1ToV2);

  public static final int CURRENT_VERSION = STEPS.size() + 1;

  private ConfigMigrator() {}

  /**
   * Rewrites {@code file} if it is from an older version or lacks keys the defaults have.
   *
   * @param bundled the config.yml shipped in the jar, used for new keys and comments
   * @return true if the file was rewritten
   */
  public static boolean migrate(File file, String bundled, Logger log) throws IOException {
    if (!file.exists()) {
      return false;
    }
    YamlConfiguration old = YamlConfiguration.loadConfiguration(file);
    YamlConfiguration fresh = load(bundled);

    int version = old.getInt(VERSION_KEY, 1);
    if (version > CURRENT_VERSION) {
      log.warning("config.yml is from a newer JavaSkript (v" + version + "); leaving it alone");
      return false;
    }
    for (int v = version; v < CURRENT_VERSION; v++) {
      STEPS.get(v - 1).accept(old);
    }

    boolean missing =
        leaves(fresh).stream().anyMatch(key -> !old.contains(key) && !key.equals(VERSION_KEY));
    if (version == CURRENT_VERSION && !missing) {
      return false;
    }

    Files.copy(
        file.toPath(),
        new File(file.getParentFile(), "config.yml.bak").toPath(),
        StandardCopyOption.REPLACE_EXISTING);

    for (String key : leaves(fresh)) {
      Object value = old.get(key);
      if (value == null || key.equals(VERSION_KEY)) {
        continue;
      }
      if (!sameKind(fresh.get(key), value)) {
        log.warning("config.yml: '" + key + "' has the wrong type, using the default");
        continue;
      }
      fresh.set(key, value);
    }
    for (String key : leaves(old)) {
      if (!fresh.contains(key)) {
        log.warning("config.yml: unknown key '" + key + "' removed (see config.yml.bak)");
      }
    }
    fresh.set(VERSION_KEY, CURRENT_VERSION);
    fresh.save(file);
    log.info("Updated config.yml from v" + version + " to v" + CURRENT_VERSION);
    return true;
  }

  private static YamlConfiguration load(String text) throws IOException {
    YamlConfiguration yaml = new YamlConfiguration();
    try {
      yaml.loadFromString(text);
    } catch (InvalidConfigurationException e) {
      throw new IOException("Bundled config.yml is invalid", e);
    }
    return yaml;
  }

  private static List<String> leaves(YamlConfiguration yaml) {
    return yaml.getKeys(true).stream().filter(key -> !yaml.isConfigurationSection(key)).toList();
  }

  private static boolean sameKind(Object expected, Object actual) {
    if (expected instanceof Number) {
      return actual instanceof Number;
    }
    return expected == null || expected.getClass().isInstance(actual);
  }

  // ── Steps ──────────────────────────────────────────────

  /** 1 → 2: single-key sections folded into general/scripts, subsystems moved under modules. */
  private static void v1ToV2(YamlConfiguration c) {
    move(c, "debug.enabled", "general.debug");
    move(c, "update-checker.enabled", "general.update-checker");
    move(c, "errors.clean-stack-traces", "general.clean-stack-traces");
    move(c, "file-watcher.enabled", "scripts.hot-reload.enabled");
    move(c, "file-watcher.reload-delay", "scripts.hot-reload.delay-ms");

    boolean sync = c.getBoolean("teams.tab.sync-vanilla-scoreboard", true);
    boolean force = c.getBoolean("teams.tab.force-vanilla-with-tab", false);
    if (c.contains("teams.tab.sync-vanilla-scoreboard")
        || c.contains("teams.tab.force-vanilla-with-tab")) {
      c.set("teams.tab.vanilla-scoreboard", !sync ? "never" : force ? "always" : "auto");
    }
    c.set("teams.tab.sync-vanilla-scoreboard", null);
    c.set("teams.tab.force-vanilla-with-tab", null);

    moveSection(c, "economy", "modules.economy");
    moveSection(c, "teams", "modules.teams");
  }

  private static void move(YamlConfiguration c, String from, String to) {
    if (c.contains(from)) {
      c.set(to, c.get(from));
      c.set(from, null);
    }
  }

  private static void moveSection(YamlConfiguration c, String from, String to) {
    var section = c.getConfigurationSection(from);
    if (section == null) {
      return;
    }
    for (String key : section.getKeys(true)) {
      if (!section.isConfigurationSection(key)) {
        c.set(to + "." + key, section.get(key));
      }
    }
    c.set(from, null);
  }
}
