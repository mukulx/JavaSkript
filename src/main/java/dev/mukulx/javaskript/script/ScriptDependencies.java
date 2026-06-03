package dev.mukulx.javaskript.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare Maven dependencies for a script
 *
 * <p>Example usage:
 *
 * <pre>
 * &#64;ScriptDependencies({
 *     "com.google.code.gson:gson:2.11.0",
 *     "org.mongodb:mongodb-driver-sync:5.1.0"
 * })
 * public class MyScript implements Listener {
 *     // Your script code
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScriptDependencies {
  String[] value();
}
