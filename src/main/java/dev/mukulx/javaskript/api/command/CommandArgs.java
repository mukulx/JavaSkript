package dev.mukulx.javaskript.api.command;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Factory class for creating command arguments with built-in validation and tab-completion. */
public final class CommandArgs {

  private CommandArgs() {}

  // ==========================================
  // String Arguments
  // ==========================================

  public static CommandArgument<String> string(String name) {
    return new SimpleArgument<>(name, false, false, (s, in) -> in, (s, in) -> List.of());
  }

  public static CommandArgument<String> greedyString(String name) {
    return new SimpleArgument<>(name, false, true, (s, in) -> in, (s, in) -> List.of());
  }

  // ==========================================
  // Number Arguments
  // ==========================================

  public static CommandArgument<Integer> integer(String name) {
    return integer(name, Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  public static CommandArgument<Integer> integer(String name, int min, int max) {
    return new SimpleArgument<>(
        name,
        false,
        false,
        (s, in) -> {
          try {
            int val = Integer.parseInt(in);
            if (val < min || val > max) {
              throw new CommandArgumentException(
                  "Argument '" + name + "' must be between " + min + " and " + max + ".");
            }
            return val;
          } catch (NumberFormatException e) {
            throw new CommandArgumentException("Argument '" + name + "' must be a valid integer.");
          }
        },
        (s, in) -> {
          if (in.isEmpty()) {
            return List.of("<" + name + ">");
          }
          return List.of();
        });
  }

  public static CommandArgument<Double> doubleNum(String name) {
    return doubleNum(name, -Double.MAX_VALUE, Double.MAX_VALUE);
  }

  public static CommandArgument<Double> doubleNum(String name, double min, double max) {
    return new SimpleArgument<>(
        name,
        false,
        false,
        (s, in) -> {
          try {
            double val = Double.parseDouble(in);
            if (!Double.isFinite(val) || val < min || val > max) {
              throw new CommandArgumentException(
                  "Argument '" + name + "' must be between " + min + " and " + max + ".");
            }
            return val;
          } catch (NumberFormatException e) {
            throw new CommandArgumentException("Argument '" + name + "' must be a valid number.");
          }
        },
        (s, in) -> {
          if (in.isEmpty()) {
            return List.of("<" + name + ">");
          }
          return List.of();
        });
  }

  // ==========================================
  // Boolean Argument
  // ==========================================

  public static CommandArgument<Boolean> bool(String name) {
    return new SimpleArgument<>(
        name,
        false,
        false,
        (s, in) -> {
          if (in.equalsIgnoreCase("true") || in.equalsIgnoreCase("yes") || in.equals("1")) {
            return true;
          }
          if (in.equalsIgnoreCase("false") || in.equalsIgnoreCase("no") || in.equals("0")) {
            return false;
          }
          throw new CommandArgumentException("Argument '" + name + "' must be true or false.");
        },
        (s, in) -> {
          return List.of("true", "false").stream()
              .filter(v -> v.startsWith(in.toLowerCase()))
              .collect(Collectors.toList());
        });
  }

  // ==========================================
  // Player Argument
  // ==========================================

  public static CommandArgument<Player> player(String name) {
    return new SimpleArgument<>(
        name,
        false,
        false,
        (s, in) -> {
          Player target = Bukkit.getPlayerExact(in);
          if (target == null) {
            target = Bukkit.getPlayer(in);
          }
          if (target == null || !target.isOnline()) {
            throw new CommandArgumentException("Player '" + in + "' was not found online.");
          }
          return target;
        },
        (s, in) -> {
          String lower = in.toLowerCase();
          return Bukkit.getOnlinePlayers().stream()
              .map(Player::getName)
              .filter(pName -> pName.toLowerCase().startsWith(lower))
              .sorted()
              .collect(Collectors.toList());
        });
  }

  // ==========================================
  // Choice Arguments (Static & Dynamic)
  // ==========================================

  public static CommandArgument<String> choice(String name, String... choices) {
    return choice(name, Arrays.asList(choices));
  }

  public static CommandArgument<String> choice(String name, Collection<String> choices) {
    return choice(name, () -> choices);
  }

  public static CommandArgument<String> choice(
      String name, Supplier<Collection<String>> choiceSupplier) {
    return new SimpleArgument<>(
        name,
        false,
        false,
        (s, in) -> {
          Collection<String> choices = choiceSupplier.get();
          if (choices == null) {
            return in;
          }
          for (String c : choices) {
            if (c.equalsIgnoreCase(in)) {
              return c;
            }
          }
          throw new CommandArgumentException(
              "Invalid choice for '" + name + "'. Valid options: " + String.join(", ", choices));
        },
        (s, in) -> {
          Collection<String> choices = choiceSupplier.get();
          if (choices == null) return List.of();
          String lower = in.toLowerCase();
          return choices.stream()
              .filter(c -> c.toLowerCase().startsWith(lower))
              .sorted()
              .collect(Collectors.toList());
        });
  }

  // ==========================================
  // Custom Extensible Argument
  // ==========================================

  public static <T> CommandArgument<T> custom(
      String name, Function<String, T> parser, Supplier<Collection<String>> tabCompleter) {
    return new SimpleArgument<>(
        name,
        false,
        false,
        (s, in) -> {
          try {
            T res = parser.apply(in);
            if (res == null) {
              throw new CommandArgumentException("Invalid value for '" + name + "': " + in);
            }
            return res;
          } catch (CommandArgumentException e) {
            throw e;
          } catch (Exception e) {
            throw new CommandArgumentException("Failed to parse '" + name + "': " + e.getMessage());
          }
        },
        (s, in) -> {
          if (tabCompleter == null) return List.of();
          Collection<String> coll = tabCompleter.get();
          if (coll == null) return List.of();
          String lower = in.toLowerCase();
          return coll.stream()
              .filter(c -> c.toLowerCase().startsWith(lower))
              .collect(Collectors.toList());
        });
  }

  // ==========================================
  // Implementation Record
  // ==========================================

  private static class SimpleArgument<T> implements CommandArgument<T> {
    private final String name;
    private final boolean optional;
    private final boolean greedy;
    private final ArgumentParser<T> parser;
    private final ArgumentCompleter completer;

    public SimpleArgument(
        String name,
        boolean optional,
        boolean greedy,
        ArgumentParser<T> parser,
        ArgumentCompleter completer) {
      this.name = name;
      this.optional = optional;
      this.greedy = greedy;
      this.parser = parser;
      this.completer = completer;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isOptional() {
      return optional;
    }

    @Override
    public boolean isGreedy() {
      return greedy;
    }

    @Override
    public T parse(CommandSender sender, String input) throws CommandArgumentException {
      return parser.parse(sender, input);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String input) {
      return completer.complete(sender, input);
    }
  }

  @FunctionalInterface
  private interface ArgumentParser<T> {
    T parse(CommandSender sender, String input) throws CommandArgumentException;
  }

  @FunctionalInterface
  private interface ArgumentCompleter {
    List<String> complete(CommandSender sender, String input);
  }
}
