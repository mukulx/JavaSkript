package dev.mukulx.javaskript.dependency;

import com.google.gson.Gson;
import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class DependencyManager {

  private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
  private final JavaSkriptPlugin plugin;
  private final Gson gson;
  private final File libsDirectory;
  private final Map<String, List<File>> resolvedDependencies;

  public DependencyManager(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    this.gson = new Gson();
    String cacheFolderName = plugin.getConfig().getString("dependencies.cache-folder", "libs");
    this.libsDirectory = new File(plugin.getDataFolder(), cacheFolderName);
    this.resolvedDependencies = new HashMap<>();

    if (!libsDirectory.exists()) {
      libsDirectory.mkdirs();
    }
  }

  /**
   * Resolves and downloads a Maven dependency with all transitive dependencies
   *
   * @param coordinate Maven coordinate in format "groupId:artifactId:version"
   * @return List of resolved JAR files, or empty list if resolution failed
   */
  public List<File> resolveDependency(String coordinate) {
    // Check cache first
    if (resolvedDependencies.containsKey(coordinate)) {
      plugin.getLogger().info("Using cached dependency: " + coordinate);
      return resolvedDependencies.get(coordinate);
    }

    plugin.getLogger().info("Resolving dependency: " + coordinate);

    try {
      String[] parts = coordinate.split(":");
      if (parts.length != 3) {
        plugin.getLogger().severe("Invalid Maven coordinate: " + coordinate);
        return Collections.emptyList();
      }

      String groupId = parts[0];
      String artifactId = parts[1];
      String version = parts[2];

      // Download main artifact
      File mainJar = downloadArtifact(groupId, artifactId, version);
      if (mainJar == null) {
        return Collections.emptyList();
      }

      List<File> allJars = new ArrayList<>();
      allJars.add(mainJar);

      // Try to resolve transitive dependencies from POM
      List<String> transitiveDeps = parseTransitiveDependencies(groupId, artifactId, version);
      for (String dep : transitiveDeps) {
        try {
          String[] depParts = dep.split(":");
          if (depParts.length >= 3) {
            File depJar = downloadArtifact(depParts[0], depParts[1], depParts[2]);
            if (depJar != null) {
              allJars.add(depJar);
            }
          }
        } catch (Exception e) {
          plugin
              .getLogger()
              .warning("Failed to download transitive dependency: " + dep + " - " + e.getMessage());
        }
      }

      // Cache the result
      resolvedDependencies.put(coordinate, allJars);

      plugin
          .getLogger()
          .info(
              "Successfully resolved " + allJars.size() + " file(s) for dependency: " + coordinate);

      return allJars;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to resolve dependency: " + coordinate, e);
      return Collections.emptyList();
    }
  }

  private File downloadArtifact(String groupId, String artifactId, String version) {
    try {
      String groupPath = groupId.replace('.', '/');
      String jarName = artifactId + "-" + version + ".jar";
      File localFile = new File(libsDirectory, jarName);

      // If already downloaded, return it
      if (localFile.exists()) {
        plugin.getLogger().fine("Using cached JAR: " + jarName);
        return localFile;
      }

      // Build download URL
      String urlString =
          MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" + jarName;

      plugin.getLogger().info("Downloading: " + urlString);

      // Download
      URL url = new URL(urlString);
      try (InputStream in = url.openStream()) {
        Files.copy(in, localFile.toPath());
      }

      plugin.getLogger().info("Downloaded: " + jarName);
      return localFile;

    } catch (Exception e) {
      plugin
          .getLogger()
          .warning(
              "Failed to download "
                  + groupId
                  + ":"
                  + artifactId
                  + ":"
                  + version
                  + " - "
                  + e.getMessage());
      return null;
    }
  }

  private List<String> parseTransitiveDependencies(
      String groupId, String artifactId, String version) {
    List<String> dependencies = new ArrayList<>();

    try {
      String groupPath = groupId.replace('.', '/');
      String pomName = artifactId + "-" + version + ".pom";
      String urlString =
          MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" + pomName;

      URL url = new URL(urlString);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
        StringBuilder pomContent = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          pomContent.append(line).append("\n");
        }

        // Simple XML parsing for dependencies
        String pom = pomContent.toString();
        int depStart = pom.indexOf("<dependencies>");
        int depEnd = pom.indexOf("</dependencies>");

        if (depStart != -1 && depEnd != -1) {
          String depsSection = pom.substring(depStart, depEnd);

          // Extract each dependency
          int pos = 0;
          while ((pos = depsSection.indexOf("<dependency>", pos)) != -1) {
            int endPos = depsSection.indexOf("</dependency>", pos);
            if (endPos == -1) break;

            String depBlock = depsSection.substring(pos, endPos);

            String depGroupId = extractXmlTag(depBlock, "groupId");
            String depArtifactId = extractXmlTag(depBlock, "artifactId");
            String depVersion = extractXmlTag(depBlock, "version");
            String scope = extractXmlTag(depBlock, "scope");
            String optional = extractXmlTag(depBlock, "optional");

            // Skip dependencies provided by Paper/Bukkit
            if (depGroupId != null
                && (depGroupId.equals("org.slf4j")
                    || depGroupId.equals("org.bukkit")
                    || depGroupId.equals("io.papermc.paper"))) {
              pos = endPos;
              continue;
            }

            // Skip if version contains unresolved property placeholders
            if (depVersion != null && depVersion.contains("${")) {
              plugin
                  .getLogger()
                  .fine(
                      "Skipping dependency with property placeholder: "
                          + depGroupId
                          + ":"
                          + depArtifactId
                          + ":"
                          + depVersion);
              pos = endPos;
              continue;
            }

            // Only include compile scope dependencies
            if (depGroupId != null
                && depArtifactId != null
                && depVersion != null
                && !"test".equals(scope)
                && !"provided".equals(scope)
                && !"true".equals(optional)) {
              dependencies.add(depGroupId + ":" + depArtifactId + ":" + depVersion);
            }

            pos = endPos;
          }
        }
      }

    } catch (Exception e) {
      plugin.getLogger().fine("Could not parse POM for transitive dependencies: " + e.getMessage());
    }

    return dependencies;
  }

  private String extractXmlTag(String xml, String tagName) {
    String startTag = "<" + tagName + ">";
    String endTag = "</" + tagName + ">";

    int start = xml.indexOf(startTag);
    int end = xml.indexOf(endTag);

    if (start != -1 && end != -1 && end > start) {
      return xml.substring(start + startTag.length(), end).trim();
    }

    return null;
  }

  /**
   * Resolves multiple dependencies at once
   *
   * @param coordinates List of Maven coordinates
   * @return Combined list of all resolved JAR files
   */
  public List<File> resolveDependencies(List<String> coordinates) {
    List<File> allFiles = new ArrayList<>();

    for (String coordinate : coordinates) {
      List<File> files = resolveDependency(coordinate);
      allFiles.addAll(files);
    }

    return allFiles;
  }

  /**
   * Get all resolved dependencies for a specific coordinate
   *
   * @param coordinate Maven coordinate
   * @return List of resolved files, or empty list if not resolved
   */
  public List<File> getResolvedDependency(String coordinate) {
    return resolvedDependencies.getOrDefault(coordinate, Collections.emptyList());
  }

  /** Clear the dependency cache */
  public void clearCache() {
    resolvedDependencies.clear();
    plugin.getLogger().info("Dependency cache cleared");
  }

  /** Get the libs directory where dependencies are stored */
  public File getLibsDirectory() {
    return libsDirectory;
  }

  /** Get all cached dependencies */
  public Map<String, List<File>> getAllResolvedDependencies() {
    return Collections.unmodifiableMap(resolvedDependencies);
  }
}
