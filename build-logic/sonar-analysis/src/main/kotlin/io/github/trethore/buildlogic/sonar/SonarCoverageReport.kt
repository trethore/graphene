package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException

internal data class SonarCoverageReport(
    val measuresAvailable: Boolean,
    val coverage: String?,
    val lineCoverage: String?,
    val branchCoverage: String?,
    val uncoveredLines: Int?,
    val linesToCover: Int?,
    val files: List<SonarCoverageFile>,
)

internal data class SonarCoverageFile(
    val path: String,
    val linesToCover: Int,
    val uncoveredLineRanges: List<SonarLineRange>,
)

internal data class SonarLineRange(
    val startLine: Int,
    val endLine: Int,
)

internal class SonarCoverageLoader(
    private val client: SonarClient,
) {
    private companion object {
        const val COVERAGE = "coverage"
        const val LINE_COVERAGE = "line_coverage"
        const val BRANCH_COVERAGE = "branch_coverage"
        const val LINES_TO_COVER = "lines_to_cover"
        const val UNCOVERED_LINES = "uncovered_lines"
    }

    fun load(projectKey: String): SonarCoverageReport {
        val measures = fetchMeasures(projectKey)
        val linesToCover = measures.optionalSonarMetricInt(LINES_TO_COVER, "coverage")
        val uncoveredLines = measures.optionalSonarMetricInt(UNCOVERED_LINES, "coverage")
        validateLineCounts(linesToCover, uncoveredLines, "coverage")
        val files = if (uncoveredLines != null && uncoveredLines > 0) {
            fetchFiles(projectKey)
        } else {
            emptyList()
        }

        return SonarCoverageReport(
            measuresAvailable = measures.isNotEmpty(),
            coverage = measures[COVERAGE],
            lineCoverage = measures[LINE_COVERAGE],
            branchCoverage = measures[BRANCH_COVERAGE],
            uncoveredLines = uncoveredLines,
            linesToCover = linesToCover,
            files = files,
        )
    }

    private fun fetchMeasures(projectKey: String): Map<String, String> {
        val metricKeys = listOf(
            COVERAGE,
            LINE_COVERAGE,
            BRANCH_COVERAGE,
            LINES_TO_COVER,
            UNCOVERED_LINES,
        ).joinToString(",")
        val payload = client.get(
            path = "/api/measures/component",
            parameters = mapOf("component" to projectKey, "metricKeys" to metricKeys),
            responseName = "coverage",
        )
        return sonarMeasureValues(payload.requiredSonarObject("component", "coverage"), "coverage")
    }

    private fun fetchFiles(projectKey: String): List<SonarCoverageFile> {
        return client.getComponentTreeComponents(
            parameters = mapOf(
                "component" to projectKey,
                "metricKeys" to "$LINES_TO_COVER,$UNCOVERED_LINES",
                "qualifiers" to "FIL",
                "strategy" to "leaves",
                "metricSort" to UNCOVERED_LINES,
                "metricSortFilter" to "withMeasuresOnly",
                "s" to "metric",
                "asc" to "false",
            ),
            responseName = "coverage files",
            transform = ::coverageFileSummary,
        ).sortedWith(
            compareByDescending(CoverageFileSummary::uncoveredLines).thenBy(CoverageFileSummary::path)
        ).map(::coverageFile)
    }

    private fun coverageFileSummary(component: Map<*, *>): CoverageFileSummary? {
        val responseName = "coverage file"
        val measures = sonarMeasureValues(component, responseName)
        val uncoveredLines = measures.optionalSonarMetricInt(UNCOVERED_LINES, responseName) ?: return null
        if (uncoveredLines == 0) {
            return null
        }
        val linesToCover = measures.optionalSonarMetricInt(LINES_TO_COVER, responseName)
            ?: throw GradleException("SonarQube $responseName response did not contain '$LINES_TO_COVER'.")
        validateLineCounts(linesToCover, uncoveredLines, responseName)
        val identity = component.sonarComponentIdentity(responseName)
        return CoverageFileSummary(
            key = identity.key,
            path = identity.path,
            uncoveredLines = uncoveredLines,
            linesToCover = linesToCover,
        )
    }

    private fun coverageFile(summary: CoverageFileSummary): SonarCoverageFile {
        return SonarCoverageFile(
            path = summary.path,
            linesToCover = summary.linesToCover,
            uncoveredLineRanges = fetchUncoveredLineRanges(
                summary.key,
                summary.path,
                summary.uncoveredLines,
            ),
        )
    }

    private fun fetchUncoveredLineRanges(
        key: String,
        path: String,
        expectedUncoveredLines: Int,
    ): List<SonarLineRange> {
        val responseName = "coverage lines for $path"
        val payload = client.get(
            path = "/api/sources/lines",
            parameters = mapOf("key" to key, "from" to "1"),
            responseName = responseName,
        )
        val uncoveredLines = payload.requiredSonarArray("sources", responseName)
            .mapIndexedNotNull { index, rawSource ->
                val source = rawSource as? Map<*, *>
                    ?: throw GradleException(
                        "SonarQube $responseName response contained an invalid source line at index $index."
                    )
                val lineHits = source.optionalSonarInt("lineHits", responseName) ?: return@mapIndexedNotNull null
                requireNonNegativeSonarValue(lineHits, "lineHits", responseName)
                if (lineHits == 0) {
                    val line = source.requiredSonarInt("line", responseName)
                    if (line < 1) {
                        throw GradleException("SonarQube $responseName response contained an invalid line: $line")
                    }
                    line
                } else {
                    null
                }
            }
            .sorted()
        if (uncoveredLines.size != expectedUncoveredLines) {
            throw GradleException(
                "SonarQube $responseName returned ${uncoveredLines.size} uncovered lines, " +
                    "but the coverage measure reported $expectedUncoveredLines."
            )
        }
        return lineRanges(uncoveredLines)
    }

    private fun lineRanges(lines: List<Int>): List<SonarLineRange> {
        if (lines.isEmpty()) {
            return emptyList()
        }
        val ranges = mutableListOf<SonarLineRange>()
        var startLine = lines.first()
        var endLine = startLine
        for (index in 1 until lines.size) {
            val line = lines[index]
            if (line == endLine + 1) {
                endLine = line
            } else {
                ranges += SonarLineRange(startLine, endLine)
                startLine = line
                endLine = line
            }
        }
        ranges += SonarLineRange(startLine, endLine)
        return ranges
    }

    private fun validateLineCounts(linesToCover: Int?, uncoveredLines: Int?, responseName: String) {
        if ((linesToCover == null) != (uncoveredLines == null)) {
            throw GradleException(
                "SonarQube $responseName response must provide both '$LINES_TO_COVER' and '$UNCOVERED_LINES'."
            )
        }
        if (linesToCover != null && uncoveredLines != null && uncoveredLines > linesToCover) {
            throw GradleException(
                "SonarQube $responseName response reported $uncoveredLines uncovered lines " +
                    "for only $linesToCover lines to cover."
            )
        }
    }

    private data class CoverageFileSummary(
        val key: String,
        val path: String,
        val uncoveredLines: Int,
        val linesToCover: Int,
    )
}

