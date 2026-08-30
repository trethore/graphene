package io.github.trethore.buildlogic.graphene

import io.github.trethore.buildlogic.architecture.ArchitectureChecksExtension
import java.util.zip.ZipFile
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.w3c.dom.Element

private const val JAVA_SOURCE_PATTERN = "**/*.java"
private const val COMMON_ARTIFACT_ID = "graphene-ui-common"

@Suppress("unused")
class FabricPackageConventionsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply("io.github.trethore.architecture-check")
    project.pluginManager.apply("io.github.trethore.maven-publishing")

    val target = project.grapheneTargetExtension()
    configureSharedSources(project)
    project.configureFabricPackageRuns()
    configureDependencies(project)
    configureArchitectureChecks(project)
    configureResources(project, target)
    configureJava(project, target)
    configureArchives(project)
    configurePublishing(project)
    configurePublicationVerification(project)
  }

  private fun configureSharedSources(project: Project) {
    val sourceSets = project.extensions.getByType<SourceSetContainer>()
    sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).configure {
      java.srcDir(project.rootProject.file("packages/minecraft-shared/src/main/java"))
      java.srcDir(project.rootProject.file("packages/fabric-shared/src/main/java"))
      resources.srcDir(project.rootProject.file("packages/fabric-shared/src/main/resources"))
    }
    sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME).configure {
      java.srcDir(project.rootProject.file("packages/minecraft-shared/src/test/java"))
      java.srcDir(project.rootProject.file("packages/fabric-shared/src/test/java"))
    }
  }

  private fun configureDependencies(project: Project) {
    val embeddedCommon =
        project.configurations.create("embeddedCommon") {
          isCanBeConsumed = false
          isCanBeResolved = false
        }
    project.configurations.named("api").configure { extendsFrom(embeddedCommon) }
    project.configurations.named("include").configure { extendsFrom(embeddedCommon) }
    project.dependencies.add(
        embeddedCommon.name,
        project.dependencies.project(project.commonProject().path),
    )
    project.addRelocatedJcefDependency("include")
  }

  private fun configureArchitectureChecks(project: Project) {
    project.extensions.configure<ArchitectureChecksExtension> {
      register("jcefIsolation") {
        sources.from(
            project.fileTree("src") { include(JAVA_SOURCE_PATTERN) },
            project.rootProject.fileTree("packages/minecraft-shared/src") {
              include(JAVA_SOURCE_PATTERN)
            },
            project.rootProject.fileTree("packages/fabric-shared/src") {
              include(JAVA_SOURCE_PATTERN)
            },
        )
        forbiddenImports.addAll(
            "org.cef.",
            "io.github.trethore.jcefgithub.",
        )
        failureMessage.set(
            "Fabric code must not access JCEF directly; use the common API instead."
        )
      }

      register("minecraftSharedLoaderIndependence") {
        sources.from(
            project.rootProject.fileTree("packages/minecraft-shared/src/main/java") {
              include(JAVA_SOURCE_PATTERN)
            }
        )
        forbiddenImports.addAll(
            "net.fabricmc.",
            "io.github.trethore.graphene.fabric.",
        )
        failureMessage.set(
            "Minecraft-shared code must not depend on a mod loader."
        )
      }
    }
  }

  private fun configureResources(project: Project, target: GrapheneTargetExtension) {
    project.tasks.named("processResources", ProcessResources::class.java) {
      project.configureFabricModExpansion(this, target)
    }
  }

  private fun configureJava(project: Project, target: GrapheneTargetExtension) {
    project.extensions.configure<JavaPluginExtension> {
      withSourcesJar()
      withJavadocJar()
    }
    project.configureJavaToolchain(target)
    project.tasks.named<Jar>("sourcesJar") {
      from(project.commonProject().file("src/main/java"))
      from(project.commonProject().file("src/main/resources"))
    }
  }

  private fun configureArchives(project: Project) {
    project.extensions.configure<BasePluginExtension> {
      archivesName.set(project.rootProject.name)
    }
    project.tasks.named<Jar>("jar") {
      val projectName = project.rootProject.name
      inputs.property("projectName", projectName)
      from(project.rootProject.file("LICENSE")) {
        rename { "${it}_$projectName" }
      }
    }

    val stageGithubRelease =
        project.tasks.register<Sync>("stageGithubRelease") {
          group = "distribution"
          description = "Stages the runtime JAR for a GitHub release."
          into(project.layout.buildDirectory.dir("github-release"))
        }

    val runtimeArchiveTaskName =
        if (project.pluginManager.hasPlugin("net.fabricmc.fabric-loom-remap")) "remapJar" else "jar"
    val projectName = project.rootProject.name
    val projectVersion = project.version.toString()
    val minecraftVersion = project.minecraftVersion()
    val targetArchiveName = "$projectName-$projectVersion-fabric-$minecraftVersion"
    val runtimeArchive = project.tasks.named<AbstractArchiveTask>(runtimeArchiveTaskName)
    runtimeArchive.configure {
      archiveFileName.set("$targetArchiveName.jar")
    }
    project.tasks.named<Jar>("sourcesJar") {
      archiveFileName.set("$targetArchiveName-sources.jar")
    }
    project.tasks.named<Jar>("javadocJar") {
      archiveFileName.set("$targetArchiveName-javadoc.jar")
    }
    stageGithubRelease.configure {
      dependsOn(runtimeArchive)
      from(runtimeArchive.flatMap { it.archiveFile })
    }
  }

  private fun configurePublishing(project: Project) {
    val publishing = project.extensions.getByType<PublishingExtension>()
    val minecraftVersion = project.minecraftVersion()
    publishing.publications.register("mavenJava", MavenPublication::class.java) {
      artifactId = "graphene-ui-$minecraftVersion"
      from(project.components.getByName("java"))
    }

    project.tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }
  }

  private fun configurePublicationVerification(project: Project) {
    val generatedPom =
        project.layout.buildDirectory.file("publications/mavenJava/pom-default.xml")
    val runtimeArchiveTaskName =
        if (project.pluginManager.hasPlugin("net.fabricmc.fabric-loom-remap")) "remapJar" else "jar"
    val runtimeArchive = project.tasks.named<AbstractArchiveTask>(runtimeArchiveTaskName)
    val runtimeArchiveFile = runtimeArchive.flatMap { it.archiveFile }
    val embeddedCommonPath = "META-INF/jars/common-${project.version}.jar"
    val commonProject = project.commonProject()
    val commonGroup = commonProject.group.toString()
    val commonVersion = commonProject.version.toString()
    val verifyMavenPublication =
        project.tasks.register("verifyMavenPublication") {
          group = LifecycleBasePlugin.VERIFICATION_GROUP
          description = "Verifies that the Maven publication is self-contained."
          dependsOn("generatePomFileForMavenJavaPublication", runtimeArchive)
          inputs.file(generatedPom)
          inputs.file(runtimeArchiveFile)

          doLast {
            verifyCommonPomDependency(
                generatedPom.get().asFile,
                commonGroup,
                commonVersion,
            )

            ZipFile(runtimeArchiveFile.get().asFile).use { runtimeJar ->
              check(runtimeJar.getEntry(embeddedCommonPath) != null) {
                "The runtime JAR does not contain $embeddedCommonPath"
              }
            }
          }
        }

    project.tasks.named("check").configure { dependsOn(verifyMavenPublication) }
  }

  private fun verifyCommonPomDependency(
      pomFile: java.io.File,
      expectedGroup: String,
      expectedVersion: String,
  ) {
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    documentBuilderFactory.isNamespaceAware = true
    documentBuilderFactory.setFeature(
        "http://apache.org/xml/features/disallow-doctype-decl",
        true,
    )
    documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")

    val document = documentBuilderFactory.newDocumentBuilder().parse(pomFile)
    val dependencies = document.getElementsByTagNameNS("*", "dependency")
    val hasCommonDependency =
        (0 until dependencies.length).any { index ->
          val dependency = dependencies.item(index) as Element
          dependency.childText("groupId") == expectedGroup &&
              dependency.childText("artifactId") == COMMON_ARTIFACT_ID &&
              dependency.childText("version") == expectedVersion &&
              dependency.childText("scope") == "compile"
        }

    check(hasCommonDependency) {
      "The Maven POM must declare $expectedGroup:$COMMON_ARTIFACT_ID:$expectedVersion " +
          "with compile scope for the consumer development classpath"
    }
  }

  private fun Element.childText(name: String): String? {
    for (index in 0 until childNodes.length) {
      val child = childNodes.item(index)
      if (child is Element && child.localName == name) {
        return child.textContent.trim()
      }
    }
    return null
  }
}
