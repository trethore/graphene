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

// Gradle project name
rootProject.name = "graphene"

include("packages:common")

include("packages:fabric-26.2")

include("packages:fabric-1.21.11")

include("tools:debug-client-fabric-26.2")

include("tools:debug-client-fabric-1.21.11")

project(":tools").projectDir = file("debug-client")

project(":tools:debug-client-fabric-26.2").projectDir = file("debug-client/fabric-26.2")

project(":tools:debug-client-fabric-1.21.11").projectDir = file("debug-client/fabric-1.21.11")
