import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.api.gui.ItemBuilder;
import dev.mukulx.javaskript.api.item.ItemHelper;
import dev.mukulx.javaskript.script.FoliaSupport;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * Example demonstrating JavaSkript's powerful, fluent ItemBuilder API.
 *
 * <p>Features: - MiniMessage gradient names & formatted lore - Custom RPG weapons with PDC tags and
 * custom model data - Colored leather armor (RGB) - Custom player heads & skull textures - Custom
 * potions with effects - 100% open extensibility with meta(Class, Consumer) for any Bukkit item
 */
@FoliaSupport
public class ItemBuilderExample {

  private ItemHelper items;
  private CommandHelper commands;

  public void onEnable() {
    commands
        .create("customitem")
        .description("Give yourself custom crafted items")
        .permission("javaskript.items")
        .subcommand(
            "sword",
            sub ->
                sub.executesPlayer(
                    (player, ctx) -> {
                      ItemStack sword = createExcalibur();
                      items.give(player, sword);
                      ctx.replySuccess("You received §6Excalibur§a!");
                    }))
        .subcommand(
            "armor",
            sub ->
                sub.executesPlayer(
                    (player, ctx) -> {
                      ItemStack chestplate = createRoyalChestplate();
                      items.give(player, chestplate);
                      ctx.replySuccess("You received §5Royal Crimson Armor§a!");
                    }))
        .subcommand(
            "potion",
            sub ->
                sub.executesPlayer(
                    (player, ctx) -> {
                      ItemStack potion = createElixirOfSpeed();
                      items.give(player, potion);
                      ctx.replySuccess("You received §bElixir of the Wind§a!");
                    }))
        .subcommand(
            "book",
            sub ->
                sub.executesPlayer(
                    (player, ctx) -> {
                      ItemStack book = createAncientTome();
                      items.give(player, book);
                      ctx.replySuccess("You received §eAncient Spell Tome§a!");
                    }))
        .register();
  }

  /** RPG Legendary Sword with MiniMessage gradient, PDC tag, and glow */
  private ItemStack createExcalibur() {
    return items
        .create(Material.NETHERITE_SWORD)
        .name("<gradient:#ff5555:#ffaa00><bold>EXCALIBUR</bold></gradient>")
        .lore(
            "&7An ancient blade pulled from stone.",
            "",
            "&c+25 Attack Damage",
            "&6Special: &eRight-click to cast lightning",
            "",
            "&4&lLEGENDARY ARTIFACT")
        .enchant(Enchantment.SHARPNESS, 6)
        .enchant(Enchantment.FIRE_ASPECT, 2)
        .unbreakable()
        .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE)
        .pdc("ability", "lightning_strike")
        .pdc("rarity", "legendary")
        .customModelData(1042)
        .glow()
        .build();
  }

  /** Custom dyed leather armor with RGB color */
  private ItemStack createRoyalChestplate() {
    return items
        .create(Material.LEATHER_CHESTPLATE)
        .name("<gradient:#8e2de2:#4a00e0><bold>ROYAL TUNIC</bold></gradient>")
        .lore("&7Woven with enchanted royal silk.", "", "&5+10 Armor Defense")
        .color(140, 20, 220) // Custom RGB purple
        .unbreakable()
        .flags(ItemFlag.HIDE_DYE)
        .build();
  }

  /** Custom potion with custom effects, base type, and potion color */
  private ItemStack createElixirOfSpeed() {
    return ItemBuilder.of(Material.POTION)
        .name("<aqua><bold>ELIXIR OF THE WIND</bold></aqua>")
        .lore("&7Grants supernatural agility for a short time.", "", "&eDuration: &f30 Seconds")
        .potionType(PotionType.SPEED)
        .potionColor(Color.fromRGB(0, 255, 230))
        .addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 2))
        .addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 30, 1))
        .build();
  }

  /** Open extensibility with .meta(Class, Consumer) for specialized Bukkit items like BookMeta */
  private ItemStack createAncientTome() {
    return ItemBuilder.of(Material.WRITTEN_BOOK)
        .name("<gold><bold>ANCIENT SPELL TOME</bold></gold>")
        .lore("&7Contains forbidden knowledge.")
        .meta(
            BookMeta.class,
            book -> {
              book.setTitle("Ancient Arcana");
              book.setAuthor("Archmage Mukul");
              book.addPage("In the beginning, there was only bytecode and void...");
            })
        .glow()
        .build();
  }
}
