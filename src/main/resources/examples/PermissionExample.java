// Permission Example - Shows how to register and use permissions dynamically
// This demonstrates LuckPerms-style permission registration

import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.permissions.PermissionDefault;

@FoliaSupport
public class PermissionExample implements Listener, CommandExecutor, TabCompleter {

  private static final String PERM_BASE = "example";
  private static final String PERM_ADMIN = PERM_BASE + ".admin";
  private static final String PERM_MODERATOR = PERM_BASE + ".moderator";
  private static final String PERM_VIP = PERM_BASE + ".vip";
  private static final String PERM_WILDCARD = PERM_BASE + ".*";

  @EventHandler
  public void onPluginEnable(PluginEnableEvent event) {
    if (event == null || event.getPlugin() == null) return;

    // Only register permissions when JavaSkript loads
    if (!event.getPlugin().getName().equals("JavaSkript")) return;

    var plugin = (dev.mukulx.javaskript.JavaSkriptPlugin) event.getPlugin();
    var permRegistry = plugin.getPermissionRegistry();

    // Register individual permissions
    permRegistry.registerPermission(PERM_ADMIN, "Grants admin access", PermissionDefault.OP);

    permRegistry.registerPermission(
        PERM_MODERATOR, "Grants moderator access", PermissionDefault.OP);

    permRegistry.registerPermission(PERM_VIP, "Grants VIP access", PermissionDefault.FALSE);

    // Register wildcard permission with children
    Map<String, Boolean> children = new HashMap<>();
    children.put(PERM_ADMIN, true);
    children.put(PERM_MODERATOR, true);
    children.put(PERM_VIP, true);

    permRegistry.registerPermissionWithChildren(
        PERM_WILDCARD, "Grants all example permissions", PermissionDefault.OP, children);

    System.out.println("[PermissionExample] Registered permissions!");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players can use this!").color(NamedTextColor.RED));
      return true;
    }

    // Check permissions
    boolean isAdmin = player.hasPermission(PERM_ADMIN);
    boolean isModerator = player.hasPermission(PERM_MODERATOR);
    boolean isVIP = player.hasPermission(PERM_VIP);

    player.sendMessage(Component.text("Your Permissions:", NamedTextColor.GOLD));
    player.sendMessage(
        Component.text("  Admin: ", NamedTextColor.YELLOW)
            .append(
                Component.text(
                    isAdmin ? "✓" : "✗", isAdmin ? NamedTextColor.GREEN : NamedTextColor.RED)));
    player.sendMessage(
        Component.text("  Moderator: ", NamedTextColor.YELLOW)
            .append(
                Component.text(
                    isModerator ? "✓" : "✗",
                    isModerator ? NamedTextColor.GREEN : NamedTextColor.RED)));
    player.sendMessage(
        Component.text("  VIP: ", NamedTextColor.YELLOW)
            .append(
                Component.text(
                    isVIP ? "✓" : "✗", isVIP ? NamedTextColor.GREEN : NamedTextColor.RED)));

    return true;
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    return List.of();
  }
}
