package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.Project
import java.io.File

internal class DependencySourceUnpacker(
    private val project: Project,
    private val cfrDecompiler: CfrDecompiler,
    private val options: UnpackOptions,
    loomMinecraftArtifacts: Set<File>,
) {
    private val artifactResolver = ArtifactResolver(project, loomMinecraftArtifacts)
    private val archiveUnpacker = ArchiveUnpacker(project)
    private val nestedJarUnpacker = NestedJarUnpacker(project, archiveUnpacker, cfrDecompiler)

    fun unpack(coordinate: ModuleCoordinate, referencesDir: File) {
        unpackCoordinate(coordinate, artifactResolver.resolveBinaryArtifact(coordinate), referencesDir)
    }

    private fun unpackCoordinate(
        coordinate: ModuleCoordinate,
        binaryArtifact: File?,
        referencesDir: File,
    ) {
        val targetName = ReferencePaths.safePathSegment(
            "${coordinate.group}-${coordinate.name}-${coordinate.version}"
        )
        val targetDir = referencesDir.resolve(targetName)

        project.delete(targetDir)
        targetDir.mkdirs()

        val relativeTargetDir = ReferencePaths.relativeToRoot(project, targetDir)
        val sourceArtifact = artifactResolver.resolveSourceArtifact(coordinate)
        if (sourceArtifact != null) {
            project.logger.lifecycle("Unpacking sources for ${coordinate.label} -> $relativeTargetDir")
            archiveUnpacker.unpackArchive(sourceArtifact, targetDir)
            if (binaryArtifact != null) {
                archiveUnpacker.unpackBinaryResources(binaryArtifact, targetDir)
                nestedJarUnpacker.unpackIfRequested(binaryArtifact, targetDir, options)
            }
            return
        }

        require(binaryArtifact != null) {
            "Could not resolve binary artifact for ${coordinate.label}."
        }

        project.logger.lifecycle(
            "No source jar for ${coordinate.label}; decompiling with CFR -> $relativeTargetDir"
        )
        archiveUnpacker.unpackBinaryResources(binaryArtifact, targetDir)
        cfrDecompiler.decompile(binaryArtifact, targetDir)
        nestedJarUnpacker.unpackIfRequested(binaryArtifact, targetDir, options)
    }
}
