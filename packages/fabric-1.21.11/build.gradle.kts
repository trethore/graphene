import io.github.trethore.buildlogic.unpack

plugins {
  id("net.fabricmc.fabric-loom-remap")
  `maven-publish`
}

val minecraftVersion = "1.21.11"
val loaderVersion = providers.gradleProperty("loader_version").get()
val fabricApiVersion = "0.141.4+1.21.11"
val jcefGithubVersion = providers.gradleProperty("jcefgithub_version").get()

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

dependencies {
  unpack(minecraft("com.mojang:minecraft:$minecraftVersion"))
  mappings(loom.officialMojangMappings())
  modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
  unpack(modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion"))

  implementation(project(":packages:common"))
  include(project(":packages:common"))
  include("io.github.trethore:jcefgithub:${jcefGithubVersion}") {
    isTransitive = false
    artifact {
      classifier = "all-relocated"
    }
  }
}

val checkFabricArchitecture by tasks.registering {
  group = "verification"
  description = "Ensures Fabric source code does not access JCEF directly."

  val javaSources =
      fileTree("src") {
        include("**/*.java")
      }

  inputs.files(javaSources)

  doLast {
    val forbiddenImports =
        listOf(
            "org.cef.",
            "io.github.trethore.jcefgithub.",
        )

    val violations =
        javaSources.files.flatMap { sourceFile ->
          sourceFile.readLines().mapIndexedNotNull { index, line ->
            val trimmedLine = line.trim()
            val forbiddenImport = forbiddenImports.firstOrNull { packageName ->
              trimmedLine.startsWith("import $packageName")
            }

            forbiddenImport?.let {
              "${sourceFile.relativeTo(projectDir)}:${index + 1}: $trimmedLine"
            }
          }
        }

    check(violations.isEmpty()) {
      buildString {
        appendLine("Fabric code must not import JCEF directly.")
        appendLine("JCEF can only be accessed from packages/common.")
        appendLine()
        violations.forEach(::appendLine)
      }
    }
  }
}

tasks.named("check") {
  dependsOn(checkFabricArchitecture)
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

  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
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
      from(components["java"])
    }
  }
}
