package dev.mukulx.javaskript.api.command;

import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Represents a typed, validated command argument with automatic tab-completion.
 *
 * @param <T> The parsed type
 */
public interface CommandArgument<T> {

  /** Name of the argument (e.g. "target", "amount"). */
  String getName();

  /** Whether this argument is optional. */
  boolean isOptional();

  /** Whether this argument consumes all remaining input words. */
  default boolean isGreedy() {
    return false;
  }

  /**
   * Parse the argument value from raw string input.
   *
   * @param sender The command sender
   * @param input Raw argument string
   * @return Parsed object of type T
   * @throws CommandArgumentException If parsing fails (validation error)
   */
  T parse(CommandSender sender, String input) throws CommandArgumentException;

  /**
   * Provide tab completion suggestions for this argument.
   *
   * @param sender The command sender
   * @param input The current partial input
   * @return List of suggestions
   */
  List<String> tabComplete(CommandSender sender, String input);
}
