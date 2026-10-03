package dev.mukulx.javaskript.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class RecipeHelperTest {

  private final JavaSkriptPlugin plugin = mock(JavaSkriptPlugin.class);

  /** A helper that already owns one recipe. Building a real recipe needs a server registry. */
  @SuppressWarnings("unchecked")
  private RecipeHelper helperWithRecipe() throws Exception {
    RecipeHelper helper = new RecipeHelper(plugin, "CustomRecipes.java");
    var field = RecipeHelper.class.getDeclaredField("registeredRecipes");
    field.setAccessible(true);
    ((List<NamespacedKey>) field.get(helper))
        .add(new NamespacedKey("javaskript", "customrecipes_x"));
    return helper;
  }

  @Test
  void removeAllClearsTheRecipesFromOnlinePlayersRecipeBooks() throws Exception {
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      RecipeHelper helper = helperWithRecipe();
      Player player = mock(Player.class);
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.<Player>of(player));
      bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(player)).thenReturn(true);
      bukkit.when(() -> Bukkit.removeRecipe(any(NamespacedKey.class))).thenReturn(true);

      helper.removeAll();

      verify(player)
          .undiscoverRecipes(
              org.mockito.ArgumentMatchers.<Collection<NamespacedKey>>argThat(
                  keys -> keys.size() == 1));
    }
  }

  @Test
  void removeAllWithNoRecipesTouchesNoPlayers() {
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      Player player = mock(Player.class);
      bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.<Player>of(player));

      new RecipeHelper(plugin, "Empty.java").removeAll();

      verify(player, never()).undiscoverRecipes(any(Collection.class));
    }
  }
}
