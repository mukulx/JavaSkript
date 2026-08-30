package dev.mukulx.javaskript.api.command;

import org.bukkit.command.ConsoleCommandSender;

@FunctionalInterface
public interface ConsoleCallback {
  void execute(ConsoleCommandSender console, CommandContext ctx) throws Exception;
}
