pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "build-logic"

include("architecture-check")

include("unpack-sources")

include("sonar")
