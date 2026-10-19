package dev.mukulx.javaskript.api.economy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyHelperTest {

  private EconomyHelper economy;
  private final EconomyProvider custom = mock(EconomyProvider.class);

  @BeforeEach
  void setUp() {
    JavaSkriptPlugin plugin = mock(JavaSkriptPlugin.class);
    when(plugin.getConfig()).thenReturn(new YamlConfiguration());
    when(plugin.getLogger()).thenReturn(Logger.getLogger("EconomyHelperTest"));
    economy = new EconomyHelper(plugin);
  }

  @Test
  void customProviderReplacesADisabledEconomy() {
    assertFalse(economy.isEnabled());

    economy.setProvider(custom);
    UUID player = UUID.randomUUID();
    when(custom.getBalance(player)).thenReturn(7.0);

    assertTrue(economy.isEnabled());
    assertSame(custom, economy.getProvider());
    assertTrue(economy.getBalance(player) == 7.0);

    economy.resetProvider();
    assertFalse(economy.isEnabled());
  }

  @Test
  void reloadKeepsCustomProviderAndCurrencies() {
    economy.setProvider(custom);
    economy.registerCurrency("Gems", custom);

    economy.reload();

    assertSame(custom, economy.getProvider());
    assertTrue(economy.currency("gems").isPresent());
  }

  @Test
  void unloadingScriptReleasesItsProviders() {
    economy.setProvider(custom);
    economy.registerCurrency("gems", custom);

    economy.releaseProvidersFrom(custom.getClass().getClassLoader());

    assertFalse(economy.hasCustomProvider());
    assertTrue(economy.currency("gems").isEmpty());
  }
}
