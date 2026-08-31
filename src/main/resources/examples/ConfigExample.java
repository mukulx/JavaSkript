import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.ScriptConfig;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

@FoliaSupport
public class ConfigExample implements CommandExecutor, TabCompleter {

  private ScriptConfig config;

  public ConfigExample() {
    if (config == null) {
      config = new ScriptConfig(JavaSkriptPlugin.getInstance(), "ConfigExample.java");
    }

    initializeConfigs();
  }

  private void initializeConfigs() {
    // 1. Top-of-file header comments
    config.setHeader(
        "ConfigExample Configuration", "All comments and user modifications are preserved!");

    // 2. Non-destructive defaults with comments
    config.addDefault("enabled", true, "Enable or disable this feature");
    config.addDefault("cooldown", 60, "Cooldown in seconds between uses");

    // 3. Automated version migrations (no data loss)
    config.migrate(
        2,
        c -> {
          c.addDefault("new-feature.multiplier", 1.5, "New multiplier added in v2");
        });

    // 4. Multi-file support
    config.addDefault("messages.yml", "welcome", "Welcome to the server", "Join message");
    config.addDefault("messages.yml", "goodbye", "See you later", "Quit message");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players").color(NamedTextColor.RED));
      return true;
    }

    boolean enabled = config.getBoolean("enabled", true);
    int cooldown = config.getInt("cooldown", 60);
    String welcomeMsg = config.getString("messages.yml", "welcome", "Welcome");

    player.sendMessage(Component.text("Config Values:", NamedTextColor.GOLD));
    player.sendMessage(Component.text("  Enabled: " + enabled, NamedTextColor.YELLOW));
    player.sendMessage(Component.text("  Cooldown: " + cooldown + "s", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("  Message: " + welcomeMsg, NamedTextColor.YELLOW));
    player.sendMessage(
        Component.text(
            "Data folder: " + config.getDataFolder().getAbsolutePath(), NamedTextColor.GRAY));

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    return List.of();
  }
}
