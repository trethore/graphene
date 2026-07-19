package io.github.trethore.buildlogic.sonar

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

abstract class SonarIssuesTask : DefaultTask() {
    @get:Input
    abstract val hostUrl: Property<String>

    @get:Input
    abstract val projectKey: Property<String>

    @get:Internal
    abstract val token: Property<String>

    @TaskAction
    fun listIssues() {
        val sonarToken = token.orNull
        if (sonarToken.isNullOrBlank()) {
            throw GradleException("${SonarConstants.TOKEN_ENV} is missing. Copy .env.example to .env and set a token.")
        }

        val sonarProjectKey = projectKey.get()
        val issues = fetchIssues(sonarToken, sonarProjectKey)
        if (issues.isEmpty()) {
            logger.lifecycle("No unresolved SonarQube issues found for $sonarProjectKey.")
            return
        }

        logger.lifecycle("Unresolved SonarQube issues for $sonarProjectKey: ${issues.size}")
        issues.forEach { issue ->
            logger.lifecycle(formatIssue(sonarProjectKey, issue))
        }
    }

    private fun fetchIssues(sonarToken: String, sonarProjectKey: String): List<Map<*, *>> {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(SonarConstants.REQUEST_TIMEOUT_SECONDS))
            .build()
        val credentials = Base64.getEncoder().encodeToString("$sonarToken:".toByteArray(StandardCharsets.UTF_8))
        val issues = mutableListOf<Map<*, *>>()
        val sonarHostUrl = hostUrl.get().trimEnd('/')
        var page = 1

        while (true) {
            if (page > SonarConstants.MAX_ISSUE_PAGES) {
                throw GradleException(
                    "SonarQube issues response exceeded ${SonarConstants.MAX_ISSUE_PAGES} pages."
                )
            }
            val request = buildIssuesRequest(sonarHostUrl, sonarProjectKey, credentials, page)
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                throw GradleException("SonarQube issues request failed with HTTP ${response.statusCode()}: ${response.body()}")
            }

            val payload = parsePayload(response.body())
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

    private fun buildIssuesRequest(
        sonarHostUrl: String,
        sonarProjectKey: String,
        credentials: String,
        page: Int,
    ): HttpRequest {
        val query = "componentKeys=${urlEncode(sonarProjectKey)}&resolved=false&p=$page&ps=${SonarConstants.PAGE_SIZE}"
        return HttpRequest.newBuilder()
            .uri(URI.create("$sonarHostUrl/api/issues/search?$query"))
            .header("Authorization", "Basic $credentials")
            .timeout(Duration.ofSeconds(SonarConstants.REQUEST_TIMEOUT_SECONDS))
            .GET()
            .build()
    }

    private fun parsePayload(responseBody: String): Map<*, *> {
        return JsonSlurper().parseText(responseBody) as? Map<*, *>
            ?: throw GradleException("SonarQube issues response was not a JSON object.")
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

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun firstImpactValue(issue: Map<*, *>, key: String): String? {
        val firstImpact = (issue["impacts"] as? List<*>)
            .orEmpty()
            .filterIsInstance<Map<*, *>>()
            .firstOrNull()

        return firstImpact?.get(key)?.toString()
    }
}
