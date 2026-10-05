package dev.mukulx.javaskript.command.sub;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import org.bukkit.command.CommandSender;

/** Alias for {@code /js profile top}. */
public class TimingsSubCommand extends SubCommand {

  private final ProfileSubCommand profile;

  public TimingsSubCommand(JavaSkriptPlugin plugin) {
    super(plugin);
    this.profile = new ProfileSubCommand(plugin);
  }

  @Override
  public void execute(CommandSender sender, String[] args) {
    profile.execute(sender, new String[] {"profile", "top"});
  }
}
