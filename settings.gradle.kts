pluginManagement {
  includeBuild("build-logic")

  repositories {
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net/")
    }
    mavenCentral()
    gradlePluginPortal()
  }

  plugins {
    id("net.fabricmc.fabric-loom-remap") version providers.gradleProperty("loom_version")
  }
}

// Gradle project name
rootProject.name = "graphene"

include("packages:common")

include("packages:fabric-1.21.11")
