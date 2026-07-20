package dev.mukulx.javaskript.api;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;

public class RecipeHelper {

  private final JavaSkriptPlugin plugin;
  private final String scriptName;
  private final List<NamespacedKey> registeredRecipes;

  public RecipeHelper(JavaSkriptPlugin plugin, String scriptName) {
    this.plugin = plugin;
    this.scriptName = scriptName.replace(".java", "").toLowerCase();
    this.registeredRecipes = new ArrayList<>();
  }

  public ShapedBuilder shaped(String key, ItemStack result) {
    return new ShapedBuilder(createKey(key), result);
  }

  public ShapelessBuilder shapeless(String key, ItemStack result) {
    return new ShapelessBuilder(createKey(key), result);
  }

  public FurnaceBuilder furnace(String key, ItemStack result, Material source) {
    return new FurnaceBuilder(createKey(key), result, source);
  }

  public BlastingBuilder blasting(String key, ItemStack result, Material source) {
    return new BlastingBuilder(createKey(key), result, source);
  }

  public SmokingBuilder smoking(String key, ItemStack result, Material source) {
    return new SmokingBuilder(createKey(key), result, source);
  }

  public CampfireBuilder campfire(String key, ItemStack result, Material source) {
    return new CampfireBuilder(createKey(key), result, source);
  }

  public StonecuttingBuilder stonecutting(String key, ItemStack result, Material source) {
    return new StonecuttingBuilder(createKey(key), result, source);
  }

  public SmithingBuilder smithing(String key, ItemStack result) {
    return new SmithingBuilder(createKey(key), result);
  }

  private NamespacedKey createKey(String key) {
    return new NamespacedKey(plugin, scriptName + "_" + key);
  }

  private boolean register(Recipe recipe, NamespacedKey key) {
    // If not on main thread, schedule to main thread and wait
    if (!Bukkit.isPrimaryThread()) {
      final boolean[] result = {false};
      try {
        Bukkit.getScheduler()
            .callSyncMethod(
                plugin,
                () -> {
                  result[0] = registerSync(recipe, key);
                  return result[0];
                })
            .get();
        return result[0];
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning("Failed to register recipe " + key + " (async): " + e.getMessage());
        return false;
      }
    }

    return registerSync(recipe, key);
  }

  private boolean registerSync(Recipe recipe, NamespacedKey key) {
    try {
      Bukkit.addRecipe(recipe);
      registeredRecipes.add(key);
      plugin.debug("Registered recipe: " + key);
      return true;
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to register recipe " + key + ": " + e.getMessage());
      return false;
    }
  }

  public boolean remove(String key) {
    NamespacedKey namespacedKey = createKey(key);
    boolean removed = Bukkit.removeRecipe(namespacedKey);
    if (removed) {
      registeredRecipes.remove(namespacedKey);
      plugin.debug("Removed recipe: " + namespacedKey);
    }
    return removed;
  }

  public void removeAll() {
    for (NamespacedKey key : new ArrayList<>(registeredRecipes)) {
      Bukkit.removeRecipe(key);
    }
    registeredRecipes.clear();
    plugin.debug("Removed all recipes for script: " + scriptName);
  }

  public List<NamespacedKey> getRegisteredRecipes() {
    return new ArrayList<>(registeredRecipes);
  }

  public class ShapedBuilder {
    private final NamespacedKey key;
    private final ShapedRecipe recipe;
    private String[] shape;

    public ShapedBuilder(NamespacedKey key, ItemStack result) {
      this.key = key;
      this.recipe = new ShapedRecipe(key, result);
    }

    public ShapedBuilder shape(String... rows) {
      if (rows.length < 1 || rows.length > 3) {
        throw new IllegalArgumentException("Shape must have 1-3 rows");
      }
      for (String row : rows) {
        if (row.length() < 1 || row.length() > 3) {
          throw new IllegalArgumentException("Each row must have 1-3 characters");
        }
      }
      this.shape = rows;
      recipe.shape(rows);
      return this;
    }

    public ShapedBuilder ingredient(char symbol, Material material) {
      recipe.setIngredient(symbol, material);
      return this;
    }

    public ShapedBuilder ingredient(char symbol, RecipeChoice choice) {
      recipe.setIngredient(symbol, choice);
      return this;
    }

    public ShapedBuilder group(String group) {
      recipe.setGroup(group);
      return this;
    }

    public ShapedBuilder category(org.bukkit.inventory.recipe.CraftingBookCategory category) {
      recipe.setCategory(category);
      return this;
    }

    public boolean register() {
      if (shape == null) {
        throw new IllegalStateException("Shape not set! Use .shape() first");
      }
      return RecipeHelper.this.register(recipe, key);
    }
  }

  public class ShapelessBuilder {
    private final NamespacedKey key;
    private final ShapelessRecipe recipe;

    public ShapelessBuilder(NamespacedKey key, ItemStack result) {
      this.key = key;
      this.recipe = new ShapelessRecipe(key, result);
    }

    public ShapelessBuilder add(Material material) {
      recipe.addIngredient(material);
      return this;
    }

    public ShapelessBuilder add(Material material, int count) {
      recipe.addIngredient(count, material);
      return this;
    }

    public ShapelessBuilder add(RecipeChoice choice) {
      recipe.addIngredient(choice);
      return this;
    }

    public ShapelessBuilder group(String group) {
      recipe.setGroup(group);
      return this;
    }

