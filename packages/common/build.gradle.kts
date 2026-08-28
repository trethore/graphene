plugins {
  `java-library`
  `maven-publish`
  id("io.github.trethore.architecture-check")
}

val javaVersion = JavaLanguageVersion.of(21)
val jcefGithubVersion = libs.versions.jcefgithub.get()

val compileOnlyAndTestRuntime =
    configurations.create("compileOnlyAndTestRuntime") {
      isCanBeConsumed = false
      isCanBeResolved = false
    }

configurations {
  compileOnly { extendsFrom(compileOnlyAndTestRuntime) }
  testRuntimeOnly { extendsFrom(compileOnlyAndTestRuntime) }
}

dependencies {
  compileOnlyAndTestRuntime(libs.gson)
  compileOnlyAndTestRuntime(libs.slf4j.api)

  implementation("io.github.trethore:jcefgithub:${jcefGithubVersion}:all-relocated") {
    isTransitive = false
  }
}

architectureChecks {
  register("platformIndependence") {
    sources.from(fileTree("src/main/java") { include("**/*.java") })
    forbiddenImports.addAll(
        "net.minecraft.",
        "com.mojang.blaze3d.",
        "net.fabricmc.",
        "org.lwjgl.",
    )
    failureMessage.set(
        "Common code must not depend on Minecraft, Fabric, Mojang rendering, or LWJGL."
    )
  }

  register("publicApi") {
    sources.from(fileTree("src/main/java") { include("**/api/**/*.java") })
    forbiddenImports.addAll(
        "org.cef.",
        "io.github.trethore.jcefgithub.",
        "io.github.trethore.graphene.internal.",
    )
    allowedImports.addAll(
        "io.github.trethore.graphene.internal.runtime.GrapheneContextFactory",
        "io.github.trethore.graphene.internal.runtime.GrapheneRuntimeController",
    )
    failureMessage.set("The public common API may only expose Graphene-owned and JDK types.")
  }

  register("jcefIsolation") {
    sources.from(
        fileTree("src/main/java") {
          include("**/*.java")
          exclude("**/internal/cef/**/*.java")
        }
    )
    forbiddenImports.addAll(
        "org.cef.",
        "io.github.trethore.jcefgithub.",
    )
    failureMessage.set("JCEF access is restricted to the common internal CEF adapter.")
  }
}

java {
  withSourcesJar()
  toolchain.languageVersion.set(javaVersion)
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
}
