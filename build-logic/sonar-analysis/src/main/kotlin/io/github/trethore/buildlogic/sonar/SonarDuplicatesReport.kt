package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException

internal data class SonarDuplicatesReport(
    val affectedFiles: Int?,
    val duplicatedLines: Int?,
    val duplicatedLinesDensity: String?,
    val groups: List<SonarDuplicateGroup>?,
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
        val affectedFiles = summary.optionalSonarMetricInt(DUPLICATED_FILES, "duplication summary")
        val duplicatedLines = summary.optionalSonarMetricInt(DUPLICATED_LINES, "duplication summary")
        val groups = when (affectedFiles) {
            null -> null
            0 -> emptyList()
            else -> {
                val duplicatedFiles = fetchDuplicatedFiles(projectKey)
                if (duplicatedFiles.isEmpty()) {
                    throw GradleException(
                        "SonarQube reported $affectedFiles duplicated files, but returned no duplicated file details."
                    )
                }
                fetchDuplicateGroups(duplicatedFiles)
            }
        }
        return SonarDuplicatesReport(
            affectedFiles = affectedFiles,
            duplicatedLines = duplicatedLines,
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
        return sonarMeasureValues(
            payload.requiredSonarObject("component", "duplication summary"),
            "duplication summary",
        )
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
        ).sortedBy(DuplicatedFile::path)
    }

    private fun duplicatedFile(component: Map<*, *>): DuplicatedFile? {
        val responseName = "duplication file"
        val lines = sonarMeasureValues(component, responseName)
            .optionalSonarMetricInt(DUPLICATED_LINES, responseName) ?: return null
        if (lines == 0) {
            return null
        }

        val identity = component.sonarComponentIdentity(responseName)
        return DuplicatedFile(identity.key, identity.path)
    }

    private fun fetchDuplicateGroups(duplicatedFiles: List<DuplicatedFile>): List<SonarDuplicateGroup> {
        val duplicatedFilesByKey = duplicatedFiles.associateBy(DuplicatedFile::key)
        val duplicateGroups = linkedSetOf<SonarDuplicateGroup>()

        duplicatedFiles.forEach { duplicatedFile ->
            val responseName = "duplications for ${duplicatedFile.path}"
            val payload = client.get(
                path = "/api/duplications/show",
                parameters = mapOf("key" to duplicatedFile.key),
                responseName = responseName,
            )
            val files = payload.requiredSonarObject("files", responseName)
                .map { (reference, value) ->
                    val file = value as? Map<*, *>
                        ?: throw GradleException(
                            "SonarQube $responseName response contained an invalid file for reference $reference."
                        )
                    val key = file.requiredSonarString("key", responseName)
                    reference.toString() to (duplicatedFilesByKey[key] ?: DuplicatedFile(
                        key = key,
                        path = file.optionalSonarString("name", responseName) ?: key,
                    ))
                }
                .toMap()
            val duplications = payload.requiredSonarArray("duplications", responseName)

            duplications.forEachIndexed { index, rawDuplication ->
                val duplication = rawDuplication as? Map<*, *>
                    ?: throw GradleException(
                        "SonarQube $responseName response contained an invalid duplication at index $index."
                    )
                val occurrences = duplication.requiredSonarArray("blocks", responseName)
                    .mapIndexed { blockIndex, rawBlock ->
                        val block = rawBlock as? Map<*, *>
                            ?: throw GradleException(
                                "SonarQube $responseName response contained an invalid block at index $blockIndex."
                            )
                        duplicateOccurrence(block, files, responseName)
                    }
                    .distinct()
                    .sortedWith(compareBy(SonarDuplicateOccurrence::path, SonarDuplicateOccurrence::line))
                if (occurrences.size < 2) {
                    throw GradleException(
                        "SonarQube $responseName response contained a duplication with fewer than two occurrences."
                    )
                }
                duplicateGroups.add(SonarDuplicateGroup(occurrences))
            }
        }

        return duplicateGroups.sortedWith(
            compareBy(
                { group -> group.occurrences.first().path },
                { group -> group.occurrences.first().line },
            )
        )
    }

    private fun duplicateOccurrence(
        block: Map<*, *>,
        files: Map<String, DuplicatedFile>,
        responseName: String,
    ): SonarDuplicateOccurrence {
        val reference = block.requiredSonarString("_ref", responseName)
        val file = files[reference]
            ?: throw GradleException("SonarQube $responseName response referenced unknown file '$reference'.")
        val line = block.requiredSonarInt("from", responseName)
        val lineCount = block.requiredSonarInt("size", responseName)
        if (line < 1 || lineCount < 1) {
            throw GradleException(
                "SonarQube $responseName response contained an invalid duplicate range: line=$line, size=$lineCount"
            )
        }
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
            add("Groups: ${report.groups?.size ?: SONAR_NOT_AVAILABLE}")
            add("Affected files: ${report.affectedFiles ?: SONAR_NOT_AVAILABLE}")
            add("Duplicated lines: ${report.duplicatedLines ?: SONAR_NOT_AVAILABLE}")
            add("Density: ${formatSonarPercentage(report.duplicatedLinesDensity)}")

            report.groups?.forEachIndexed { index, duplicateGroup ->
                add("${index + 1}. ${formatGroupSize(duplicateGroup)}")
                duplicateGroup.occurrences.forEach { occurrence ->
                    add("- ${occurrence.path}:${formatRange(occurrence)}")
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

    private fun formatRange(occurrence: SonarDuplicateOccurrence): String {
        return if (occurrence.line == occurrence.endLine) {
            occurrence.line.toString()
        } else {
            "${occurrence.line}-${occurrence.endLine}"
        }
    }

    private fun formatLineCount(lineCount: Int): String = "$lineCount ${if (lineCount == 1) "line" else "lines"}"
}
