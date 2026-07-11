pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "example-mod-build-logic"

include("unpack-sources")

include("sonar")
