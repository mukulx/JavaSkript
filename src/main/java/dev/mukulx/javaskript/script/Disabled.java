package dev.mukulx.javaskript.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation to mark a script as disabled. Disabled scripts will not be loaded by JavaSkript. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Disabled {
  /** Optional reason for disabling the script. */
  String value() default "";
}
