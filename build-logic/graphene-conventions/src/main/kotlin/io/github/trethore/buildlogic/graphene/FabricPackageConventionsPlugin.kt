package io.github.trethore.buildlogic.graphene

import io.github.trethore.buildlogic.architecture.ArchitectureChecksExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
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
import org.gradle.plugins.signing.SigningExtension

@Suppress("unused")
class FabricPackageConventionsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply("io.github.trethore.architecture-check")
    project.pluginManager.apply("maven-publish")
    project.pluginManager.apply("signing")

    val target = project.grapheneTargetExtension()
    project.configureFabricPackageRuns()
    configureDependencies(project)
    configureArchitectureChecks(project)
    configureResources(project, target)
    configureJava(project, target)
    configureArchives(project)
    configurePublishing(project)
  }

  private fun configureDependencies(project: Project) {
    val embeddedCommon =
        project.configurations.create("embeddedCommon") {
          isCanBeConsumed = false
          isCanBeResolved = false
        }
    project.configurations.named("implementation").configure { extendsFrom(embeddedCommon) }
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
        sources.from(project.fileTree("src") { include("**/*.java") })
        forbiddenImports.addAll(
            "org.cef.",
            "io.github.trethore.jcefgithub.",
        )
        failureMessage.set(
            "Fabric code must not access JCEF directly; use the common API instead."
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
    val runtimeArchive = project.tasks.named<AbstractArchiveTask>(runtimeArchiveTaskName)
    runtimeArchive.configure {
      archiveFileName.set("$projectName-$projectVersion-fabric-$minecraftVersion.jar")
    }
    stageGithubRelease.configure {
      dependsOn(runtimeArchive)
      from(runtimeArchive.flatMap { it.archiveFile })
    }
  }

  private fun configurePublishing(project: Project) {
    val publishing = project.extensions.getByType<PublishingExtension>()
    val projectGroup = project.group.toString()
    val minecraftVersion = project.minecraftVersion()
    val publication =
        publishing.publications.register("mavenJava", MavenPublication::class.java) {
          artifactId = "graphene-ui-$minecraftVersion"
          from(project.components.getByName("java"))
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
            withXml {
              val dependencies = asNode().get("dependencies") as groovy.util.NodeList
              dependencies
                  .flatMap { (it as groovy.util.Node).children() }
                  .filterIsInstance<groovy.util.Node>()
                  .filter { dependency ->
                    val groupId = dependency.get("groupId") as groovy.util.NodeList
                    val artifactId = dependency.get("artifactId") as groovy.util.NodeList
                    groupId.text() == projectGroup && artifactId.text() == "common"
                  }
                  .forEach { dependency -> dependency.parent().remove(dependency) }
            }
          }
        }

    publishing.repositories {
      maven {
        name = "MavenCentralBundle"
        url = project.rootProject.layout.buildDirectory
            .dir("central-portal/staging")
            .get()
            .asFile
            .toURI()
      }
      maven {
        name = "GitHubPackages"
        url = project.uri("https://maven.pkg.github.com/trethore/graphene")
        credentials {
          username = project.providers.environmentVariable("GITHUB_ACTOR").orNull
          password = project.providers.environmentVariable("GITHUB_TOKEN").orNull
        }
      }
    }

    project.tasks.withType<GenerateModuleMetadata>().configureEach { enabled = false }

    val signingKey = project.providers.environmentVariable("MAVEN_GPG_PRIVATE_KEY")
    val signingPassphrase = project.providers.environmentVariable("MAVEN_GPG_PASSPHRASE")
    val centralPublishRequested =
        project.gradle.startParameter.taskNames.any {
          it.contains("MavenCentralBundle", ignoreCase = true)
        }

    if (centralPublishRequested) {
      check(!project.version.toString().endsWith("-SNAPSHOT")) {
        "Maven Central publishing requires a non-SNAPSHOT release version"
      }
      check(signingKey.isPresent) {
        "Maven Central publishing requires MAVEN_GPG_PRIVATE_KEY"
      }
      check(signingPassphrase.isPresent) {
        "Maven Central publishing requires MAVEN_GPG_PASSPHRASE"
      }
    }

    project.extensions.configure<SigningExtension> {
      setRequired { centralPublishRequested }
      if (signingKey.isPresent && signingPassphrase.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassphrase.get())
        sign(publication.get())
      }
    }
  }
}
