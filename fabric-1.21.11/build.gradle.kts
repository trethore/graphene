import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.publish.maven.MavenPublication
import tytoo.graphene.UnpackSourcesTask

plugins {
	id("net.fabricmc.fabric-loom-remap")
	id("maven-publish")
	id("signing")
}

val modVersion = rootProject.property("mod_version") as String
val mavenGroup = rootProject.property("maven_group") as String
val archivesBaseName = rootProject.property("archives_base_name") as String
val minecraftVersion = property("minecraft_version") as String
val loaderVersion = rootProject.property("loader_version") as String
val fabricApiVersion = property("fabric_api_version") as String
val jcefGithubVersion = rootProject.property("jcefgithub_version") as String
val junitVersion = rootProject.property("junit_version") as String
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
	archivesName.set("$archivesBaseName-fabric-$minecraftVersion")
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
	splitEnvironmentSourceSets()
	log4jConfigs.from(rootProject.file("config/log4j2.graphene-debug.xml"))
}

val clientSS: NamedDomainObjectProvider<SourceSet> = sourceSets.named("client")
sourceSets {
	create("debug") {
		java.setSrcDirs(listOf("src/debug/java"))
		resources.setSrcDirs(listOf("src/debug/resources"))
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
	minecraft("com.mojang:minecraft:$minecraftVersion")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

	implementation(project(":common"))
	include(project(":common"))

	implementation("io.github.trethore:jcefgithub:$jcefGithubVersion:all-relocated") {
		isTransitive = false
	}
	include("io.github.trethore:jcefgithub:$jcefGithubVersion:all-relocated")
	sourceDeps("io.github.trethore:jcefgithub:$jcefGithubVersion:sources")

	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

	testImplementation(platform("org.junit:junit-bom:$junitVersion"))
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

	from(rootProject.file("LICENSE")) {
		rename { "${it}_${archivesName.get()}" }
	}

	exclude("tytoo/grapheneuidebug/**")
	exclude("assets/graphene-ui-debug/**")
	exclude("graphene-ui-debug.mixins.json")
	exclude("tytoo/grapheneuitest/**")
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = "$archivesBaseName-fabric-$minecraftVersion"
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

val unpackedSourcesDir: Directory = rootProject.layout.projectDirectory.dir("references")
val minecraftCacheDirProvider: Directory = rootProject.layout.projectDirectory.dir(".gradle/loom-cache/minecraftMaven")
val fabricCacheDirProvider: Directory = rootProject.layout.projectDirectory.dir(".gradle/loom-cache/remapped_mods/remapped/net/fabricmc/fabric-api")

val cleanSources by tasks.registering(Delete::class) {
	group = "help"
	description = "Deletes unpacked sources in references/"
	delete(unpackedSourcesDir)
}

val unpackSources by tasks.registering(UnpackSourcesTask::class) {
	group = "help"
	description = "Unpacks dependency sources and clones git repos into references/"
	dependsOn(cleanSources)
	sourceDeps.from(configurations.named("sourceDeps"))
	gitRepos.set(
		listOf(
			"https://github.com/trethore/jcef#master",
			"https://github.com/chromiumembedded/cef"
		)
	)
	outputDir.set(unpackedSourcesDir)
	minecraftCacheDir.set(minecraftCacheDirProvider)
	fabricCacheDir.set(fabricCacheDirProvider)
}
