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

loom {
	splitEnvironmentSourceSets() // so client source set is created
}

val clientSS: NamedDomainObjectProvider<SourceSet> = sourceSets.named("client")
sourceSets {
	create("debug") {
		java.srcDir("src/debug/java")
		resources.srcDir("src/debug/resources")
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

tasks.named<ProcessResources>("processClientResources") {
	val version = project.version
	inputs.property("version", version)
	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.named<ProcessResources>("processDebugResources") {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	val version = project.version
	inputs.property("version", version)
	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val archivesName = project.base.archivesName

	from("LICENSE") {
		rename { "${it}_${archivesName.get()}" }
	}
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
