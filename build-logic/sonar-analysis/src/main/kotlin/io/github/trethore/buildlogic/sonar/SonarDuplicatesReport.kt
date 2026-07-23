package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException

internal data class SonarDuplicatesReport(
    val affectedFiles: String?,
    val duplicatedLines: String?,
    val duplicatedLinesDensity: String?,
    val groups: List<SonarDuplicateGroup>,
)

internal data class SonarDuplicateGroup(
    val occurrences: List<SonarDuplicateOccurrence>,
)

internal data class SonarDuplicateOccurrence(
    val fileKey: String,
    val path: String,
    val line: Int,
    val lineCount: Int,
) {
    val endLine: Int
        get() = line + lineCount - 1
}

internal class SonarDuplicatesLoader(
    private val client: SonarClient,
) {
    private companion object {
        const val DUPLICATED_FILES = "duplicated_files"
        const val DUPLICATED_LINES = "duplicated_lines"
        const val DUPLICATED_LINES_DENSITY = "duplicated_lines_density"
        const val SUMMARY_METRIC_KEYS = "$DUPLICATED_LINES,$DUPLICATED_LINES_DENSITY,$DUPLICATED_FILES"
    }

    fun load(projectKey: String): SonarDuplicatesReport {
        val summary = fetchSummary(projectKey)
        if (summary[DUPLICATED_FILES]?.toIntOrNull() == 0) {
            return report(summary, emptyList())
        }

        val duplicatedFiles = fetchDuplicatedFiles(projectKey)
        val groups = if (duplicatedFiles.isEmpty()) {
            emptyList()
        } else {
            fetchDuplicateGroups(duplicatedFiles)
        }
        return report(summary, groups)
    }

    private fun report(
        summary: Map<String, String>,
        groups: List<SonarDuplicateGroup>,
    ): SonarDuplicatesReport {
        return SonarDuplicatesReport(
            affectedFiles = summary[DUPLICATED_FILES],
            duplicatedLines = summary[DUPLICATED_LINES],
            duplicatedLinesDensity = summary[DUPLICATED_LINES_DENSITY],
            groups = groups,
        )
    }

    private fun fetchSummary(projectKey: String): Map<String, String> {
        val payload = client.get(
            path = "/api/measures/component",
            parameters = mapOf("component" to projectKey, "metricKeys" to SUMMARY_METRIC_KEYS),
            responseName = "duplication summary",
        )
        val component = payload["component"] as? Map<*, *>
            ?: throw GradleException("SonarQube duplication summary response did not contain a component.")

        return sonarMeasureValues(component)
    }

    private fun fetchDuplicatedFiles(projectKey: String): List<DuplicatedFile> {
        return client.getComponentTreeComponents(
            parameters = mapOf(
                "component" to projectKey,
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
        val lines = sonarMeasureValues(component)[DUPLICATED_LINES]?.toIntOrNull() ?: 0
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

    private fun fetchDuplicateGroups(duplicatedFiles: List<DuplicatedFile>): List<SonarDuplicateGroup> {
        val duplicatedFilesByKey = duplicatedFiles.associateBy(DuplicatedFile::key)
        val duplicateGroups = linkedSetOf<SonarDuplicateGroup>()

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
                    .sortedWith(compareBy(SonarDuplicateOccurrence::path, SonarDuplicateOccurrence::line))
                if (occurrences.size >= 2) {
                    duplicateGroups.add(SonarDuplicateGroup(occurrences))
                }
            }
        }

        return duplicateGroups.toList()
    }

    private fun duplicateOccurrence(
        block: Map<*, *>,
        files: Map<String, DuplicatedFile>,
    ): SonarDuplicateOccurrence? {
        val reference = block["_ref"]?.toString() ?: return null
        val file = files[reference] ?: return null
        val line = (block["from"] as? Number)?.toInt() ?: return null
        val lineCount = (block["size"] as? Number)?.toInt() ?: return null
        return SonarDuplicateOccurrence(
            fileKey = file.key,
            path = file.path,
            line = line,
            lineCount = lineCount,
        )
    }

    private data class DuplicatedFile(
        val key: String,
        val path: String,
    )
}

internal object SonarDuplicatesRenderer {
    fun render(report: SonarDuplicatesReport): List<String> {
        return buildList {
            add("Duplication summary:")
            add("  Duplicate groups: ${report.groups.size}")
            add("  Affected files: ${report.affectedFiles ?: SONAR_NOT_AVAILABLE}")
            add("  Duplicated lines: ${report.duplicatedLines ?: SONAR_NOT_AVAILABLE}")
            add("  Duplication density: ${formatSonarPercentage(report.duplicatedLinesDensity)}")

            if (report.groups.isNotEmpty()) {
                add("")
                add("Duplicate groups:")
                report.groups.forEachIndexed { index, duplicateGroup ->
                    add("  ${index + 1}. ${formatGroupSize(duplicateGroup)}")
                    duplicateGroup.occurrences.forEach { occurrence ->
                        add(
                            "     - ${occurrence.path}:${occurrence.line}:1 " +
                                "(lines ${occurrence.line}-${occurrence.endLine}, " +
                                "${formatLineCount(occurrence.lineCount)})"
                        )
                    }
                }
            }
        }
    }

    private fun formatGroupSize(duplicateGroup: SonarDuplicateGroup): String {
        val minimumLineCount = duplicateGroup.occurrences.minOf(SonarDuplicateOccurrence::lineCount)
        val maximumLineCount = duplicateGroup.occurrences.maxOf(SonarDuplicateOccurrence::lineCount)
        val lineCount = if (minimumLineCount == maximumLineCount) {
            formatLineCount(minimumLineCount)
        } else {
            "$minimumLineCount-$maximumLineCount lines"
        }
        return "$lineCount, ${duplicateGroup.occurrences.size} occurrences"
    }

    private fun formatLineCount(lineCount: Int): String = "$lineCount ${if (lineCount == 1) "line" else "lines"}"
}
