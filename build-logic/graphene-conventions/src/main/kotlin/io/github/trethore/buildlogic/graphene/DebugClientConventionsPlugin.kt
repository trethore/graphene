package io.github.trethore.buildlogic.graphene

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources

@Suppress("unused")
class DebugClientConventionsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val target = project.grapheneTargetExtension()
    val targetName = project.fabricTargetName()
    val grapheneProject = project.project(":packages:$targetName")
    val grapheneMainSourceSet =
        grapheneProject.extensions
            .getByType<SourceSetContainer>()
            .named(SourceSet.MAIN_SOURCE_SET_NAME)
    val commonMainSourceSet =
        project
            .commonProject()
            .extensions
            .getByType<SourceSetContainer>()
            .named(SourceSet.MAIN_SOURCE_SET_NAME)
    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    val grapheneRuntimeSourceSet =
        sourceSets.create("grapheneRuntime") {
          resources {
            srcDir(grapheneProject.layout.projectDirectory.dir("src/main/resources"))
            include("fabric.mod.json")
          }
        }

    project.extensions.configure<BasePluginExtension> {
      archivesName.set("graphene-debug-${project.minecraftVersion()}")
    }
    project.configureJavaToolchain(target)
    configureLoom(project, sourceSets, grapheneRuntimeSourceSet)

    project.dependencies.add(
        "implementation",
        project.files(grapheneMainSourceSet.map { it.output }),
    )
    project.dependencies.add(
        "implementation",
        project.files(commonMainSourceSet.map { it.output }),
    )
    project.dependencies.addProvider(
        "implementation",
        project.versionCatalog().findLibrary("gson").get(),
    )
    project.addRelocatedJcefDependency("runtimeOnly")
    project.dependencies.add("runtimeOnly", grapheneRuntimeSourceSet.output)

    project.tasks.named<ProcessResources>("processResources") {
      from(project.rootProject.file("debug-client/shared/resources"))
      project.configureFabricModExpansion(this, target)
    }
    project.tasks.named<ProcessResources>(grapheneRuntimeSourceSet.processResourcesTaskName) {
      project.configureFabricModExpansion(this, target)
    }
    project.tasks.named("classes") {
      dependsOn(grapheneRuntimeSourceSet.classesTaskName)
    }
    project.tasks.matching { it.name.startsWith("genSourcesWith") }.configureEach {
      dependsOn(grapheneProject.tasks.named(name))
      enabled = false
    }
  }

  private fun configureLoom(
      project: Project,
      sourceSets: SourceSetContainer,
      grapheneRuntimeSourceSet: SourceSet,
  ) {
    val loom = project.configureLoomDefaults()
    loom.mods.register("grapheneui") {
      sourceSet(grapheneRuntimeSourceSet)
    }
    loom.mods.register("grapheneui-debug") {
      sourceSet(sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get())
    }
    loom.runs.register("debugClient") {
      client()
      sourceSet.set(sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).map { it.name })
      displayName.set("Graphene Debug Client ${project.minecraftVersion()}")
      appendProjectPathToDisplayName.set(false)
      generateRunConfig.set(true)
      runDirectory.set(project.layout.projectDirectory.dir("run/client"))
    }
  }
}
