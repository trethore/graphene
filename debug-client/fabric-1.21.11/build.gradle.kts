plugins {
  id("net.fabricmc.fabric-loom-remap")
}

val targetMinecraftVersion = "1.21.11"
val loaderVersion = providers.gradleProperty("loader_version").get()
val fabricApiVersion = "0.141.4+1.21.11"
val grapheneProject = project(":packages:fabric-1.21.11")
val grapheneMainSourceSet = grapheneProject.extensions.getByType<SourceSetContainer>().named("main")
val grapheneDevelopmentJar = grapheneProject.tasks.named<Jar>("jar")

base {
  archivesName = "graphene-debug-$targetMinecraftVersion"
}

loom {
  mods {
    register("grapheneui-debug") {
      sourceSet(sourceSets.main.get())
    }
  }

  runs {
    create("debugClient") {
      client()
      sourceSet.set(sourceSets.main.get().name)
      displayName.set("Graphene Debug Client $targetMinecraftVersion")
      appendProjectPathToDisplayName.set(false)
      generateRunConfig.set(true)
      runDirectory.set(layout.projectDirectory.dir("run/client"))
      systemProperties.put(
          "graphene.debug",
          providers.gradleProperty("grapheneDebug").orElse("*"),
      )
    }
  }
}

dependencies {
  minecraft("com.mojang:minecraft:$targetMinecraftVersion")
  mappings(loom.officialMojangMappings())
  modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
  modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
  modLocalRuntime(files(grapheneDevelopmentJar))
  compileOnly(files(grapheneMainSourceSet.map { it.output }))
  implementation(project(":packages:common"))
  implementation("com.google.code.gson:gson:${providers.gradleProperty("gson_version").get()}")
}

tasks.processResources {
  from(rootProject.file("debug-client/shared/resources"))
  val properties =
      mapOf(
          "version" to project.version.toString(),
          "minecraftVersion" to targetMinecraftVersion,
          "loaderVersion" to loaderVersion,
          "fabricApiVersion" to fabricApiVersion,
      )
  inputs.properties(properties)
  filesMatching("fabric.mod.json") {
    expand(properties)
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release = 21
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}
