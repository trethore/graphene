plugins {
	`java-library`
	id("maven-publish")
	id("signing")
}

base {
	archivesName.set("graphene-ui-common")
}

dependencies {
	api("io.github.trethore:jcefgithub:${rootProject.property("jcefgithub_version")}:all-relocated") {
		isTransitive = false
	}
	api("com.google.code.gson:gson:2.13.2")
	implementation("org.slf4j:slf4j-api:2.0.17")
	compileOnly("org.jspecify:jspecify:1.0.0")
	compileOnly("org.lwjgl:lwjgl-glfw:${rootProject.property("lwjgl_version")}")
	testImplementation("org.lwjgl:lwjgl-glfw:${rootProject.property("lwjgl_version")}")
	testCompileOnly("org.jspecify:jspecify:1.0.0")

	testImplementation(platform("org.junit:junit-bom:${rootProject.property("junit_version")}"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val forbiddenCommonImports = listOf(
	"net.minecraft.",
	"com.mojang.",
	"net.fabricmc.",
	"org.spongepowered."
)

val checkNoMinecraftImports by tasks.registering {
	group = "verification"
	description = "Ensures common sources do not depend on Minecraft, Fabric, or Mixin APIs."

	doLast {
		fileTree("src/main/java") {
			include("**/*.java")
		}.forEach { sourceFile ->
			val sourceText = sourceFile.readText()
			val forbiddenImport = forbiddenCommonImports.firstOrNull(sourceText::contains)
			check(forbiddenImport == null) {
				"Forbidden dependency '$forbiddenImport' found in common file: ${sourceFile.relativeTo(projectDir)}"
			}
		}
	}
}

tasks.named("check") {
	dependsOn(checkNoMinecraftImports)
}
