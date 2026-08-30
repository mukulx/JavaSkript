package dev.mukulx.javaskript.api.command;

/** Thrown when a command argument fails to parse. */
public class CommandArgumentException extends Exception {

  public CommandArgumentException(String message) {
    super(message);
  }
}
