package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import java.io.File

internal class ArchiveUnpacker(private val project: Project) {
    fun unpackArchive(archive: File, targetDir: File) {
        when {
            ArchiveFiles.isArchive(archive) -> project.copy {
                from(project.zipTree(archive))
                into(targetDir)
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }

            else -> project.copy {
                from(archive)
                into(targetDir)
            }
        }
    }

    fun unpackBinaryResources(archive: File, targetDir: File) {
        if (!ArchiveFiles.isArchive(archive)) {
            return
        }

        project.copy {
            from(project.zipTree(archive)) {
                exclude("**/*.class")
            }
            into(targetDir)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
