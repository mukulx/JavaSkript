package dev.mukulx.javaskript.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark scripts that support Folia. Scripts without this annotation will show a
 * warning on Folia servers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FoliaSupport {
  /** Whether this script supports Folia. Default is false (Paper-only). */
  boolean value() default true;
}
