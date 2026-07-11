package io.github.trethore.buildlogic.unpacksources

import java.io.File

internal class CfrDecompiler(
    private val commandRunner: CommandRunner,
    private val classpath: String,
) {
    fun decompile(artifact: File, targetDir: File) {
        commandRunner.run(
            listOf(
                "java",
                "-cp",
                classpath,
                "org.benf.cfr.reader.Main",
                artifact.absolutePath,
                "--outputdir",
                targetDir.absolutePath,
                "--silent",
                "true",
            ),
        )
    }

    companion object {
        fun fromClasspath(files: Iterable<File>, commandRunner: CommandRunner): CfrDecompiler {
            val classpath = files.sortedBy(File::getAbsolutePath)
                .joinToString(File.pathSeparator) { it.absolutePath }
            return CfrDecompiler(commandRunner, classpath)
        }
    }
}
