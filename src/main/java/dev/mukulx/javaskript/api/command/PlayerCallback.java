package dev.mukulx.javaskript.api.command;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerCallback {
  void execute(Player player, CommandContext ctx) throws Exception;
}
