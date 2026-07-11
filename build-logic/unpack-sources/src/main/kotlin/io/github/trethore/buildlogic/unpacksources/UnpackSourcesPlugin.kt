package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Sync

@Suppress("unused")
class UnpackSourcesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val references = project.extensions.create(
            UnpackSourcesConstants.REFERENCES_EXTENSION_NAME,
            ReferencesExtension::class.java,
        )

        project.allprojects.forEach { targetProject ->
            UnpackConfigurations.createUnpackConfiguration(targetProject)
        }
        val cfrConfiguration = UnpackConfigurations.createCfrConfiguration(project)

        project.tasks.register("cleanUnpackedSources", Delete::class.java) {
            group = UnpackSourcesConstants.TASK_GROUP
            description = "Deletes the generated references directory."
            delete(project.rootProject.layout.projectDirectory.dir(UnpackSourcesConstants.REFERENCES_DIR_NAME))
        }

        val unpackSources = project.tasks.register("unpackSources", UnpackSourcesTask::class.java) {
            group = UnpackSourcesConstants.TASK_GROUP
            description = "Unpacks selected dependency sources and Git references into references/."
            dependencyCoordinates.set(project.providers.provider { collectDependencyCoordinates(project) })
            gitReferences.set(project.providers.provider { references.gitReferences.map(GitReference::serialize) })
            unpackNestedJars.set(project.providers.provider { references.unpackNestedJars })
            cfrClasspath.from(cfrConfiguration)
            outputDirectory.set(
                project.rootProject.layout.projectDirectory.dir(UnpackSourcesConstants.REFERENCES_DIR_NAME)
            )
            outputs.upToDateWhen {
                gitReferences.get().map(GitReference::deserialize).all { reference ->
                    reference.commit != null
                }
            }
        }

        project.allprojects.forEach { targetProject ->
            targetProject.configurations.configureEach {
                if (name == UnpackSourcesConstants.LOOM_MINECRAFT_ARTIFACT_CONFIGURATION) {
                    val minecraftConfiguration = this
                    val stageMinecraftArtifact = targetProject.tasks.register(
                        UnpackSourcesConstants.STAGE_MINECRAFT_ARTIFACT_TASK_NAME,
                        Sync::class.java,
                    ) {
                        group = null
                        description = "Stages the Loom Minecraft artifact for source browsing."
                        from(minecraftConfiguration)
                        include("*.jar")
                        into(targetProject.layout.buildDirectory.dir("unpackSources/minecraftArtifact"))
                    }
                    unpackSources.configure {
                        loomMinecraftArtifacts.from(stageMinecraftArtifact)
                    }
                }
            }
        }
    }

    private fun collectDependencyCoordinates(project: Project): List<String> {
        return buildSet {
            project.allprojects.forEach { targetProject ->
                val configuration = targetProject.configurations.getByName(
                    UnpackSourcesConstants.UNPACK_CONFIGURATION_NAME
                )
                configuration.dependencies.forEach { dependency ->
                    val externalDependency = dependency as? ExternalModuleDependency
                        ?: error(
                            "unpack only supports external module dependencies; " +
                                "${targetProject.path} declared ${dependency.javaClass.simpleName}."
                        )
                    val coordinate = ModuleCoordinate.from(externalDependency)
                    add(coordinate.label)
                }
            }
        }.toList()
    }
}
