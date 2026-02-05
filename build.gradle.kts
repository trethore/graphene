import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import tytoo.graphene.UnpackSourcesTask

plugins {
	id("net.fabricmc.fabric-loom-remap")
	id("maven-publish")
}

val modVersion = property("mod_version") as String
val mavenGroup = property("maven_group") as String
val archivesBaseName = property("archives_base_name") as String
val minecraftVersion = property("minecraft_version") as String
val loaderVersion = property("loader_version") as String
val fabricApiVersion = property("fabric_api_version") as String

version = modVersion
group = mavenGroup

base {
	archivesName.set(archivesBaseName)
}

repositories {
	// Add repositories to retrieve artifacts from in here.
}

val sourceDeps: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
	isTransitive = false
	attributes {
		attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
		attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
	}
	extendsFrom(
		configurations.named("implementation").get(),
		configurations.named("modImplementation").get()
	)
}

loom {
	splitEnvironmentSourceSets() // so client source set is created
}

val clientSS: NamedDomainObjectProvider<SourceSet> = sourceSets.named("client")
sourceSets {
	create("debug") {
		java.setSrcDirs(listOf("src/debug/java"))
		resources.setSrcDirs(listOf("src/debug/resources"))
		// Inherit classpaths + compiled output from client
		compileClasspath += clientSS.get().compileClasspath + clientSS.get().output
		runtimeClasspath += clientSS.get().runtimeClasspath + clientSS.get().output
	}
}

loom {
	mods {
		register("graphene-ui") {
			sourceSet(sourceSets.named("client").get())
		}
		register("graphene-ui-debug") {
			sourceSet(sourceSets.named("debug").get())
		}
	}

	runs {
		named("client") {
			client()
			source(sourceSets.named("client").get())
			ideConfigGenerated(true)
		}
		create("debugClient") {
			client()
			source(sourceSets.named("debug").get())
			ideConfigGenerated(true)
		}
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${minecraftVersion}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")
}

tasks.withType<ProcessResources>().configureEach {
	inputs.property("version", project.version)
	filesMatching("fabric.mod.json") {
		expand("version" to project.version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
}

java {
	withSourcesJar()
}

tasks.jar {
	val archivesName = project.base.archivesName

	from("LICENSE") {
		rename { "${it}_${archivesName.get()}" }
	}

	// exclude debug
	exclude("tytoo/grapheneuidebug/**")
	exclude("assets/graphene-ui-debug/**")
	exclude("graphene-ui-debug.mixins.json")
	// exclude tests
	exclude("tytoo/grapheneuitest/**")
}

// configure the maven publication
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = archivesBaseName
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
	}
}

// Source Browsing Helpers

val unpackedSourcesDir: Directory = layout.projectDirectory.dir("libs-src")
val minecraftCacheDirProvider: Directory = layout.projectDirectory.dir(".gradle/loom-cache/minecraftMaven")
val fabricCacheDirProvider: Directory = layout.projectDirectory.dir(".gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api")

val cleanSources by tasks.registering(Delete::class) {
	group = "help"
	description = "Deletes unpacked sources in libs-src/"
	delete(unpackedSourcesDir)
}

val unpackSources by tasks.registering(UnpackSourcesTask::class) {
	group = "help"
	description = "Unpacks library, Minecraft, and Fabric sources into libs-src/"
	dependsOn(cleanSources)
	sourceDeps.from(configurations.named("sourceDeps"))
	outputDir.set(unpackedSourcesDir)
	minecraftCacheDir.set(minecraftCacheDirProvider)
	fabricCacheDir.set(fabricCacheDirProvider)
}
