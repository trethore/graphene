package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException

internal data class SonarIssuesReport(
    val projectKey: String,
    val issues: List<SonarIssue>,
)

internal data class SonarIssue(
    val component: String,
    val line: String,
    val severity: String,
    val type: String,
    val rule: String,
    val message: String,
)

internal class SonarIssuesLoader(
    private val client: SonarClient,
) {
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
            val total = (payload["total"] as? Number)?.toInt() ?: 0
            val pageIssues = (payload["issues"] as? List<*>)
                .orEmpty()
                .filterIsInstance<Map<*, *>>()
            pageIssues.mapTo(issues) { issue -> sonarIssue(projectKey, issue) }

            if (issues.size >= total) {
                return SonarIssuesReport(projectKey, issues)
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
        return SonarIssue(
            component = issue["component"].toString().removePrefix("$projectKey:"),
            line = issue["line"]?.toString() ?: "-",
            severity = issue["severity"]?.toString() ?: firstImpactValue(issue, "severity") ?: "-",
            type = issue["type"]?.toString() ?: firstImpactValue(issue, "softwareQuality") ?: "-",
            rule = issue["rule"]?.toString() ?: "-",
            message = issue["message"]?.toString()?.replace('\n', ' ') ?: "-",
        )
    }

    private fun firstImpactValue(issue: Map<*, *>, key: String): String? {
        val firstImpact = (issue["impacts"] as? List<*>)
            .orEmpty()
            .filterIsInstance<Map<*, *>>()
            .firstOrNull()

        return firstImpact?.get(key)?.toString()
    }
}

internal object SonarIssuesRenderer {
    fun render(report: SonarIssuesReport): List<String> {
        if (report.issues.isEmpty()) {
            return listOf("No unresolved SonarQube issues found for ${report.projectKey}.")
        }

        return buildList {
            add("Unresolved SonarQube issues for ${report.projectKey}: ${report.issues.size}")
            report.issues.forEach { issue ->
                add(
                    "${issue.severity} ${issue.type} ${issue.component}:${issue.line} " +
                        "${issue.rule} - ${issue.message}"
                )
            }
        }
    }
}
