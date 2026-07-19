package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class UnpackGitReferencesTask : DefaultTask() {
    @get:Input
    abstract val gitReferences: ListProperty<String>

    @get:Internal
    abstract val referencesDirectory: DirectoryProperty

    @TaskAction
    fun unpack() {
        val referencesDir = referencesDirectory.get().asFile
        referencesDir.mkdirs()
        GitReferenceUnpacker(project).unpackAll(
            gitReferences.get().map(GitReference::deserialize),
            referencesDir,
        )
    }
}
