package dev.mukulx.javaskript.script;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Orders scripts for a batch load according to their {@link LoadAfter} declarations. */
final class ScriptLoadOrder {

  /** A script to load: its file, its script key, and the names it wants to load after. */
  record Entry(File file, String key, List<String> after) {}

  // Same shape as the @Disabled marker: an annotation at the start of a line, optionally after
  // other annotations. Declarations inside comments or strings are not picked up.
  private static final Pattern LOAD_AFTER_PATTERN =
      Pattern.compile(
          "(?m)^\\s*(?:@\\w+(?:\\([^)]*\\))?\\s+)*@LoadAfter\\s*\\(\\s*(?:value\\s*=\\s*)?"
              + "(\\{[^}]*\\}|\"[^\"]*\")\\s*\\)");
  private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

  private ScriptLoadOrder() {}

  /** The names listed in the script's {@code @LoadAfter}, in source order. */
  static List<String> parseLoadAfter(String source) {
    List<String> names = new ArrayList<>();
    Matcher annotation = LOAD_AFTER_PATTERN.matcher(source);
    while (annotation.find()) {
      Matcher quoted = QUOTED.matcher(annotation.group(1));
      while (quoted.find()) {
        String name = quoted.group(1).trim();
        if (!name.isEmpty() && !names.contains(name)) {
          names.add(name);
        }
      }
    }
    return names;
  }

  /**
   * Return the entries' files with every script after the ones it names. Scripts without a
   * constraint keep their original relative order. Problems are reported through {@code warn} and
   * never prevent a script from being loaded.
   */
  static List<File> sort(List<Entry> entries, Consumer<String> warn) {
    int count = entries.size();
    List<Set<Integer>> dependencies = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Entry entry = entries.get(i);
      Set<Integer> deps = new LinkedHashSet<>();
      for (String name : entry.after()) {
        boolean found = false;
        for (int j = 0; j < count; j++) {
          if (j != i && matches(entries.get(j).key(), name)) {
            deps.add(j);
            found = true;
          }
        }
        if (!found) {
          warn.accept(
              entry.key()
                  + " declares @LoadAfter(\""
                  + name
                  + "\") but no such script is being loaded; ignoring it");
        }
      }
      dependencies.add(deps);
    }

    List<File> ordered = new ArrayList<>(count);
    boolean[] placed = new boolean[count];
    int remaining = count;
    while (remaining > 0) {
      int next = -1;
      for (int i = 0; i < count && next < 0; i++) {
        if (!placed[i] && dependencies.get(i).stream().allMatch(dep -> placed[dep])) {
          next = i;
        }
      }
      if (next < 0) {
        List<String> stuck = new ArrayList<>();
        for (int i = 0; i < count; i++) {
          if (!placed[i]) {
            stuck.add(entries.get(i).key());
          }
        }
        warn.accept(
            "Circular @LoadAfter between " + String.join(", ", stuck) + "; loading in file order");
        for (int i = 0; i < count; i++) {
          if (!placed[i]) {
            ordered.add(entries.get(i).file());
          }
        }
        break;
      }
      placed[next] = true;
      ordered.add(entries.get(next).file());
      remaining--;
    }
    return ordered;
  }

  /** A name matches a script by full key (with or without .java) or by class name. */
  private static boolean matches(String scriptKey, String name) {
    String key = stripJava(scriptKey).toLowerCase(Locale.ROOT);
    String wanted = stripJava(name.replace('\\', '/')).toLowerCase(Locale.ROOT);
    if (key.equals(wanted)) {
      return true;
    }
    String simple = key.substring(key.lastIndexOf('/') + 1);
    return !wanted.contains("/") && simple.equals(wanted);
  }

  private static String stripJava(String value) {
    return value.endsWith(".java") ? value.substring(0, value.length() - 5) : value;
  }
}
