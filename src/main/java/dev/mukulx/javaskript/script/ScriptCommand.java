package dev.mukulx.javaskript.script;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the command a script class registers when it implements CommandExecutor. Without it the
 * name is taken from the class name: {@code HealCommand} becomes {@code /heal}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScriptCommand {
  String value();
}
