import dev.mukulx.javaskript.api.RecipeHelper;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class CustomRecipes implements Listener {

  private RecipeHelper recipes;

  public void onEnable() {
    shaped();
    shapeless();
    furnace();
    blasting();
    smoking();
    campfire();
    stonecutting();
    smithing();
  }

  private void shaped() {
    ItemStack result = new ItemStack(Material.DIAMOND, 3);

    recipes
        .shaped("triple_diamond", result)
        .shape("CCC", "CEC", "CCC")
        .ingredient('C', Material.COAL)
        .ingredient('E', Material.EMERALD)
        .group("custom")
        .register();
  }

  private void shapeless() {
    ItemStack result = new ItemStack(Material.GOLDEN_APPLE);

    recipes
        .shapeless("easy_golden_apple", result)
        .add(Material.APPLE)
        .add(Material.GOLD_NUGGET, 4)
        .group("custom")
        .register();
  }

  private void furnace() {
    ItemStack result = new ItemStack(Material.DIAMOND);

    recipes
        .furnace("smelt_diamond", result, Material.COAL_BLOCK)
        .experience(10.0f)
        .cookTime(200)
        .register();
  }

  private void blasting() {
    ItemStack result = new ItemStack(Material.NETHERITE_INGOT);

    recipes
        .blasting("blast_netherite", result, Material.ANCIENT_DEBRIS)
        .experience(2.0f)
        .cookTime(100)
        .register();
  }

  private void smoking() {
    ItemStack result = new ItemStack(Material.COOKED_BEEF, 2);

    recipes
        .smoking("double_beef", result, Material.BEEF)
        .experience(0.35f)
        .cookTime(100)
        .register();
  }

  private void campfire() {
    ItemStack result = new ItemStack(Material.COOKED_CHICKEN, 2);

    recipes
        .campfire("double_chicken", result, Material.CHICKEN)
        .experience(0.35f)
        .cookTime(600)
        .register();
  }

  private void stonecutting() {
    ItemStack result = new ItemStack(Material.STONE_BRICKS, 4);

    recipes.stonecutting("efficient_bricks", result, Material.STONE).group("stonework").register();
  }

  private void smithing() {
    ItemStack result = new ItemStack(Material.NETHERITE_SWORD);

    recipes
        .smithing("custom_upgrade", result)
        .template(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
        .base(Material.DIAMOND_SWORD)
        .addition(Material.NETHERITE_INGOT)
        .register();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    event.getPlayer().discoverRecipes(recipes.getRegisteredRecipes());
  }
}
