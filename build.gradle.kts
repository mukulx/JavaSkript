plugins {
    java
    id("com.diffplug.spotless") version "6.25.0"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "dev.mukulx"
version = "1.1.0"
description = "Java scripting engine for Paper and Folia"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:examination-api:1.3.0")
    compileOnly("net.kyori:examination-string:1.3.0")
    compileOnly("org.eclipse.jdt:org.eclipse.jdt.core:3.45.0")
    compileOnly("org.ow2.asm:asm:9.10.1")
    compileOnly("org.ow2.asm:asm-commons:9.10.1")
    compileOnly("org.xerial:sqlite-jdbc:3.53.1.0")
    compileOnly("com.google.code.gson:gson:2.11.0")
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

tasks.shadowJar {
    relocate("org.bstats", "${project.group}.javaskript.libs.bstats")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
