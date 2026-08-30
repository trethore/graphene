package io.github.trethore.buildlogic.graphene

import io.github.trethore.buildlogic.architecture.ArchitectureChecksExtension
import java.util.zip.ZipFile
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
import org.gradle.plugins.signing.SigningExtension

private const val JAVA_SOURCE_PATTERN = "**/*.java"

@Suppress("unused")
class FabricPackageConventionsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply("io.github.trethore.architecture-check")
    project.pluginManager.apply("maven-publish")
    project.pluginManager.apply("signing")

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
    val commonDependency = project.dependencies.project(project.commonProject().path)
    project.dependencies.add("compileOnly", commonDependency)
    project.dependencies.add("testImplementation", commonDependency)
    project.dependencies.add("include", commonDependency)
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

  private fun configurePublicationVerification(project: Project) {
    val generatedPom =
        project.layout.buildDirectory.file("publications/mavenJava/pom-default.xml")
    val runtimeArchiveTaskName =
        if (project.pluginManager.hasPlugin("net.fabricmc.fabric-loom-remap")) "remapJar" else "jar"
    val runtimeArchive = project.tasks.named<AbstractArchiveTask>(runtimeArchiveTaskName)
    val runtimeArchiveFile = runtimeArchive.flatMap { it.archiveFile }
    val embeddedCommonPath = "META-INF/jars/common-${project.version}.jar"
    val verifyMavenPublication =
        project.tasks.register("verifyMavenPublication") {
          group = LifecycleBasePlugin.VERIFICATION_GROUP
          description = "Verifies that the Maven publication is self-contained."
          dependsOn("generatePomFileForMavenJavaPublication", runtimeArchive)
          inputs.file(generatedPom)
          inputs.file(runtimeArchiveFile)

          doLast {
            val pomContents = generatedPom.get().asFile.readText()
            check("<artifactId>common</artifactId>" !in pomContents) {
              "The Maven POM must not declare the embedded common project as a dependency"
            }

            ZipFile(runtimeArchiveFile.get().asFile).use { runtimeJar ->
              check(runtimeJar.getEntry(embeddedCommonPath) != null) {
                "The runtime JAR does not contain $embeddedCommonPath"
              }
            }
          }
        }

    project.tasks.named("check").configure { dependsOn(verifyMavenPublication) }
  }
}
