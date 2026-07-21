package io.github.trethore.buildlogic.sonar

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class SonarCoverageTask : DefaultTask() {
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
            "lines_to_cover",
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

    private fun logPercentage(label: String, value: String?) {
        logger.lifecycle("  $label: ${value?.let { "$it%" } ?: "not available"}")
    }

    private fun logLineCoverage(measures: Map<String, String>) {
        val coverage = measures["line_coverage"]
        val linesToCover = measures["lines_to_cover"]?.toIntOrNull()
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
}