    public ShapelessBuilder category(org.bukkit.inventory.recipe.CraftingBookCategory category) {
      recipe.setCategory(category);
      return this;
    }

    public boolean register() {
      return RecipeHelper.this.register(recipe, key);
    }
  }

  public class FurnaceBuilder {
    private final NamespacedKey key;
    private final FurnaceRecipe recipe;

    public FurnaceBuilder(NamespacedKey key, ItemStack result, Material source) {
      this.key = key;
      this.recipe = new FurnaceRecipe(key, result, source, 0.1f, 200);
    }

    public FurnaceBuilder experience(float exp) {
      recipe.setExperience(exp);
      return this;
    }

    public FurnaceBuilder cookTime(int ticks) {
      recipe.setCookingTime(ticks);
      return this;
    }

    public FurnaceBuilder group(String group) {
      recipe.setGroup(group);
      return this;
    }

    public FurnaceBuilder category(org.bukkit.inventory.recipe.CookingBookCategory category) {
      recipe.setCategory(category);
      return this;
    }

    public boolean register() {
      return RecipeHelper.this.register(recipe, key);
    }
  }

  public class BlastingBuilder {
    private final NamespacedKey key;
    private final BlastingRecipe recipe;

    public BlastingBuilder(NamespacedKey key, ItemStack result, Material source) {
      this.key = key;
      this.recipe = new BlastingRecipe(key, result, source, 0.1f, 100);
    }

    public BlastingBuilder experience(float exp) {
      recipe.setExperience(exp);
      return this;
    }

    public BlastingBuilder cookTime(int ticks) {
      recipe.setCookingTime(ticks);
      return this;
    }

    public BlastingBuilder group(String group) {
      recipe.setGroup(group);
      return this;
    }

    public BlastingBuilder category(org.bukkit.inventory.recipe.CookingBookCategory category) {
      recipe.setCategory(category);
      return this;
    }

    public boolean register() {
      return RecipeHelper.this.register(recipe, key);
    }
  }

  public class SmokingBuilder {
    private final NamespacedKey key;
    private final SmokingRecipe recipe;

    public SmokingBuilder(NamespacedKey key, ItemStack result, Material source) {
      this.key = key;
      this.recipe = new SmokingRecipe(key, result, source, 0.1f, 100);
    }

    public SmokingBuilder experience(float exp) {
      recipe.setExperience(exp);
      return this;
    }

    public SmokingBuilder cookTime(int ticks) {
      recipe.setCookingTime(ticks);
      return this;
    }

    public SmokingBuilder group(String group) {
      recipe.setGroup(group);
      return this;
    }

    public SmokingBuilder category(org.bukkit.inventory.recipe.CookingBookCategory category) {
      recipe.setCategory(category);
      return this;
    }

    public boolean register() {
      return RecipeHelper.this.register(recipe, key);
    }
  }

  public class CampfireBuilder {
    private final NamespacedKey key;
    private final CampfireRecipe recipe;

    public CampfireBuilder(NamespacedKey key, ItemStack result, Material source) {
      this.key = key;
      this.recipe = new CampfireRecipe(key, result, source, 0.1f, 600);
    }

    public CampfireBuilder experience(float exp) {
      recipe.setExperience(exp);
      return this;
    }

    public CampfireBuilder cookTime(int ticks) {
      recipe.setCookingTime(ticks);
      return this;
    }

    public CampfireBuilder group(String group) {
      recipe.setGroup(group);
      return this;
    }

    public CampfireBuilder category(org.bukkit.inventory.recipe.CookingBookCategory category) {
      recipe.setCategory(category);
      return this;
    }

    public boolean register() {
      return RecipeHelper.this.register(recipe, key);
    }
  }

  public class StonecuttingBuilder {
    private final NamespacedKey key;
    private final StonecuttingRecipe recipe;

    public StonecuttingBuilder(NamespacedKey key, ItemStack result, Material source) {
      this.key = key;
      this.recipe = new StonecuttingRecipe(key, result, source);
    }

    public StonecuttingBuilder group(String group) {
      recipe.setGroup(group);
      return this;
    }

    public boolean register() {
      return RecipeHelper.this.register(recipe, key);
    }
  }

  public class SmithingBuilder {
    private final NamespacedKey key;
    private final ItemStack result;
    private RecipeChoice template;
    private RecipeChoice base;
    private RecipeChoice addition;

    public SmithingBuilder(NamespacedKey key, ItemStack result) {
      this.key = key;
      this.result = result;
    }

    public SmithingBuilder template(Material material) {
      this.template = new RecipeChoice.MaterialChoice(material);
      return this;
    }

    public SmithingBuilder template(RecipeChoice choice) {
      this.template = choice;
      return this;
    }

    public SmithingBuilder base(Material material) {
      this.base = new RecipeChoice.MaterialChoice(material);
      return this;
    }

    public SmithingBuilder base(RecipeChoice choice) {
      this.base = choice;
      return this;
    }

    public SmithingBuilder addition(Material material) {
      this.addition = new RecipeChoice.MaterialChoice(material);
      return this;
    }

    public SmithingBuilder addition(RecipeChoice choice) {
      this.addition = choice;
      return this;
    }

    public boolean register() {
      if (template == null || base == null || addition == null) {
        throw new IllegalStateException("Template, base, and addition must all be set");
      }
      SmithingTransformRecipe recipe =
          new SmithingTransformRecipe(key, result, template, base, addition);
      return RecipeHelper.this.register(recipe, key);
    }
  }
}
