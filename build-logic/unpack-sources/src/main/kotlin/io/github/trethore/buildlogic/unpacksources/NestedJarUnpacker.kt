package io.github.trethore.buildlogic.unpacksources

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal class NestedJarUnpacker(
    private val project: Project,
    private val archiveUnpacker: ArchiveUnpacker,
    private val cfrDecompiler: CfrDecompiler,
) {
    fun unpackIfRequested(archive: File, targetDir: File, options: UnpackOptions) {
        if (!options.unpackNestedJars || !ArchiveFiles.isArchive(archive)) {
            return
        }
        unpackNestedJars(archive, targetDir, 0, setOf(fileHash(archive)))
    }

    private fun unpackNestedJars(
        archive: File,
        targetDir: File,
        depth: Int,
        ancestorHashes: Set<String>,
    ) {
        ZipFile(archive).use { zipFile ->
            val nestedEntries = findNestedJarEntries(zipFile, archive)
            if (nestedEntries.isEmpty()) {
                return
            }
            if (depth >= UnpackSourcesConstants.MAX_NESTED_JAR_DEPTH) {
                project.logger.warn(
                    "Skipping nested jars in ${archive.name}: maximum depth " +
                        "${UnpackSourcesConstants.MAX_NESTED_JAR_DEPTH} reached."
                )
                return
            }

            nestedEntries.forEach { entry ->
                val nestedJar = extractNestedJar(zipFile, entry, archive)
                val nestedHash = fileHash(nestedJar)
                if (nestedHash in ancestorHashes) {
                    project.logger.warn("Skipping recursive nested jar ${entry.name} in ${archive.name}.")
                    return@forEach
                }

                val nestedFileName = File(entry.name).name
                val outputName = ReferencePaths.safePathSegment(
                    "${nestedFileName.substringBeforeLast('.')}-${ReferencePaths.shortHash(entry.name)}"
                )
                val nestedTargetDir = targetDir
                    .resolve(UnpackSourcesConstants.NESTED_OUTPUT_DIR)
                    .resolve(outputName)

                nestedTargetDir.mkdirs()
                val relativeNestedTargetDir = ReferencePaths.relativeToRoot(project, nestedTargetDir)
                project.logger.lifecycle("Decompiling nested jar ${entry.name} -> $relativeNestedTargetDir")
                archiveUnpacker.unpackBinaryResources(nestedJar, nestedTargetDir)
                cfrDecompiler.decompile(nestedJar, nestedTargetDir)
                unpackNestedJars(
                    nestedJar,
                    nestedTargetDir,
                    depth + 1,
                    ancestorHashes + nestedHash,
                )
            }
        }
    }

    private fun findNestedJarEntries(zipFile: ZipFile, archive: File): List<ZipEntry> {
        val declaredPaths = fabricNestedJarPaths(zipFile, archive)
        val entries = declaredPaths?.map { path ->
            zipFile.getEntry(path)
                ?: throw GradleException("${archive.name} declares missing nested jar $path.")
        } ?: zipFile.entries().asSequence()
            .filter { entry -> !entry.isDirectory && entry.name.endsWith(".jar", ignoreCase = true) }
            .toList()
        return entries
            .filter { entry -> !entry.isDirectory && entry.name.endsWith(".jar", ignoreCase = true) }
            .sortedBy(ZipEntry::getName)
    }

    private fun fabricNestedJarPaths(zipFile: ZipFile, archive: File): List<String>? {
        val metadataEntry = zipFile.getEntry(UnpackSourcesConstants.FABRIC_MOD_JSON) ?: return null
        val metadata = zipFile.getInputStream(metadataEntry).bufferedReader().use { reader ->
            JsonSlurper().parse(reader) as? Map<*, *>
                ?: throw GradleException("${archive.name} has an invalid fabric.mod.json object.")
        }
        val jars = metadata["jars"] ?: return emptyList()
        require(jars is List<*>) { "${archive.name} has a non-array fabric.mod.json jars property." }
        return jars.mapIndexed { index, value ->
            val jar = value as? Map<*, *>
                ?: throw GradleException("${archive.name} has an invalid jars[$index] entry.")
            val path = jar["file"] as? String
                ?: throw GradleException("${archive.name} jars[$index] does not declare a file.")
            path.removePrefix("/")
        }.distinct()
    }

    private fun extractNestedJar(zipFile: ZipFile, entry: ZipEntry, parentArchive: File): File {
        val parentKey = ReferencePaths.shortHash(parentArchive.absolutePath)
        val nestedFileName = ReferencePaths.safePathSegment(File(entry.name).name)
        val entryKey = ReferencePaths.shortHash(entry.name)
        val nestedJar = project.layout.buildDirectory
            .file("unpackSources/nested/$parentKey/$entryKey-$nestedFileName")
            .get()
            .asFile

        nestedJar.parentFile.mkdirs()
        zipFile.getInputStream(entry).use { input ->
            nestedJar.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return nestedJar
    }

    private fun fileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
