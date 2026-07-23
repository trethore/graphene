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
        const val LINES_TO_COVER = "lines_to_cover"
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

        if ((measures[LINES_TO_COVER]?.toIntOrNull() ?: 0) > 0) {
            val files = fetchFiles(client, sonarProjectKey)
            if (files.isNotEmpty()) {
                logger.lifecycle("")
                logger.lifecycle("Files with lines to cover:")
                files.forEach { file ->
                    logger.lifecycle("  ${file.path}: ${file.linesToCover}")
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
            "line_coverage",
            "branch_coverage",
            "new_coverage",
            LINES_TO_COVER,
            "uncovered_lines",
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
                "metricKeys" to LINES_TO_COVER,
                "qualifiers" to "FIL",
                "strategy" to "leaves",
                "metricSort" to LINES_TO_COVER,
                "metricSortFilter" to "withMeasuresOnly",
                "s" to "metric",
                "asc" to "false",
            ),
            responseName = "coverage files",
            transform = ::coverageFile,
        )
    }

    private fun coverageFile(component: Map<*, *>): CoverageFile? {
        val linesToCover = sonarMeasureValues(component)[LINES_TO_COVER]?.toIntOrNull() ?: 0
        if (linesToCover <= 0) {
            return null
        }

        val path = component["path"]?.toString()
            ?: component["name"]?.toString()
            ?: component["key"]?.toString()
            ?: "unknown"
        return CoverageFile(path, linesToCover)
    }

    private fun logPercentage(label: String, value: String?) {
        logger.lifecycle("  $label: ${value?.let { "$it%" } ?: "not available"}")
    }

    private fun logLineCoverage(measures: Map<String, String>) {
        val coverage = measures["line_coverage"]
        val linesToCover = measures[LINES_TO_COVER]?.toIntOrNull()
        val uncoveredLines = measures["uncovered_lines"]?.toIntOrNull()
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
        val linesToCover: Int,
    )
}
