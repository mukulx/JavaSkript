package dev.mukulx.javaskript;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/** Minimal mocked plugin so runtime classes can be exercised without a server. */
public final class TestPlugin {

  private TestPlugin() {}

  public static JavaSkriptPlugin create(File dataFolder) {
    JavaSkriptPlugin plugin = mock(JavaSkriptPlugin.class);
    Server server = mock(Server.class);
    PluginManager pluginManager = mock(PluginManager.class);
    when(plugin.getDataFolder()).thenReturn(dataFolder);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("JavaSkriptTest"));
    when(plugin.getServer()).thenReturn(server);
    when(plugin.getName()).thenReturn("JavaSkript");
    when(plugin.isEnabled()).thenReturn(true);
    when(server.getPluginManager()).thenReturn(pluginManager);
    when(pluginManager.getPlugins()).thenReturn(new Plugin[0]);
    return plugin;
  }
}
