package dev.mukulx.javaskript.dependency;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Paper plugin loader that resolves and loads dependencies before the plugin initializes
 *
 * <p>This uses Paper's PluginLoader API to safely load dependencies at the correct time in the
 * plugin lifecycle.
 */
@SuppressWarnings("UnstableApiUsage")
public class DependencyLoader implements PluginLoader {

  @Override
  public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
    // Create Maven resolver
    MavenLibraryResolver resolver = new MavenLibraryResolver();

    // Use Paper's repository which mirrors Maven Central
    resolver.addRepository(
        new RemoteRepository.Builder(
                "paper", "default", "https://repo.papermc.io/repository/maven-public/")
            .build());

    // Declare plugin dependencies
    String[] dependencies = {
      "org.eclipse.jdt:org.eclipse.jdt.core:3.45.0",
      "org.ow2.asm:asm:9.10.1",
      "org.ow2.asm:asm-commons:9.10.1",
      "org.xerial:sqlite-jdbc:3.53.1.0",
      "com.google.code.gson:gson:2.11.0",
      "net.kyori:examination-api:1.3.0",
      "net.kyori:examination-string:1.3.0"
    };

    for (String dep : dependencies) {
      resolver.addDependency(new Dependency(new DefaultArtifact(dep), null));
    }

    // Add resolved dependencies to classpath
    classpathBuilder.addLibrary(resolver);
  }
}
