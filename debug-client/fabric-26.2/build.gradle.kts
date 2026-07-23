plugins {
  alias(libs.plugins.fabric.loom)
}

val targetMinecraftVersion = "26.2"
val loaderVersion = libs.versions.fabric.loader.get()
val fabricApiVersion = "0.155.2+26.2"
val grapheneProject = project(":packages:fabric-26.2")
val grapheneMainSourceSet = grapheneProject.extensions.getByType<SourceSetContainer>().named("main")
val commonProject = project(":packages:common")
val commonMainSourceSet = commonProject.extensions.getByType<SourceSetContainer>().named("main")
val jcefGithubVersion = libs.versions.jcefgithub.get()
val resourceProperties =
    mapOf(
        "version" to project.version.toString(),
        "minecraftVersion" to targetMinecraftVersion,
        "loaderVersion" to loaderVersion,
        "fabricApiVersion" to fabricApiVersion,
    )
val grapheneRuntimeSourceSet =
    sourceSets.create("grapheneRuntime") {
      resources {
        srcDir(grapheneProject.layout.projectDirectory.dir("src/main/resources"))
        include("fabric.mod.json")
      }
    }

base {
  archivesName = "graphene-debug-$targetMinecraftVersion"
}

loom {
  mods {
    register("grapheneui") {
      sourceSet(grapheneRuntimeSourceSet)
    }
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
    }
  }
}

dependencies {
  minecraft("com.mojang:minecraft:$targetMinecraftVersion")
  implementation(libs.fabric.loader)
  implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
  implementation(files(grapheneMainSourceSet.map { it.output }))
  implementation(files(commonMainSourceSet.map { it.output }))
  implementation(libs.gson)
  runtimeOnly("io.github.trethore:jcefgithub:${jcefGithubVersion}:all-relocated") {
    isTransitive = false
  }
  runtimeOnly(grapheneRuntimeSourceSet.output)
}

tasks.processResources {
  from(rootProject.file("debug-client/shared/resources"))
  inputs.properties(resourceProperties)
  filesMatching("fabric.mod.json") {
    expand(resourceProperties)
  }
}

tasks.named<ProcessResources>(grapheneRuntimeSourceSet.processResourcesTaskName) {
  inputs.properties(resourceProperties)
  filesMatching("fabric.mod.json") {
    expand(resourceProperties)
  }
}

tasks.classes {
  dependsOn(grapheneRuntimeSourceSet.classesTaskName)
}

tasks
    .matching { it.name.startsWith("genSourcesWith") }
    .configureEach {
      dependsOn(grapheneProject.tasks.named(name))
      enabled = false
    }

tasks.withType<JavaCompile>().configureEach {
  options.release = 25
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
  sourceCompatibility = JavaVersion.VERSION_25
  targetCompatibility = JavaVersion.VERSION_25
}
