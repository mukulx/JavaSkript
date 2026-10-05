package dev.mukulx.javaskript.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a script must be loaded after the named scripts when several are loaded together
 * (server start, {@code /js reload all}, enabling or reloading a folder).
 *
 * <p>Each name is a script key such as {@code "economy/Bank"} or just the class name {@code
 * "Bank"}. A name that matches no script being loaded is reported and ignored, and a cycle is
 * reported and loaded in file order, so a wrong declaration never keeps a script from loading.
 *
 * <p>The ordering is only applied to batch loads. Reloading a single script changes nothing about
 * the others.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LoadAfter {
  /** Scripts that must be loaded first. */
  String[] value();
}
