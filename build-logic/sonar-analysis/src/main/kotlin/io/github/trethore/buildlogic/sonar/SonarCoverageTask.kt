package io.github.trethore.buildlogic.sonar

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class SonarCoverageTask : DefaultTask() {
    private companion object {
        const val LINE_COVERAGE = "line_coverage"
        const val LINES_TO_COVER = "lines_to_cover"
        const val UNCOVERED_LINES = "uncovered_lines"
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
    fun showCoverage() {
        val sonarProjectKey = projectKey.get()
        val client = SonarApiClient.create(hostUrl.get(), token.orNull)
        client.waitForAnalysis(reportTaskFile.get().asFile)
        val measures = fetchMeasures(client, sonarProjectKey)
        if (measures.isEmpty()) {
            logger.lifecycle("No SonarQube coverage measures found for $sonarProjectKey.")
            return
        }

        logger.lifecycle("SonarQube coverage for $sonarProjectKey:")
        logPercentage("Overall", measures["coverage"])
        logLineCoverage(measures)
        logPercentage("Branches", measures["branch_coverage"])
        logPercentage("New code", measures["new_coverage"])

        if ((measures[UNCOVERED_LINES]?.toIntOrNull() ?: 0) > 0) {
            val files = fetchFiles(client, sonarProjectKey)
            if (files.isNotEmpty()) {
                logger.lifecycle("")
                logger.lifecycle("Files with uncovered lines:")
                files.forEach { file ->
                    logger.lifecycle(
                        "  ${file.path}: ${file.lineCoverage?.let { "$it%" } ?: "not available"} " +
                            "(${file.coveredLines}/${file.linesToCover} covered)"
                    )
                }
            }
        }
    }

    private fun fetchMeasures(
        client: SonarApiClient,
        sonarProjectKey: String,
    ): Map<String, String> {
        val metricKeys = listOf(
            "coverage",
            LINE_COVERAGE,
            "branch_coverage",
            "new_coverage",
            LINES_TO_COVER,
            UNCOVERED_LINES,
        ).joinToString(",")
        val payload = client.get(
            path = "/api/measures/component",
            parameters = mapOf("component" to sonarProjectKey, "metricKeys" to metricKeys),
            responseName = "coverage",
        )
        val component = payload["component"] as? Map<*, *>
            ?: throw GradleException("SonarQube coverage response did not contain a component.")

        return sonarMeasureValues(component)
    }

    private fun fetchFiles(
        client: SonarApiClient,
        sonarProjectKey: String,
    ): List<CoverageFile> {
        return client.getComponentTreeComponents(
            parameters = mapOf(
                "component" to sonarProjectKey,
                "metricKeys" to "$LINE_COVERAGE,$LINES_TO_COVER,$UNCOVERED_LINES",
                "qualifiers" to "FIL",
                "strategy" to "leaves",
                "metricSort" to UNCOVERED_LINES,
                "metricSortFilter" to "withMeasuresOnly",
                "s" to "metric",
                "asc" to "false",
            ),
            responseName = "coverage files",
            transform = ::coverageFile,
        )
    }

    private fun coverageFile(component: Map<*, *>): CoverageFile? {
        val measures = sonarMeasureValues(component)
        val uncoveredLines = measures[UNCOVERED_LINES]?.toIntOrNull() ?: 0
        if (uncoveredLines <= 0) {
            return null
        }

        val linesToCover = measures[LINES_TO_COVER]?.toIntOrNull() ?: 0
        val path = component["path"]?.toString()
            ?: component["name"]?.toString()
            ?: component["key"]?.toString()
            ?: "unknown"
        return CoverageFile(
            path = path,
            lineCoverage = measures[LINE_COVERAGE],
            coveredLines = linesToCover - uncoveredLines,
            linesToCover = linesToCover,
        )
    }

    private fun logPercentage(label: String, value: String?) {
        logger.lifecycle("  $label: ${value?.let { "$it%" } ?: "not available"}")
    }

    private fun logLineCoverage(measures: Map<String, String>) {
        val coverage = measures[LINE_COVERAGE]
        val linesToCover = measures[LINES_TO_COVER]?.toIntOrNull()
        val uncoveredLines = measures[UNCOVERED_LINES]?.toIntOrNull()
        val coveredLines = if (linesToCover != null && uncoveredLines != null) {
            linesToCover - uncoveredLines
        } else {
            null
        }
        val lineCount = if (coveredLines != null && linesToCover != null) {
            " ($coveredLines/$linesToCover lines)"
        } else {
            ""
        }

        logger.lifecycle("  Lines: ${coverage?.let { "$it%" } ?: "not available"}$lineCount")
    }

    private data class CoverageFile(
        val path: String,
        val lineCoverage: String?,
        val coveredLines: Int,
        val linesToCover: Int,
    )
}
