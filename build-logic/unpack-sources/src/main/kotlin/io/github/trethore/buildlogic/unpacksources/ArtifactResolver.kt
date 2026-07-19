package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolveException
import java.io.File

internal class ArtifactResolver(
    private val project: Project,
    private val loomMinecraftArtifacts: Set<File>,
) {
    fun resolveSourceArtifact(coordinate: ModuleCoordinate): File? {
        if (isMinecraft(coordinate)) {
            return null
        }
        val sourceDependency = project.dependencies.create(
            "${coordinate.label}:sources@jar",
        ) as ModuleDependency
        sourceDependency.isTransitive = false
        val configuration = project.configurations.detachedConfiguration(sourceDependency)
        configuration.isTransitive = false

        return try {
            val files = configuration.resolve().toList()
            require(files.size <= 1) {
                "Expected one source artifact for ${coordinate.label}, found ${files.size}."
            }
            files.singleOrNull()
        } catch (exception: ResolveException) {
            if (isMissingArtifact(exception)) {
                null
            } else {
                throw GradleException("Could not resolve sources for ${coordinate.label}.", exception)
            }
        }
    }

    fun resolveBinaryArtifact(coordinate: ModuleCoordinate): File? {
        return resolveMinecraftArtifactFromLoom(coordinate)
            ?: resolveSingleArtifact(coordinate)
    }

    private fun resolveSingleArtifact(coordinate: ModuleCoordinate): File? {
        val artifactDependency = project.dependencies.create(
            coordinate.label,
        ) as ModuleDependency
        artifactDependency.isTransitive = false

        val configuration = project.configurations.detachedConfiguration(artifactDependency)
        configuration.isTransitive = false

        val files = configuration.resolve().toList()
        require(files.size <= 1) {
            "Expected one binary artifact for ${coordinate.label}, found ${files.size}."
        }
        return files.singleOrNull()
    }

    private fun resolveMinecraftArtifactFromLoom(coordinate: ModuleCoordinate): File? {
        if (!isMinecraft(coordinate)) {
            return null
        }

        val resolvedFiles = loomMinecraftArtifacts
            .flatMap { file ->
                when {
                    file.isDirectory -> file.walkTopDown().toList()
                    else -> listOf(file)
                }
            }
            .filter { file -> file.isFile && file.extension.equals("jar", ignoreCase = true) }
            .toList()

        return resolvedFiles.firstOrNull { file ->
            file.name.contains(coordinate.name) && file.name.contains(coordinate.version)
        } ?: resolvedFiles.firstOrNull { file ->
            file.name.contains(coordinate.name)
        }
    }

    private fun isMissingArtifact(failure: Throwable): Boolean {
        var cause: Throwable? = failure
        while (cause != null) {
            if (cause.javaClass.simpleName == "ArtifactNotFoundException") {
                return true
            }
            cause = cause.cause
        }
        return false
    }

    private fun isMinecraft(coordinate: ModuleCoordinate): Boolean {
        return coordinate.group == "com.mojang" && coordinate.name == "minecraft"
    }
}
