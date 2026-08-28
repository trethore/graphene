plugins {
  alias(libs.plugins.fabric.loom)
}

val javaVersion = JavaLanguageVersion.of(25)
val loaderVersion = libs.versions.fabric.loader.get()
val targetMinecraftVersion = libs.versions.minecraft.v262.get()
val fabricApiVersion = libs.versions.fabric.api.v262.get()
val grapheneProject = project(":packages:fabric-26.2")
val mainSourceSet = "main"
val grapheneMainSourceSet =
    grapheneProject.extensions.getByType<SourceSetContainer>().named(mainSourceSet)
val commonProject = project(":packages:common")
val commonMainSourceSet =
    commonProject.extensions.getByType<SourceSetContainer>().named(mainSourceSet)
val grapheneRuntimeSourceSet =
    sourceSets.create("grapheneRuntime") {
      resources {
        srcDir(grapheneProject.layout.projectDirectory.dir("src/main/resources"))
        include("fabric.mod.json")
      }
    }

fun ProcessResources.expandFabricModProperties() {
  val resourceProperties =
      mapOf(
          "version" to project.version.toString(),
          "minecraftVersion" to targetMinecraftVersion,
          "loaderVersion" to loaderVersion,
          "fabricApiVersion" to fabricApiVersion,
      )
  inputs.properties(resourceProperties)
  filesMatching("fabric.mod.json") {
    expand(resourceProperties)
  }
}

base {
  archivesName = "graphene-debug-$targetMinecraftVersion"
}

loom {
  runs.configureEach {
    preferGradleTask.set(true)
    systemProperties.put("fabric.log.disableAnsi", "false")
  }

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
  runtimeOnly(libs.jcefgithub) {
    isTransitive = false
    artifact {
      classifier = "all-relocated"
    }
  }
  runtimeOnly(grapheneRuntimeSourceSet.output)
}

tasks.processResources {
  from(rootProject.file("debug-client/shared/resources"))
  expandFabricModProperties()
}

tasks.named<ProcessResources>(grapheneRuntimeSourceSet.processResourcesTaskName) {
  expandFabricModProperties()
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

java {
  toolchain.languageVersion.set(javaVersion)
}
