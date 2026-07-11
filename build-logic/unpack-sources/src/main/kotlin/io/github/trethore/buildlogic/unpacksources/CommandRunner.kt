package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.Project

internal class CommandRunner(private val project: Project) {
    fun run(command: List<String>) {
        val process = ProcessBuilder(command)
            .directory(project.rootDir)
            .inheritIO()
            .start()

        val exitCode = process.waitFor()
        require(exitCode == 0) {
            "Command failed with exit code $exitCode: ${command.joinToString(" ")}"
        }
    }
}
