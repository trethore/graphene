import org.gradle.api.JavaVersion
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import tytoo.graphene.UnpackSourcesTask

plugins {
	id("net.fabricmc.fabric-loom-remap")
	id("maven-publish")
	id("signing")
}

val modVersion = property("mod_version") as String
val mavenGroup = property("maven_group") as String
val archivesBaseName = property("archives_base_name") as String
val minecraftVersion = property("minecraft_version") as String
val loaderVersion = property("loader_version") as String
val fabricApiVersion = property("fabric_api_version") as String
val jcefGithubVersion = property("jcefgithub_version") as String
val junitVersion = property("junit_version") as String
val javaLanguageVersion: JavaLanguageVersion = JavaLanguageVersion.of(21)
val grapheneDebugSelector = (findProperty("grapheneDebug") as String?)
	?.trim()
	?.takeIf { it.isNotEmpty() }
val githubUsername: String? = (findProperty("gpr.user") as String?) ?: System.getenv("GITHUB_ACTOR")
val githubToken: String? = (findProperty("gpr.key") as String?) ?: System.getenv("GITHUB_TOKEN")
val githubRepository = (findProperty("gpr.repo") as String?) ?: System.getenv("GITHUB_REPOSITORY") ?: "trethore/graphene"
val mavenCentralUsername: String? = (findProperty("mavenCentralUsername") as String?)
	?: System.getenv("MAVEN_CENTRAL_USERNAME")
val mavenCentralPassword: String? = (findProperty("mavenCentralPassword") as String?)
	?: System.getenv("MAVEN_CENTRAL_PASSWORD")
val mavenCentralSigningKey: String? = (findProperty("mavenCentralSigningKey") as String?)
	?: System.getenv("MAVEN_GPG_PRIVATE_KEY")
val mavenCentralSigningPassphrase: String? = (findProperty("mavenCentralSigningPassphrase") as String?)
	?: System.getenv("MAVEN_GPG_PASSPHRASE")
val isMavenCentralPublishRequested: Boolean = gradle.startParameter.taskNames.any { taskName ->
	taskName == "publish" || taskName.contains("MavenCentral", ignoreCase = true)
}

version = modVersion
group = mavenGroup

base {
	archivesName.set(archivesBaseName)
}

repositories {
	mavenLocal()
	mavenCentral()
}

val sourceDeps: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
	isTransitive = false
	attributes {
		attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
		attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
	}
}

loom {
	splitEnvironmentSourceSets() // so client source set is created
	log4jConfigs.from(file("config/log4j2.graphene-debug.xml"))
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

	named("test") {
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
			if (grapheneDebugSelector != null) {
				property("graphene.debug", grapheneDebugSelector)
				property("fabric.log.level", "debug")
			}
		}
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${minecraftVersion}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
	// JCEF.
	implementation("io.github.trethore:jcefgithub:${jcefGithubVersion}:all-relocated") {
		isTransitive = false
	}
	include("io.github.trethore:jcefgithub:${jcefGithubVersion}:all-relocated")
	sourceDeps("io.github.trethore:jcefgithub:${jcefGithubVersion}:all-relocated")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")

	testImplementation(platform("org.junit:junit-bom:${junitVersion}"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<ProcessResources>().configureEach {
	inputs.property("version", project.version)
	filesMatching("fabric.mod.json") {
		expand("version" to project.version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(javaLanguageVersion.asInt())
}

java {
	toolchain {
		languageVersion.set(javaLanguageVersion)
	}
	sourceCompatibility = JavaVersion.toVersion(javaLanguageVersion.asInt())
	targetCompatibility = JavaVersion.toVersion(javaLanguageVersion.asInt())
	withSourcesJar()
	withJavadocJar()
}

val javaToolchainService: JavaToolchainService = extensions.getByType(JavaToolchainService::class.java)
val javaLauncherProvider: Provider<JavaLauncher> = javaToolchainService.launcherFor {
	languageVersion.set(javaLanguageVersion)
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
	javaLauncher.set(javaLauncherProvider)
}

tasks.withType<JavaExec>().configureEach {
	javaLauncher.set(javaLauncherProvider)
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
			pom {
				name.set("Graphene UI")
				description.set("Client-side Chromium-based UI library for Minecraft Fabric mods.")
				url.set("https://github.com/trethore/graphene")
				licenses {
					license {
						name.set("MIT License")
						url.set("https://github.com/trethore/graphene/blob/main/LICENSE")
					}
				}
				developers {
					developer {
						id.set("trethore")
						name.set("Titouan Rethore")
						email.set("titou.rethore@gmail.com")
					}
				}
				scm {
					connection.set("scm:git:git://github.com/trethore/graphene.git")
					developerConnection.set("scm:git:ssh://git@github.com/trethore/graphene.git")
					url.set("https://github.com/trethore/graphene")
				}
			}
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/$githubRepository")
			credentials {
				username = githubUsername
				password = githubToken
			}
		}
		maven {
			name = "MavenCentral"
			url = layout.buildDirectory.dir("central-portal/staging").get().asFile.toURI()
		}
	}
}

if (isMavenCentralPublishRequested) {
	check(!modVersion.endsWith("-SNAPSHOT")) {
		"Maven Central publishing requires a non-SNAPSHOT mod_version"
	}
	check(!mavenCentralUsername.isNullOrBlank()) {
		"Maven Central publishing requires MAVEN_CENTRAL_USERNAME or mavenCentralUsername"
	}
	check(!mavenCentralPassword.isNullOrBlank()) {
		"Maven Central publishing requires MAVEN_CENTRAL_PASSWORD or mavenCentralPassword"
	}
	check(!mavenCentralSigningKey.isNullOrBlank()) {
		"Maven Central publishing requires MAVEN_GPG_PRIVATE_KEY or mavenCentralSigningKey"
	}
	check(!mavenCentralSigningPassphrase.isNullOrBlank()) {
		"Maven Central publishing requires MAVEN_GPG_PASSPHRASE or mavenCentralSigningPassphrase"
	}
}

signing {
	setRequired { isMavenCentralPublishRequested }
	if (!mavenCentralSigningKey.isNullOrBlank() && !mavenCentralSigningPassphrase.isNullOrBlank()) {
		useInMemoryPgpKeys(mavenCentralSigningKey, mavenCentralSigningPassphrase)
		sign(publishing.publications["mavenJava"])
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
	description = "Unpacks dependency sources and clones git repos into libs-src/"
	dependsOn(cleanSources)
	sourceDeps.from(configurations.named("sourceDeps"))
	gitRepos.set(
		listOf(
			"https://github.com/trethore/jcef#master"
		)
	)
	outputDir.set(unpackedSourcesDir)
	minecraftCacheDir.set(minecraftCacheDirProvider)
	fabricCacheDir.set(fabricCacheDirProvider)
}
