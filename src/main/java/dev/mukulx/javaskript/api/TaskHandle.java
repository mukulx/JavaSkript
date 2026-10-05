package dev.mukulx.javaskript.api;

/** A script-owned scheduled task that can be inspected and cancelled without platform casts. */
public interface TaskHandle {
  void cancel();

  boolean isCancelled();

  Object unwrap();
}
