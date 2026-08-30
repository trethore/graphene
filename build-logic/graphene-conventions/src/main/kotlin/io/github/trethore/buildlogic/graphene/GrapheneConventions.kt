package io.github.trethore.buildlogic.graphene

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.gradle.language.jvm.tasks.ProcessResources

internal fun Project.grapheneTargetExtension(): GrapheneTargetExtension =
    extensions.create("grapheneTarget", GrapheneTargetExtension::class.java)

internal fun Project.loomExtension(): LoomGradleExtensionAPI = extensions.getByType()

internal fun Project.commonProject(): Project = project(":packages:common")

internal fun Project.fabricTargetName(): String {
  val targetName = projectDir.name
  check(targetName.startsWith("fabric-")) {
    "Fabric project directory names must start with 'fabric-': $targetName"
  }
  return targetName
}

internal fun Project.minecraftVersion(): String = fabricTargetName().removePrefix("fabric-")

internal fun Project.versionCatalog(): VersionCatalog =
    extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureJavaToolchain(target: GrapheneTargetExtension) {
  extensions.getByType<JavaPluginExtension>().toolchain.languageVersion.set(
      target.javaVersion.map(JavaLanguageVersion::of)
  )
}

internal fun Project.configureFabricModExpansion(
    task: ProcessResources,
    target: GrapheneTargetExtension,
) {
  val catalog = versionCatalog()
  val resourceProperties =
      mapOf(
          "version" to version.toString(),
          "minecraftVersion" to minecraftVersion(),
          "loaderVersion" to catalog.findVersion("fabric-loader").get().requiredVersion,
          "fabricApiVersion" to target.fabricApiVersion.get(),
          "javaVersion" to target.javaVersion.get(),
      )
  task.inputs.properties(resourceProperties)
  task.filesMatching("fabric.mod.json") {
    expand(resourceProperties)
  }
}

internal fun Project.addRelocatedJcefDependency(configuration: String) {
  dependencies.addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>(
      configuration,
      versionCatalog().findLibrary("jcefgithub").get(),
  ) {
    isTransitive = false
    artifact { classifier = "all-relocated" }
  }
}

internal fun Project.configureLoomDefaults(): LoomGradleExtensionAPI {
  val loom = loomExtension()
  loom.runs.configureEach {
    preferGradleTask.set(true)
    systemProperties.put("fabric.log.disableAnsi", "false")
  }
  return loom
}

internal fun Project.configureFabricPackageRuns() {
  val loom = configureLoomDefaults()
  loom.runs.named("client").configure {
    displayName.set("Minecraft Client (Fabric ${minecraftVersion()})")
    appendProjectPathToDisplayName.set(false)
    generateRunConfig.set(true)
    runDirectory.set(layout.projectDirectory.dir("run/client"))
  }
  loom.runs.named("server").configure {
    displayName.set("Minecraft Server (Fabric ${minecraftVersion()})")
    appendProjectPathToDisplayName.set(false)
    generateRunConfig.set(true)
    runDirectory.set(layout.projectDirectory.dir("run/server"))
  }
}
