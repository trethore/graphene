import io.github.trethore.buildlogic.unpack
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
  alias(libs.plugins.fabric.loom)
  id("io.github.trethore.architecture-check")
  `maven-publish`
  signing
}

val minecraftVersion = "26.2"
val loaderVersion = libs.versions.fabric.loader.get()
val fabricApiVersion = "0.155.2+26.2"
val jcefGithubVersion = libs.versions.jcefgithub.get()
val mavenCentralSigningKey = providers.environmentVariable("MAVEN_GPG_PRIVATE_KEY")
val mavenCentralSigningPassphrase = providers.environmentVariable("MAVEN_GPG_PASSPHRASE")
val isMavenCentralPublishRequested =
    gradle.startParameter.taskNames.any { taskName ->
      taskName.contains("MavenCentralBundle", ignoreCase = true)
    }

base {
  archivesName = rootProject.name
}

loom {
  runs {
    named("client") {
      displayName.set("Minecraft Client 26.2")
      appendProjectPathToDisplayName.set(false)
      generateRunConfig.set(true)
      runDirectory.set(layout.projectDirectory.dir("run/client"))
    }

    named("server") {
      displayName.set("Minecraft Server 26.2")
      appendProjectPathToDisplayName.set(false)
      generateRunConfig.set(true)
      runDirectory.set(layout.projectDirectory.dir("run/server"))
    }
  }
}

val embeddedCommon =
    configurations.create("embeddedCommon") {
      isCanBeConsumed = false
      isCanBeResolved = false
    }

configurations.implementation {
  extendsFrom(embeddedCommon)
}

configurations.include {
  extendsFrom(embeddedCommon)
}

dependencies {
  unpack(minecraft("com.mojang:minecraft:$minecraftVersion"))
  implementation(libs.fabric.loader)
  unpack(implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion"))

  embeddedCommon(project(":packages:common"))
  include("io.github.trethore:jcefgithub:${jcefGithubVersion}") {
    isTransitive = false
    artifact {
      classifier = "all-relocated"
    }
  }
}

architectureChecks {
  register("jcefIsolation") {
    sources.from(fileTree("src") { include("**/*.java") })
    forbiddenImports.addAll(
        "org.cef.",
        "io.github.trethore.jcefgithub.",
    )
    failureMessage.set("Fabric code must not access JCEF directly; use the common API instead.")
  }
}

tasks.processResources {
  val version = project.version.toString()
  val properties =
      mapOf(
          "version" to version,
          "minecraftVersion" to minecraftVersion,
          "loaderVersion" to loaderVersion,
          "fabricApiVersion" to fabricApiVersion,
      )
  inputs.properties(properties)

  filesMatching("fabric.mod.json") {
    expand(properties)
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release = 25
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
  withSourcesJar()
  withJavadocJar()

  sourceCompatibility = JavaVersion.VERSION_25
  targetCompatibility = JavaVersion.VERSION_25
}

tasks.named<Jar>("sourcesJar") {
  from(project(":packages:common").file("src/main/java"))
  from(project(":packages:common").file("src/main/resources"))
}

tasks.jar {
  val projectName = rootProject.name
  inputs.property("projectName", projectName)
  archiveFileName.set("${rootProject.name}-${project.version}-fabric-${minecraftVersion}.jar")

  from(rootProject.file("LICENSE")) {
    rename { "${it}_$projectName" }
  }
}

tasks.register<Sync>("stageGithubRelease") {
  group = "distribution"
  description = "Stages the runtime JAR for a GitHub release."

  val jar = tasks.named<AbstractArchiveTask>("jar")

  dependsOn(jar)
  from(jar.flatMap { it.archiveFile })
  into(layout.buildDirectory.dir("github-release"))
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      artifactId = "graphene-ui-$minecraftVersion"
      from(components["java"])

      pom {
        name.set("Graphene UI")
        description.set("Client-side Chromium-based UI library for Minecraft Fabric mods.")
        url.set("https://github.com/trethore/graphene")

        licenses {
          license {
            name.set("MIT License")
            url.set("https://github.com/trethore/graphene/blob/main/LICENSE")
          }
        }

        developers {
          developer {
            id.set("trethore")
            name.set("Titouan Rethore")
            email.set("titou.rethore@gmail.com")
          }
        }

        scm {
          connection.set("scm:git:git://github.com/trethore/graphene.git")
          developerConnection.set("scm:git:ssh://git@github.com/trethore/graphene.git")
          url.set("https://github.com/trethore/graphene")
        }

        withXml {
          val dependencies = asNode().get("dependencies") as groovy.util.NodeList
          dependencies
              .flatMap { (it as groovy.util.Node).children() }
              .filterIsInstance<groovy.util.Node>()
              .filter { dependency ->
                val groupId = dependency.get("groupId") as groovy.util.NodeList
                val artifactId = dependency.get("artifactId") as groovy.util.NodeList
                groupId.text() == project.group.toString() && artifactId.text() == "common"
              }
              .forEach { dependency -> dependency.parent().remove(dependency) }
        }
      }
    }
  }

  repositories {
    maven {
      name = "MavenCentralBundle"
      url = rootProject.layout.buildDirectory.dir("central-portal/staging").get().asFile.toURI()
    }
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/trethore/graphene")
      credentials {
        username = providers.environmentVariable("GITHUB_ACTOR").orNull
        password = providers.environmentVariable("GITHUB_TOKEN").orNull
      }
    }
  }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
  enabled = false
}

if (isMavenCentralPublishRequested) {
  check(!project.version.toString().endsWith("-SNAPSHOT")) {
    "Maven Central publishing requires a non-SNAPSHOT release version"
  }
  check(mavenCentralSigningKey.isPresent) {
    "Maven Central publishing requires MAVEN_GPG_PRIVATE_KEY"
  }
  check(mavenCentralSigningPassphrase.isPresent) {
    "Maven Central publishing requires MAVEN_GPG_PASSPHRASE"
  }
}

signing {
  setRequired { isMavenCentralPublishRequested }
  if (mavenCentralSigningKey.isPresent && mavenCentralSigningPassphrase.isPresent) {
    useInMemoryPgpKeys(mavenCentralSigningKey.get(), mavenCentralSigningPassphrase.get())
    sign(publishing.publications["mavenJava"])
  }
}
