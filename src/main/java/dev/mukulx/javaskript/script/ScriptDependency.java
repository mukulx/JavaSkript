package dev.mukulx.javaskript.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify script dependencies Scripts with dependencies will be loaded after their
 * dependencies
 *
 * <p>Example: @Depends({"DatabaseHelper.java", "ConfigManager.java"}) public class MyScript
 * implements Listener { // ... }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScriptDependency {
  /** Array of script file names this script depends on */
  String[] value();
}
