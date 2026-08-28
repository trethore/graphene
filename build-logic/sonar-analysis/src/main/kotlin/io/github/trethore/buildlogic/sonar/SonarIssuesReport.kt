package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException

internal data class SonarIssuesReport(
    val issues: List<SonarIssue>,
)

internal data class SonarIssue(
    val component: String,
    val range: SonarIssueRange?,
    val impacts: List<SonarIssueImpact>,
    val rule: String,
    val message: String,
)

internal data class SonarIssueRange(
    val startLine: Int,
    val startColumn: Int?,
    val endLine: Int,
    val endColumn: Int?,
)

internal data class SonarIssueImpact(
    val severity: String,
    val softwareQuality: String,
)

internal class SonarIssuesLoader(
    private val client: SonarClient,
) {
    private companion object {
        val WHITESPACE = Regex("\\s+")
    }

    fun load(projectKey: String): SonarIssuesReport {
        val issues = mutableListOf<SonarIssue>()
        var page = 1

        while (true) {
            if (page > SonarConstants.MAX_PAGES) {
                throw GradleException(
                    "SonarQube issues response exceeded ${SonarConstants.MAX_PAGES} pages."
                )
            }
            val payload = client.get(
                path = "/api/issues/search",
                parameters = mapOf(
                    "componentKeys" to projectKey,
                    "resolved" to "false",
                    "p" to page.toString(),
                    "ps" to SonarConstants.PAGE_SIZE.toString(),
                ),
                responseName = "issues",
            )
            val total = requireNonNegativeSonarValue(
                payload.requiredSonarInt("total", "issues"),
                "total",
                "issues",
            )
            val pageIssues = payload.requiredSonarArray("issues", "issues")
                .mapIndexed { index, issue ->
                    issue as? Map<*, *>
                        ?: throw GradleException(
                            "SonarQube issues response contained an invalid issue at index $index on page $page."
                        )
                }
            pageIssues.mapTo(issues) { issue -> sonarIssue(projectKey, issue) }

            if (issues.size >= total) {
                return SonarIssuesReport(
                    issues.sortedWith(
                        compareBy(
                            SonarIssue::component,
                            { issue -> issue.range?.startLine ?: Int.MAX_VALUE },
                            SonarIssue::rule,
                        )
                    ),
                )
            }
            if (pageIssues.isEmpty()) {
                throw GradleException(
                    "SonarQube returned an empty issues page before the reported total was reached."
                )
            }

            page += 1
        }
    }

    private fun sonarIssue(projectKey: String, issue: Map<*, *>): SonarIssue {
        val component = issue.requiredSonarString("component", "issues").removePrefix("$projectKey:")
        return SonarIssue(
            component = component,
            range = issueRange(issue),
            impacts = issueImpacts(issue),
            rule = issue.requiredSonarString("rule", "issues"),
            message = issue.requiredSonarString("message", "issues").replace(WHITESPACE, " "),
        )
    }

    private fun issueRange(issue: Map<*, *>): SonarIssueRange? {
        val rawTextRange = issue["textRange"]
        if (rawTextRange != null) {
            val textRange = rawTextRange as? Map<*, *>
                ?: throw GradleException("SonarQube issues response did not contain a valid 'textRange' object.")
            val startLine = positiveLine(textRange.requiredSonarInt("startLine", "issue text range"))
            val endLine = positiveLine(textRange.requiredSonarInt("endLine", "issue text range"))
            val startOffset = nonNegativeOffset(textRange.optionalSonarInt("startOffset", "issue text range"))
            val endOffset = nonNegativeOffset(textRange.optionalSonarInt("endOffset", "issue text range"))
            if ((startOffset == null) != (endOffset == null)) {
                throw GradleException("SonarQube issue text range must provide both start and end offsets.")
            }
            if (endLine < startLine || (endLine == startLine && startOffset != null && endOffset != null && endOffset < startOffset)) {
                throw GradleException("SonarQube issue text range ended before it started.")
            }
            return SonarIssueRange(
                startLine = startLine,
                startColumn = startOffset?.plus(1),
                endLine = endLine,
                endColumn = endOffset?.plus(1),
            )
        }

        val line = positiveLine(issue.optionalSonarInt("line", "issues") ?: return null)
        return SonarIssueRange(line, null, line, null)
    }

    private fun issueImpacts(issue: Map<*, *>): List<SonarIssueImpact> {
        val rawImpacts = issue["impacts"]
        if (rawImpacts != null) {
            val impacts = rawImpacts as? List<*>
                ?: throw GradleException("SonarQube issues response did not contain a valid 'impacts' array.")
            if (impacts.isNotEmpty()) {
                return impacts.mapIndexed { index, impact ->
                    val values = impact as? Map<*, *>
                        ?: throw GradleException(
                            "SonarQube issues response contained an invalid impact at index $index."
                        )
                    SonarIssueImpact(
                        severity = values.requiredSonarString("severity", "issue impact"),
                        softwareQuality = values.requiredSonarString("softwareQuality", "issue impact"),
                    )
                }.distinct().sortedWith(compareBy(SonarIssueImpact::softwareQuality, SonarIssueImpact::severity))
            }
        }

        val severity = issue.optionalSonarString("severity", "issues")
        val type = issue.optionalSonarString("type", "issues")
        return if (severity != null && type != null) {
            listOf(SonarIssueImpact(severity, type))
        } else {
            emptyList()
        }
    }

    private fun positiveLine(line: Int): Int {
        if (line < 1) {
            throw GradleException("SonarQube issue response contained an invalid line: $line")
        }
        return line
    }

    private fun nonNegativeOffset(offset: Int?): Int? {
        if (offset != null && offset < 0) {
            throw GradleException("SonarQube issue response contained an invalid offset: $offset")
        }
        return offset
    }
}

internal object SonarIssuesRenderer {
    fun render(report: SonarIssuesReport): List<String> {
        return buildList {
            if (report.issues.isEmpty()) {
                add("No unresolved issues found.")
            } else {
                report.issues.forEach { issue ->
                    add(
                        "${issue.component}:${formatRange(issue.range)} ${formatImpacts(issue.impacts)} " +
                            "${issue.rule} - ${issue.message}"
                    )
                }
            }
        }
    }

    private fun formatRange(range: SonarIssueRange?): String {
        if (range == null) {
            return "-"
        }
        if (range.startColumn == null || range.endColumn == null) {
            return if (range.startLine == range.endLine) {
                range.startLine.toString()
            } else {
                "${range.startLine}-${range.endLine}"
            }
        }
        return "${range.startLine}:${range.startColumn}-${range.endLine}:${range.endColumn}"
    }

    private fun formatImpacts(impacts: List<SonarIssueImpact>): String {
        if (impacts.isEmpty()) {
            return "[not available]"
        }
        return impacts.joinToString(prefix = "[", postfix = "]") { impact ->
            "${impact.severity} ${impact.softwareQuality}"
        }
    }
}
