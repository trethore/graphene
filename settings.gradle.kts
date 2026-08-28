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
}

rootProject.name = "graphene"

include("packages:common")

listOf("fabric-1.21.11", "fabric-26.2").forEach { target ->
  include("packages:$target")
  include("tools:debug-client-$target")
  project(":tools:debug-client-$target").projectDir = file("debug-client/$target")
}

project(":tools").projectDir = file("debug-client")
