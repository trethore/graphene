package io.github.trethore.buildlogic.sonar

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class SonarDuplicatesTask : DefaultTask() {
    private companion object {
        const val DUPLICATED_FILES = "duplicated_files"
        const val DUPLICATED_LINES = "duplicated_lines"
        const val DUPLICATED_LINES_DENSITY = "duplicated_lines_density"
        const val NOT_AVAILABLE = "not available"
        const val SUMMARY_METRIC_KEYS = "$DUPLICATED_LINES,$DUPLICATED_LINES_DENSITY,$DUPLICATED_FILES"
    }

    @get:Input
    abstract val hostUrl: Property<String>

    @get:Input
    abstract val projectKey: Property<String>

    @get:Internal
    abstract val token: Property<String>

    @get:Internal
    abstract val reportTaskFile: RegularFileProperty

    @TaskAction
    fun showDuplicates() {
        val sonarProjectKey = projectKey.get()
        val client = SonarApiClient.create(hostUrl.get(), token.orNull)
        client.waitForAnalysis(reportTaskFile.get().asFile)

        val summary = fetchSummary(client, sonarProjectKey)
        if (summary[DUPLICATED_FILES]?.toIntOrNull() == 0) {
            showSummary(summary, 0)
            return
        }

        val duplicatedFiles = fetchDuplicatedFiles(client, sonarProjectKey)
        if (duplicatedFiles.isEmpty()) {
            showSummary(summary, 0)
            return
        }

        val duplicateGroups = fetchDuplicateGroups(client, duplicatedFiles)
        showSummary(summary, duplicateGroups.size)
        if (duplicateGroups.isEmpty()) {
            return
        }

        logger.lifecycle("")
        logger.lifecycle("Duplicate groups:")
        duplicateGroups.forEachIndexed { index, duplicateGroup ->
            logger.lifecycle("  ${index + 1}. ${formatGroupSize(duplicateGroup)}")
            duplicateGroup.occurrences.forEach { occurrence ->
                logger.lifecycle(
                    "     - ${occurrence.file.path}:${occurrence.line}:1 " +
                        "(lines ${occurrence.line}-${occurrence.endLine}, ${formatLineCount(occurrence.lineCount)})"
                )
            }
        }
    }

    private fun fetchSummary(
        client: SonarApiClient,
        sonarProjectKey: String,
    ): Map<String, String> {
        val payload = client.get(
            path = "/api/measures/component",
            parameters = mapOf("component" to sonarProjectKey, "metricKeys" to SUMMARY_METRIC_KEYS),
            responseName = "duplication summary",
        )
        val component = payload["component"] as? Map<*, *>
            ?: throw GradleException("SonarQube duplication summary response did not contain a component.")

        return sonarMeasureValues(component)
    }

    private fun fetchDuplicatedFiles(
        client: SonarApiClient,
        sonarProjectKey: String,
    ): List<DuplicatedFile> {
        return client.getComponentTreeComponents(
            parameters = mapOf(
                "component" to sonarProjectKey,
                "metricKeys" to DUPLICATED_LINES,
                "qualifiers" to "FIL",
                "strategy" to "leaves",
                "metricSort" to DUPLICATED_LINES,
                "metricSortFilter" to "withMeasuresOnly",
                "s" to "metric",
                "asc" to "false",
            ),
            responseName = "duplication files",
            transform = ::duplicatedFile,
        )
    }

    private fun duplicatedFile(component: Map<*, *>): DuplicatedFile? {
        val measures = sonarMeasureValues(component)
        val lines = measures[DUPLICATED_LINES]?.toIntOrNull() ?: 0
        if (lines == 0) {
            return null
        }

        val path = component["path"]?.toString()
            ?: component["name"]?.toString()
            ?: component["key"]?.toString()
            ?: "unknown"
        val key = component["key"]?.toString()
            ?: throw GradleException("SonarQube duplication file response did not contain a component key.")

        return DuplicatedFile(key, path)
    }

    private fun fetchDuplicateGroups(
        client: SonarApiClient,
        duplicatedFiles: List<DuplicatedFile>,
    ): List<DuplicateGroup> {
        val duplicatedFilesByKey = duplicatedFiles.associateBy(DuplicatedFile::key)
        val duplicateGroups = linkedSetOf<DuplicateGroup>()

        duplicatedFiles.forEach { duplicatedFile ->
            val payload = client.get(
                path = "/api/duplications/show",
                parameters = mapOf("key" to duplicatedFile.key),
                responseName = "duplications for ${duplicatedFile.path}",
            )
            val files = (payload["files"] as? Map<*, *>)
                .orEmpty()
                .mapNotNull { (reference, value) ->
                    val file = value as? Map<*, *> ?: return@mapNotNull null
                    val key = file["key"]?.toString() ?: return@mapNotNull null
                    reference.toString() to (duplicatedFilesByKey[key] ?: DuplicatedFile(
                        key = key,
                        path = file["name"]?.toString() ?: key,
                    ))
                }
                .toMap()
            val duplications = (payload["duplications"] as? List<*>)
                .orEmpty()
                .filterIsInstance<Map<*, *>>()

            duplications.forEach { duplication ->
                val occurrences = (duplication["blocks"] as? List<*>)
                    .orEmpty()
                    .filterIsInstance<Map<*, *>>()
                    .mapNotNull { block -> duplicateOccurrence(block, files) }
                    .sortedWith(compareBy({ occurrence -> occurrence.file.path }, DuplicateOccurrence::line))
                if (occurrences.size >= 2) {
                    duplicateGroups.add(DuplicateGroup(occurrences))
                }
            }
        }

        return duplicateGroups.toList()
    }

    private fun duplicateOccurrence(
        block: Map<*, *>,
        files: Map<String, DuplicatedFile>,
    ): DuplicateOccurrence? {
        val reference = block["_ref"]?.toString() ?: return null
        val file = files[reference] ?: return null
        val line = (block["from"] as? Number)?.toInt() ?: return null
        val lineCount = (block["size"] as? Number)?.toInt() ?: return null
        return DuplicateOccurrence(file, line, lineCount)
    }

    private fun showSummary(summary: Map<String, String>, duplicateGroups: Int) {
        logger.lifecycle("Duplication summary:")
        logger.lifecycle("  Duplicate groups: $duplicateGroups")
        logger.lifecycle("  Affected files: ${summary[DUPLICATED_FILES] ?: NOT_AVAILABLE}")
        logger.lifecycle("  Duplicated lines: ${summary[DUPLICATED_LINES] ?: NOT_AVAILABLE}")
        logger.lifecycle("  Duplication density: ${formatPercentage(summary[DUPLICATED_LINES_DENSITY])}")
    }

    private fun formatGroupSize(duplicateGroup: DuplicateGroup): String {
        val minimumLineCount = duplicateGroup.occurrences.minOf(DuplicateOccurrence::lineCount)
        val maximumLineCount = duplicateGroup.occurrences.maxOf(DuplicateOccurrence::lineCount)
        val lineCount = if (minimumLineCount == maximumLineCount) {
            formatLineCount(minimumLineCount)
        } else {
            "$minimumLineCount-$maximumLineCount lines"
        }
        return "$lineCount, ${duplicateGroup.occurrences.size} occurrences"
    }

    private fun formatLineCount(lineCount: Int): String = "$lineCount ${if (lineCount == 1) "line" else "lines"}"

    private fun formatPercentage(value: String?): String = value?.let { "$it%" } ?: NOT_AVAILABLE

    private data class DuplicatedFile(
        val key: String,
        val path: String,
    )

    private data class DuplicateGroup(
        val occurrences: List<DuplicateOccurrence>,
    )

    private data class DuplicateOccurrence(
        val file: DuplicatedFile,
        val line: Int,
        val lineCount: Int,
    ) {
        val endLine: Int
            get() = line + lineCount - 1
    }
}
