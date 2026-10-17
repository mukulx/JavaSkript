import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.api.resourcepack.ResourcePack;
import dev.mukulx.javaskript.api.resourcepack.ResourcePackHelper;
import dev.mukulx.javaskript.script.FoliaSupport;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Example of the Resource Pack API: a pack with a custom item, built entirely in code.
 *
 * <p>The texture is drawn here so the example needs no files. Real packs usually load PNGs, models
 * and sounds from plugins/JavaSkript/resourcepacks/, e.g. {@code .itemModel("ruby",
 * Path.of("ruby.png"))} or a whole folder with {@code .folder("my-pack")}.
 *
 * <p>Players receive the pack when they join. Use /ruby to get the custom item.
 */
@FoliaSupport
public class ResourcePackExample {

  private ResourcePackHelper resourcePacks;
  private CommandHelper commands;
  private ResourcePack pack;

  public void onEnable() throws IOException {
    pack =
        resourcePacks
            .create("main")
            .description("Ruby example")
            .itemModel("ruby", rubyTexture())
            .onLoaded(
                player ->
                    player.sendMessage(Component.text("Ruby pack loaded!", NamedTextColor.GREEN)))
            .register();

    commands
        .create("ruby")
        .description("Get a ruby from the example resource pack")
        .executesPlayer(
            (player, ctx) -> {
              ItemStack ruby = pack.applyModel(new ItemStack(Material.EMERALD), "ruby");
              ruby.editMeta(meta -> meta.itemName(Component.text("Ruby", NamedTextColor.RED)));
              player.getInventory().addItem(ruby);
            });
  }

  /** A 16x16 red gem. */
  private static byte[] rubyTexture() throws IOException {
    BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    Color dark = new Color(140, 10, 30);
    Color light = new Color(235, 60, 80);
    for (int y = 3; y < 13; y++) {
      int half = y < 8 ? y - 2 : 13 - y;
      for (int x = 8 - half; x < 8 + half; x++) {
        image.setRGB(x, y, (x + y) % 3 == 0 ? light.getRGB() : dark.getRGB());
      }
    }
    ByteArrayOutputStream png = new ByteArrayOutputStream();
    ImageIO.write(image, "png", png);
    return png.toByteArray();
  }
}
