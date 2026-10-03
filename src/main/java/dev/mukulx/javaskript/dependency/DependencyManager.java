package dev.mukulx.javaskript.dependency;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DependencyManager {

  private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
  private final JavaSkriptPlugin plugin;
  private final File libsDirectory;
  private final Map<String, List<File>> resolvedDependencies;

  public DependencyManager(JavaSkriptPlugin plugin) {
    this.plugin = plugin;
    String cacheFolderName = plugin.getConfig().getString("dependencies.cache-folder", "libs");
    this.libsDirectory = new File(plugin.getDataFolder(), cacheFolderName);
    this.resolvedDependencies = new ConcurrentHashMap<>();

    if (!libsDirectory.exists()) {
      libsDirectory.mkdirs();
    }
  }

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

  /** Open a connection that can never hang a script load indefinitely. */
  private URLConnection openConnection(String urlString) throws IOException {
    URLConnection connection = java.net.URI.create(urlString).toURL().openConnection();
    connection.setConnectTimeout(10_000);
    connection.setReadTimeout(30_000);
    return connection;
  }

  /**
   * Compare the file with the SHA-1 Maven Central publishes next to every artifact. Returns false
   * only on a real mismatch. If the checksum cannot be fetched, the download is accepted with a
   * warning so mirrors and older artifacts without checksums keep working.
   */
  private boolean matchesPublishedChecksum(File file, String artifactUrl) throws Exception {
    String expected;
    try (InputStream in = openConnection(artifactUrl + ".sha1").getInputStream()) {
      expected = new String(in.readNBytes(256), java.nio.charset.StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      plugin
          .getLogger()
          .warning("No published checksum for " + artifactUrl + ", skipping verification");
      return true;
    }
    // Some checksum files append the file name after the hash
    int space = expected.indexOf(' ');
    if (space > 0) {
      expected = expected.substring(0, space);
    }

    java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
    try (InputStream in = Files.newInputStream(file.toPath())) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
    }
    return HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(expected);
  }

  private File downloadArtifact(String groupId, String artifactId, String version) {
    File tempFile = null;
    try {
      String groupPath = groupId.replace('.', '/');
      String remoteName = artifactId + "-" + version + ".jar";
      // Local cache name carries the group so same-named artifacts from different groups don't
      // clash
      String jarName = groupId.replace('.', '_') + "-" + remoteName;
      File localFile = new File(libsDirectory, jarName);

      // If already downloaded and not empty, return it
      if (localFile.exists() && localFile.length() > 0) {
        plugin.getLogger().fine("Using cached JAR: " + jarName);
        return localFile;
      }

      String urlString =
          MAVEN_CENTRAL + groupPath + "/" + artifactId + "/" + version + "/" + remoteName;

      plugin.getLogger().info("Downloading: " + urlString);

      // Unique name, so two scripts resolving the same artifact never write the same temp file
      tempFile = Files.createTempFile(libsDirectory.toPath(), jarName, ".tmp").toFile();

      try (InputStream in = openConnection(urlString).getInputStream()) {
        Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }

      if (!matchesPublishedChecksum(tempFile, urlString)) {
        plugin.getLogger().severe("Checksum mismatch for " + jarName + ", discarding the download");
        tempFile.delete();
        return null;
      }

      if (tempFile.exists() && tempFile.length() > 0) {
        Files.move(
            tempFile.toPath(),
            localFile.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        plugin.getLogger().info("Downloaded: " + jarName);
        return localFile;
      } else {
        plugin.getLogger().warning("Downloaded file was empty: " + jarName);
        if (tempFile.exists()) {
          tempFile.delete();
        }
        return null;
      }

    } catch (Exception e) {
      if (tempFile != null && tempFile.exists()) {
        try {
          tempFile.delete();
        } catch (Exception ignored) {
        }
      }
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

      Document pom;
      try (InputStream in = openConnection(urlString).getInputStream()) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        pom = factory.newDocumentBuilder().parse(in);
      }

      Element project = pom.getDocumentElement();
      Map<String, String> properties = readProperties(project, groupId, version);

      // Only the project's own <dependencies>. Walking the whole document would also pick up
      // <dependencyManagement>, plugin dependencies and profiles.
      Element dependenciesElement = childElement(project, "dependencies");
      if (dependenciesElement == null) {
        return dependencies;
      }

      for (Node node = dependenciesElement.getFirstChild();
          node != null;
          node = node.getNextSibling()) {
        if (!(node instanceof Element dependency)
            || !dependency.getTagName().equals("dependency")) {
          continue;
        }

        String depGroupId = resolveProperties(childText(dependency, "groupId"), properties);
        String depArtifactId = resolveProperties(childText(dependency, "artifactId"), properties);
        String depVersion = resolveProperties(childText(dependency, "version"), properties);
        String scope = childText(dependency, "scope");
        String optional = childText(dependency, "optional");

        if (depGroupId == null || depArtifactId == null || depVersion == null) {
          // Versions managed by a parent POM are not resolved here
          continue;
        }

        if (depGroupId.equals("org.slf4j")
            || depGroupId.equals("org.bukkit")
            || depGroupId.equals("io.papermc.paper")) {
          continue;
        }

        if (depGroupId.contains("${")
            || depArtifactId.contains("${")
            || depVersion.contains("${")
            || depVersion.startsWith("[")
            || depVersion.startsWith("(")) {
          plugin
              .getLogger()
              .fine(
                  "Skipping dependency with unresolved version: "
                      + depGroupId
                      + ":"
                      + depArtifactId
                      + ":"
                      + depVersion);
          continue;
        }

        if (!"test".equals(scope) && !"provided".equals(scope) && !"true".equals(optional)) {
          dependencies.add(depGroupId + ":" + depArtifactId + ":" + depVersion);
        }
      }

    } catch (Exception e) {
      plugin.getLogger().fine("Could not parse POM for transitive dependencies: " + e.getMessage());
    }

    return dependencies;
  }

  /** Properties usable in ${...} placeholders: the POM's <properties> plus its own coordinates. */
  private Map<String, String> readProperties(Element project, String groupId, String version) {
    Map<String, String> properties = new HashMap<>();
    properties.put("project.groupId", groupId);
    properties.put("project.version", version);
    properties.put("pom.version", version);

    Element propertiesElement = childElement(project, "properties");
    if (propertiesElement != null) {
      for (Node node = propertiesElement.getFirstChild();
          node != null;
          node = node.getNextSibling()) {
        if (node instanceof Element property) {
          properties.put(property.getTagName(), property.getTextContent().trim());
        }
      }
    }
    return properties;
  }

  private String resolveProperties(String value, Map<String, String> properties) {
    if (value == null) {
      return null;
    }
    // A few passes cover properties defined in terms of other properties
    for (int pass = 0; pass < 3 && value.contains("${"); pass++) {
      for (Map.Entry<String, String> property : properties.entrySet()) {
        value = value.replace("${" + property.getKey() + "}", property.getValue());
      }
    }
    return value;
  }

  private Element childElement(Element parent, String tagName) {
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && element.getTagName().equals(tagName)) {
        return element;
      }
    }
    return null;
  }

  private String childText(Element parent, String tagName) {
    Element child = childElement(parent, tagName);
    if (child == null) {
      return null;
    }
    String text = child.getTextContent().trim();
    return text.isEmpty() ? null : text;
  }

  public List<File> resolveDependencies(List<String> coordinates) {
    List<File> allFiles = new ArrayList<>();

    for (String coordinate : coordinates) {
      List<File> files = resolveDependency(coordinate);
      allFiles.addAll(files);
    }

    return allFiles;
  }

  public List<File> getResolvedDependency(String coordinate) {
    return resolvedDependencies.getOrDefault(coordinate, Collections.emptyList());
  }

  public void clearCache() {
    resolvedDependencies.clear();
    plugin.getLogger().info("Dependency cache cleared");
  }

  public File getLibsDirectory() {
    return libsDirectory;
  }

  public Map<String, List<File>> getAllResolvedDependencies() {
    return Collections.unmodifiableMap(resolvedDependencies);
  }
}
