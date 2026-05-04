pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}

	val loomVersion = settings.extra["loom_version"] as String
	plugins {
		id("net.fabricmc.fabric-loom-remap") version loomVersion
	}
}

rootProject.name = "graphene-ui"

include("common")
include("fabric-1.21.11")
