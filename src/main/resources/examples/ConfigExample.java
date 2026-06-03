import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.ScriptConfig;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
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
    FileConfiguration mainConfig = config.getConfig("config.yml");
    if (!mainConfig.contains("enabled")) {
      mainConfig.set("enabled", true);
      mainConfig.set("cooldown", 60);
      config.saveConfig("config.yml", mainConfig);
    }

    FileConfiguration messages = config.getConfig("messages.yml");
    if (!messages.contains("welcome")) {
      messages.set("welcome", "Welcome to the server");
      messages.set("goodbye", "See you later");
      messages.set("error", "An error occurred");
      config.saveConfig("messages.yml", messages);
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players").color(NamedTextColor.RED));
      return true;
    }

    boolean enabled = config.getBoolean("config.yml", "enabled", true);
    int cooldown = config.getInt("config.yml", "cooldown", 60);
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
