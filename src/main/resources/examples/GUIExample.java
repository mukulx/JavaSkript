import dev.mukulx.javaskript.api.gui.GUI;
import dev.mukulx.javaskript.api.gui.ItemBuilder;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@FoliaSupport
public class GUIExample implements CommandExecutor, TabCompleter {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Component.text("Only players").color(NamedTextColor.RED));
      return true;
    }

    if (args.length > 0) {
      switch (args[0].toLowerCase()) {
        case "small" -> openSmallGUI(player);
        case "large" -> openLargeGUI(player);
        case "shop" -> openShopGUI(player);
        default -> openMainGUI(player);
      }
    } else {
      openMainGUI(player);
    }

    return true;
  }

  private void openMainGUI(Player player) {
    GUI gui = new GUI(Component.text("GUI Examples", NamedTextColor.GOLD), 3);

    var smallGUI =
        new ItemBuilder(Material.CHEST)
            .name(Component.text("Small GUI (3 rows)", NamedTextColor.GREEN))
            .lore("Click to open")
            .build();

    var largeGUI =
        new ItemBuilder(Material.ENDER_CHEST)
            .name(Component.text("Large GUI (6 rows)", NamedTextColor.AQUA))
            .lore("Click to open")
            .build();

    var shopGUI =
        new ItemBuilder(Material.EMERALD)
            .name(Component.text("Shop GUI", NamedTextColor.YELLOW))
            .lore("Click to open")
            .build();

    gui.setItem(11, smallGUI, e -> openSmallGUI(player));
    gui.setItem(13, largeGUI, e -> openLargeGUI(player));
    gui.setItem(15, shopGUI, e -> openShopGUI(player));

    gui.fillBorder(
        new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build());

    gui.open(player);
  }

  private void openSmallGUI(Player player) {
    GUI gui = new GUI("Small GUI", 3);

    for (int i = 0; i < 9; i++) {
      var item =
          new ItemBuilder(Material.PAPER)
              .name(Component.text("Slot " + i, NamedTextColor.WHITE))
              .build();

      gui.setItem(
          i + 9,
          item,
          e -> {
            player.sendMessage("You clicked slot " + e.getSlot());
          });
    }

    var back =
        new ItemBuilder(Material.ARROW).name(Component.text("Back", NamedTextColor.RED)).build();

    gui.setItem(22, back, e -> openMainGUI(player));

    gui.open(player);
  }

  private void openLargeGUI(Player player) {
    GUI gui = new GUI("Large GUI", 6);

    for (int i = 0; i < 54; i++) {
      if (i % 9 == 0 || i % 9 == 8 || i < 9 || i >= 45) {
        continue;
      }

      var item =
          new ItemBuilder(Material.STONE)
              .name(Component.text("Item " + i, NamedTextColor.GRAY))
              .build();

      gui.setItem(i, item);
    }

    gui.fillBorder(
        new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(Component.text(" ")).build());

    var back =
        new ItemBuilder(Material.ARROW).name(Component.text("Back", NamedTextColor.RED)).build();

    gui.setItem(49, back, e -> openMainGUI(player));

    gui.open(player);
  }

  private void openShopGUI(Player player) {
    GUI gui = new GUI(Component.text("Shop", NamedTextColor.GOLD), 4);

    var diamond =
        new ItemBuilder(Material.DIAMOND)
            .name(Component.text("Diamond", NamedTextColor.AQUA))
            .lore("Price: 100 coins", "Click to buy")
            .glow()
            .build();

    var gold =
        new ItemBuilder(Material.GOLD_INGOT)
            .name(Component.text("Gold Ingot", NamedTextColor.GOLD))
            .lore("Price: 50 coins", "Click to buy")
            .amount(5)
            .build();

    var iron =
        new ItemBuilder(Material.IRON_INGOT)
            .name(Component.text("Iron Ingot", NamedTextColor.WHITE))
            .lore("Price: 25 coins", "Click to buy")
            .amount(10)
            .build();

    gui.setItem(
        11,
        diamond,
        e -> {
          player.getInventory().addItem(new ItemStack(Material.DIAMOND));
          player.sendMessage(Component.text("Purchased diamond").color(NamedTextColor.GREEN));
          player.closeInventory();
        });

    gui.setItem(
        13,
        gold,
        e -> {
          player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5));
          player.sendMessage(Component.text("Purchased 5 gold").color(NamedTextColor.GREEN));
          player.closeInventory();
        });

    gui.setItem(
        15,
        iron,
        e -> {
          player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 10));
          player.sendMessage(Component.text("Purchased 10 iron").color(NamedTextColor.GREEN));
          player.closeInventory();
        });

    gui.fillBorder(
        new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.text(" ")).build());

    var back =
        new ItemBuilder(Material.ARROW).name(Component.text("Back", NamedTextColor.RED)).build();

    gui.setItem(31, back, e -> openMainGUI(player));

    gui.onClose(
        e -> {
          player.sendMessage(Component.text("Thanks for shopping", NamedTextColor.GRAY));
        });

    gui.open(player);
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return List.of("small", "large", "shop");
    }
    return List.of();
  }
}