internal object SonarCoverageRenderer {
    fun render(report: SonarCoverageReport): List<String> {
        return buildList {
            if (!report.measuresAvailable) {
                add("No coverage measures found.")
                return@buildList
            }

            add("Overall: ${formatSonarPercentage(report.coverage)}")
            add(formatLineCoverage(report))
            add("Branches: ${formatSonarPercentage(report.branchCoverage)}")

            if (report.files.isNotEmpty()) {
                report.files.forEach { file ->
                    val uncoveredLines = file.uncoveredLineRanges.sumOf { range -> range.endLine - range.startLine + 1 }
                    add("${file.path}: ${file.linesToCover - uncoveredLines}/${file.linesToCover} covered, to cover: " +
                        formatRanges(file.uncoveredLineRanges))
                }
            }
        }
    }

    private fun formatLineCoverage(report: SonarCoverageReport): String {
        return if (report.uncoveredLines != null && report.linesToCover != null) {
            "Lines: ${report.linesToCover - report.uncoveredLines}/${report.linesToCover} covered, " +
                "${report.uncoveredLines} uncovered (${formatSonarPercentage(report.lineCoverage)})"
        } else {
            "Lines: ${formatSonarPercentage(report.lineCoverage)}"
        }
    }

    private fun formatRanges(ranges: List<SonarLineRange>): String {
        return ranges.joinToString(", ") { range ->
            if (range.startLine == range.endLine) {
                range.startLine.toString()
            } else {
                "${range.startLine}-${range.endLine}"
            }
        }
    }
}
