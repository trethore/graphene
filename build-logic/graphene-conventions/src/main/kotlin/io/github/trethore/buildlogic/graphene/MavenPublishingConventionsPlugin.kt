package io.github.trethore.buildlogic.graphene

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

@Suppress("unused")
class MavenPublishingConventionsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply("maven-publish")
    project.pluginManager.apply("signing")

    val publishing = project.extensions.getByType<PublishingExtension>()
    configurePublications(publishing)
    configureRepositories(project, publishing)
    configureSigning(project, publishing)
  }

  private fun configurePublications(publishing: PublishingExtension) {
    publishing.publications.withType<MavenPublication>().configureEach {
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

  private fun configureRepositories(project: Project, publishing: PublishingExtension) {
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
  }

  private fun configureSigning(project: Project, publishing: PublishingExtension) {
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
        publishing.publications.withType<MavenPublication>().configureEach {
          sign(this)
        }
      }
    }
  }
}
