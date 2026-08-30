package dev.mukulx.javaskript.api.command;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface CommandCallback {
  void execute(CommandSender sender, CommandContext ctx) throws Exception;
}
