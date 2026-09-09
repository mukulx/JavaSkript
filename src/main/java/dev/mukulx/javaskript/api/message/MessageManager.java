package dev.mukulx.javaskript.api.message;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.File;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages plugin and system messages loaded from messages.yml.
 *
 * <p>Supports both MiniMessage tags (<green>, <bold>, <#hex>) and legacy section color codes.
 */
public class MessageManager {

  private final JavaSkriptPlugin plugin;
  private final File messagesFile;
  private FileConfiguration config;
  private final MiniMessage miniMessage;

  public MessageManager(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
    this.miniMessage = MiniMessage.miniMessage();
    load();
  }

  /** Load or reload messages.yml from disk. */
  public synchronized void load() {
    if (!messagesFile.exists()) {
      plugin.saveResource("messages.yml", false);
    }
    this.config = YamlConfiguration.loadConfiguration(messagesFile);
  }

  /** Reload messages.yml. */
  public synchronized void reload() {
    load();
  }

  /** Get raw string message from messages.yml or fallback. */
  public String getRaw(String path, String defaultMessage) {
    if (config == null) {
      return defaultMessage;
    }
    return config.getString(path, defaultMessage);
  }

  /**
   * Parse a message into a Component with placeholder replacement.
   *
   * @param path The YAML path in messages.yml
   * @param defaultMessage Default text if path not found
   * @param replacements Key-value pairs for replacement (e.g. "{player}", "Steve")
   * @return Formatted Component
   */
  public Component get(String path, String defaultMessage, String... replacements) {
    String raw = getRaw(path, defaultMessage);
    if (raw == null) {
      return Component.empty();
    }

    if (replacements != null && replacements.length >= 2) {
      for (int i = 0; i < replacements.length - 1; i += 2) {
        String key = replacements[i];
        String val = replacements[i + 1];
        if (key != null && val != null) {
          raw = raw.replace(key, val);
        }
      }
    }

    return parse(raw);
  }

  /** Parse message with a map of placeholders. */
  public Component get(String path, String defaultMessage, Map<String, String> placeholders) {
    String raw = getRaw(path, defaultMessage);
    if (raw == null) {
      return Component.empty();
    }

    if (placeholders != null) {
      for (Map.Entry<String, String> entry : placeholders.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
          raw = raw.replace(entry.getKey(), entry.getValue());
        }
      }
    }

    return parse(raw);
  }

  /** Send a configured message directly to a CommandSender. */
  public void send(
      CommandSender sender, String path, String defaultMessage, String... replacements) {
    if (sender == null) return;
    Component component = get(path, defaultMessage, replacements);
    sender.sendMessage(component);
  }

  /** Parse MiniMessage or legacy formatting into a Component. */
  public Component parse(String message) {
    if (message == null || message.isEmpty()) {
      return Component.empty();
    }
    // If message contains legacy section codes, translate them first
    if (message.contains("§") || message.contains("&")) {
      message = message.replace('&', '§');
      return LegacyComponentSerializer.legacySection().deserialize(message);
    }
    try {
      return miniMessage.deserialize(message);
    } catch (Exception e) {
      return Component.text(message);
    }
  }
}
