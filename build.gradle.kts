plugins {
    java
    `maven-publish`
    id("com.diffplug.spotless") version "6.25.0"
    id("com.gradleup.shadow") version "9.3.1"
}

group = "dev.mukulx"
version = "2.0.1"
description = "In-memory scripting engine for Paper and Folia"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.eclipse.jdt:org.eclipse.jdt.core:3.46.0")
    compileOnly("org.ow2.asm:asm:9.10.1")
    compileOnly("org.xerial:sqlite-jdbc:3.46.1.3")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    implementation("org.bstats:bstats-bukkit:3.2.1")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.xerial:sqlite-jdbc:3.46.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    
    filesMatching("paper-plugin.yml") {
        expand("version" to version)
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

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks.shadowJar)
        }
    }
}
