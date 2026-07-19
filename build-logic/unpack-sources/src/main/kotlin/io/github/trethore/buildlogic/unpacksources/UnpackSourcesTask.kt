package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class UnpackSourcesTask : DefaultTask() {
    @get:Input
    abstract val dependencyCoordinates: ListProperty<String>

    @get:Input
    abstract val gitReferences: ListProperty<String>

    @get:Input
    abstract val unpackNestedJars: Property<Boolean>

    @get:Classpath
    abstract val cfrClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val loomMinecraftArtifacts: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun unpack() {
        val referencesDir = outputDirectory.get().asFile
        project.delete(referencesDir)
        referencesDir.mkdirs()

        val cfrDecompiler = CfrDecompiler.fromClasspath(
            cfrClasspath.files,
            CommandRunner(project),
        )
        val options = UnpackOptions(unpackNestedJars.get())
        val dependencySourceUnpacker = DependencySourceUnpacker(
            project,
            cfrDecompiler,
            options,
            loomMinecraftArtifacts.files,
        )

        dependencyCoordinates.get().sorted().forEach { coordinateLabel ->
            dependencySourceUnpacker.unpack(ModuleCoordinate.parse(coordinateLabel), referencesDir)
        }

        GitReferenceUnpacker(project).unpackAll(
            gitReferences.get().map(GitReference::deserialize),
            referencesDir,
        )
    }
}
