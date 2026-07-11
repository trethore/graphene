import io.github.trethore.buildlogic.unpack

plugins {
  id("net.fabricmc.fabric-loom-remap")
  `maven-publish`
}

val minecraftVersion = "1.21.11"
val loaderVersion = "0.19.2"
val fabricApiVersion = "0.141.4+1.21.11"

base {
  archivesName = rootProject.name
}

loom {
  runs {
    named("client") {
      name("Minecraft Client 1.21.11")
      appendProjectPathToConfigName.set(false)
      ideConfigGenerated(true)
      runDir("run/client")
    }

    named("server") {
      name("Minecraft Server 1.21.11")
      appendProjectPathToConfigName.set(false)
      ideConfigGenerated(true)
      runDir("run/server")
    }
  }
}

configurations.implementation {
  extendsFrom(configurations.include.get())
}

dependencies {
  unpack(minecraft("com.mojang:minecraft:$minecraftVersion"))
  mappings(loom.officialMojangMappings())
  modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
  unpack(modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion"))

  include(project(":packages:common"))
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
