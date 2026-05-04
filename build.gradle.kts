import org.gradle.api.JavaVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
	id("net.fabricmc.fabric-loom-remap") apply false
}

val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(21)

tasks.register("cleanSources") {
	group = "help"
	description = "Deletes unpacked sources in references/."
	dependsOn(":fabric-1.21.11:cleanSources")
}

tasks.register("unpackSources") {
	group = "help"
	description = "Unpacks dependency sources and clones git repos into references/."
	dependsOn(":fabric-1.21.11:unpackSources")
}

subprojects {
	group = rootProject.property("maven_group") as String
	version = rootProject.property("mod_version") as String

	repositories {
		mavenLocal()
		mavenCentral()
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
	}

	plugins.withId("java") {
		extensions.configure(JavaPluginExtension::class.java) {
			toolchain {
				languageVersion.set(javaLanguageVersion)
			}
			sourceCompatibility = JavaVersion.toVersion(javaLanguageVersion.asInt())
			targetCompatibility = JavaVersion.toVersion(javaLanguageVersion.asInt())
		}

		tasks.withType(JavaCompile::class.java).configureEach {
			options.release.set(javaLanguageVersion.asInt())
		}

		tasks.withType(Test::class.java).configureEach {
			useJUnitPlatform()
		}
	}
}
