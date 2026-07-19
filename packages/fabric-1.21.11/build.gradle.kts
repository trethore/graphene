import io.github.trethore.buildlogic.unpack
import org.gradle.api.publish.tasks.GenerateModuleMetadata

plugins {
  id("net.fabricmc.fabric-loom-remap")
  id("io.github.trethore.architecture-check")
  `maven-publish`
  signing
}

val minecraftVersion = "1.21.11"
val loaderVersion = providers.gradleProperty("loader_version").get()
val fabricApiVersion = "0.141.4+1.21.11"
val jcefGithubVersion = providers.gradleProperty("jcefgithub_version").get()
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
      displayName.set("Minecraft Client 1.21.11")
      appendProjectPathToDisplayName.set(false)
      generateRunConfig.set(true)
      runDirectory.set(layout.projectDirectory.dir("run/client"))
    }

    named("server") {
      displayName.set("Minecraft Server 1.21.11")
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
  mappings(loom.officialMojangMappings())
  modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
  unpack(modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion"))

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
  options.release = 21
}

java {
  withSourcesJar()
  withJavadocJar()

  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

tasks.named<Jar>("sourcesJar") {
  from(project(":packages:common").file("src/main/java"))
  from(project(":packages:common").file("src/main/resources"))
}

tasks.jar {
  val projectName = rootProject.name
  inputs.property("projectName", projectName)

  from(rootProject.file("LICENSE")) {
    rename { "${it}_$projectName" }
  }
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      artifactId = "graphene-ui"
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
  }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
  enabled = false
}

if (isMavenCentralPublishRequested) {
  check(!project.version.toString().endsWith("-SNAPSHOT")) {
    "Maven Central publishing requires a non-SNAPSHOT mod_version"
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
