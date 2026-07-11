pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "build-logic"

include("unpack-sources")

include("sonar")
