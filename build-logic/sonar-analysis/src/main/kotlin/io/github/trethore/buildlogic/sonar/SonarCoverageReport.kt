package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException

internal data class SonarCoverageReport(
    val projectKey: String,
    val measuresAvailable: Boolean,
    val coverage: String?,
    val lineCoverage: String?,
    val branchCoverage: String?,
    val newCoverage: String?,
    val coveredLines: Int?,
    val linesToCover: Int?,
    val files: List<SonarCoverageFile>,
)

internal data class SonarCoverageFile(
    val path: String,
    val lineCoverage: String?,
    val coveredLines: Int,
    val linesToCover: Int,
)

internal class SonarCoverageLoader(
    private val client: SonarClient,
) {
    private companion object {
        const val LINE_COVERAGE = "line_coverage"
        const val LINES_TO_COVER = "lines_to_cover"
        const val UNCOVERED_LINES = "uncovered_lines"
    }

    fun load(projectKey: String): SonarCoverageReport {
        val measures = fetchMeasures(projectKey)
        val linesToCover = measures[LINES_TO_COVER]?.toIntOrNull()
        val uncoveredLines = measures[UNCOVERED_LINES]?.toIntOrNull()
        val files = if ((uncoveredLines ?: 0) > 0) {
            fetchFiles(projectKey)
        } else {
            emptyList()
        }

        return SonarCoverageReport(
            projectKey = projectKey,
            measuresAvailable = measures.isNotEmpty(),
            coverage = measures["coverage"],
            lineCoverage = measures[LINE_COVERAGE],
            branchCoverage = measures["branch_coverage"],
            newCoverage = measures["new_coverage"],
            coveredLines = if (linesToCover != null && uncoveredLines != null) {
                linesToCover - uncoveredLines
            } else {
                null
            },
            linesToCover = linesToCover,
            files = files,
        )
    }

    private fun fetchMeasures(projectKey: String): Map<String, String> {
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
            parameters = mapOf("component" to projectKey, "metricKeys" to metricKeys),
            responseName = "coverage",
        )
        val component = payload["component"] as? Map<*, *>
            ?: throw GradleException("SonarQube coverage response did not contain a component.")

        return sonarMeasureValues(component)
    }

    private fun fetchFiles(projectKey: String): List<SonarCoverageFile> {
        return client.getComponentTreeComponents(
            parameters = mapOf(
                "component" to projectKey,
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

    private fun coverageFile(component: Map<*, *>): SonarCoverageFile? {
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
        return SonarCoverageFile(
            path = path,
            lineCoverage = measures[LINE_COVERAGE],
            coveredLines = linesToCover - uncoveredLines,
            linesToCover = linesToCover,
        )
    }
}

internal object SonarCoverageRenderer {
    fun render(report: SonarCoverageReport): List<String> {
        if (!report.measuresAvailable) {
            return listOf("No SonarQube coverage measures found for ${report.projectKey}.")
        }

        return buildList {
            add("SonarQube coverage for ${report.projectKey}:")
            add("  Overall: ${formatSonarPercentage(report.coverage)}")
            add("  Lines: ${formatSonarPercentage(report.lineCoverage)}${formatLineCount(report)}")
            add("  Branches: ${formatSonarPercentage(report.branchCoverage)}")
            add("  New code: ${formatSonarPercentage(report.newCoverage)}")

            if (report.files.isNotEmpty()) {
                add("")
                add("Files with uncovered lines:")
                report.files.forEach { file ->
                    add(
                        "  ${file.path}: ${formatSonarPercentage(file.lineCoverage)} " +
                            "(${file.coveredLines}/${file.linesToCover} covered)"
                    )
                }
            }
        }
    }

    private fun formatLineCount(report: SonarCoverageReport): String {
        return if (report.coveredLines != null && report.linesToCover != null) {
            " (${report.coveredLines}/${report.linesToCover} lines)"
        } else {
            ""
        }
    }
}
