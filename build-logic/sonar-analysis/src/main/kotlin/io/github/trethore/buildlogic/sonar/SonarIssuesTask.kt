package io.github.trethore.buildlogic.sonar

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class SonarIssuesTask : DefaultTask() {
    @get:Input
    abstract val hostUrl: Property<String>

    @get:Input
    abstract val projectKey: Property<String>

    @get:Internal
    abstract val token: Property<String>

    @get:Internal
    abstract val reportTaskFile: RegularFileProperty

    @TaskAction
    fun listIssues() {
        val sonarProjectKey = projectKey.get()
        val client = SonarApiClient.create(hostUrl.get(), token.orNull)
        client.waitForAnalysis(reportTaskFile.get().asFile)
        val issues = fetchIssues(client, sonarProjectKey)
        if (issues.isEmpty()) {
            logger.lifecycle("No unresolved SonarQube issues found for $sonarProjectKey.")
            return
        }

        logger.lifecycle("Unresolved SonarQube issues for $sonarProjectKey: ${issues.size}")
        issues.forEach { issue ->
            logger.lifecycle(formatIssue(sonarProjectKey, issue))
        }
    }

    private fun fetchIssues(client: SonarApiClient, sonarProjectKey: String): List<Map<*, *>> {
        val issues = mutableListOf<Map<*, *>>()
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
                    "componentKeys" to sonarProjectKey,
                    "resolved" to "false",
                    "p" to page.toString(),
                    "ps" to SonarConstants.PAGE_SIZE.toString(),
                ),
                responseName = "issues",
            )
            val total = (payload["total"] as? Number)?.toInt() ?: 0
            val pageIssues = (payload["issues"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
            issues += pageIssues

            if (issues.size >= total) {
                return issues
            }
            if (pageIssues.isEmpty()) {
                throw GradleException(
                    "SonarQube returned an empty issues page before the reported total was reached."
                )
            }

            page += 1
        }
    }

    private fun formatIssue(sonarProjectKey: String, issue: Map<*, *>): String {
        val component = issue["component"].toString().removePrefix("$sonarProjectKey:")
        val line = issue["line"]?.toString() ?: "-"
        val severity = issue["severity"]?.toString() ?: firstImpactValue(issue, "severity") ?: "-"
        val type = issue["type"]?.toString() ?: firstImpactValue(issue, "softwareQuality") ?: "-"
        val rule = issue["rule"]?.toString() ?: "-"
        val message = issue["message"]?.toString()?.replace('\n', ' ') ?: "-"

        return "$severity $type $component:$line $rule - $message"
    }

    private fun firstImpactValue(issue: Map<*, *>, key: String): String? {
        val firstImpact = (issue["impacts"] as? List<*>)
            .orEmpty()
            .filterIsInstance<Map<*, *>>()
            .firstOrNull()

        return firstImpact?.get(key)?.toString()
    }
}
