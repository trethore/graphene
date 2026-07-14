package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.Project
import java.io.File
import java.net.URI

internal class GitReferenceUnpacker(private val project: Project) {
    private val commandRunner = CommandRunner(project)

    fun unpackAll(gitReferences: Iterable<GitReference>, referencesDir: File) {
        gitReferences.forEach { gitReference ->
            unpack(gitReference, referencesDir)
        }
    }

    private fun unpack(gitReference: GitReference, referencesDir: File) {
        val revision = gitReference.commit ?: gitReference.branch
        val targetDir = targetDirectory(gitReference, referencesDir)

        project.delete(targetDir)
        targetDir.parentFile.mkdirs()

        val cloneArgs = mutableListOf(
            "clone",
            "--branch",
            gitReference.branch,
            "--depth",
            "1",
            "--single-branch",
            "--no-checkout",
        )
        if (gitReference.sparsePaths.isNotEmpty()) {
            cloneArgs += "--filter=blob:none"
        }
        cloneArgs += listOf(gitReference.url, targetDir.absolutePath)

        val relativeTargetDir = ReferencePaths.relativeToRoot(project, targetDir)
        project.logger.lifecycle("Cloning ${gitReference.url} (${gitReference.branch}) -> $relativeTargetDir")
        commandRunner.run(listOf("git") + cloneArgs)

        if (gitReference.commit != null) {
            val fetchArgs = mutableListOf(
                "git",
                "-C",
                targetDir.absolutePath,
                "fetch",
                "--depth",
                "1",
            )
            if (gitReference.sparsePaths.isNotEmpty()) {
                fetchArgs += "--filter=blob:none"
            }
            fetchArgs += listOf("origin", gitReference.commit)
            commandRunner.run(fetchArgs)
        }

        if (gitReference.sparsePaths.isNotEmpty()) {
            val patterns = gitReference.sparsePaths
                .flatMap { path -> listOf("/$path", "/$path/**") }
                .distinct()
            commandRunner.run(
                listOf(
                    "git",
                    "-C",
                    targetDir.absolutePath,
                    "sparse-checkout",
                    "set",
                    "--no-cone",
                ) + patterns
            )
        }

        commandRunner.run(
            listOf(
                "git",
                "-C",
                targetDir.absolutePath,
                "checkout",
                "--detach",
                revision,
            )
        )
    }

    companion object {
        fun targetDirectory(gitReference: GitReference, referencesDir: File): File {
            val revision = gitReference.commit ?: gitReference.branch
            val label = parseGitLabel(gitReference)
            return referencesDir.resolve(ReferencePaths.safePathSegment("$label-$revision"))
        }

        private fun parseGitLabel(gitReference: GitReference): String {
            val path = gitPath(gitReference.url)
            val segments = path
                .trim('/')
                .split('/')
                .filter { it.isNotBlank() }

            if (segments.size >= 2) {
                val owner = segments[segments.size - 2]
                val repo = segments.last().removeSuffix(".git")
                return "$owner-$repo"
            }

            return gitReference.name?.takeIf { it.isNotBlank() }
                ?: segments.lastOrNull()?.removeSuffix(".git")?.takeIf { it.isNotBlank() }
                ?: "git-reference"
        }

        private fun gitPath(url: String): String {
            return try {
                URI(url).path.takeIf { !it.isNullOrBlank() } ?: url.substringAfter(':', url)
            } catch (_: Exception) {
                url.substringAfter(':', url)
            }
        }
    }
}
