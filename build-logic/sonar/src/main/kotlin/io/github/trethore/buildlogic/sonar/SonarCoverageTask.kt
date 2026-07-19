package io.github.trethore.buildlogic.sonar

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
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
import java.util.Properties
import java.util.concurrent.TimeUnit

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
        val sonarToken = token.orNull
        if (sonarToken.isNullOrBlank()) {
            throw GradleException("${SonarConstants.TOKEN_ENV} is missing. Copy .env.example to .env and set a token.")
        }

        val sonarProjectKey = projectKey.get()
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(SonarConstants.REQUEST_TIMEOUT_SECONDS))
            .build()
        val credentials = Base64.getEncoder().encodeToString("$sonarToken:".toByteArray(StandardCharsets.UTF_8))
        waitForAnalysis(client, credentials)
        val measures = fetchMeasures(client, credentials, sonarProjectKey)
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
        client: HttpClient,
        credentials: String,
        sonarProjectKey: String,
    ): Map<String, String> {
        val sonarHostUrl = hostUrl.get().trimEnd('/')
        val request = buildMeasuresRequest(sonarHostUrl, sonarProjectKey, credentials)
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            throw GradleException("SonarQube coverage request failed with HTTP ${response.statusCode()}: ${response.body()}")
        }

        val payload = parsePayload(response.body())
        val component = payload["component"] as? Map<*, *>
            ?: throw GradleException("SonarQube coverage response did not contain a component.")

        return (component["measures"] as? List<*>)
            .orEmpty()
            .filterIsInstance<Map<*, *>>()
            .mapNotNull { measure ->
                val metric = measure["metric"]?.toString() ?: return@mapNotNull null
                val value = measureValue(measure) ?: return@mapNotNull null
                metric to value
            }
            .toMap()
    }

    private fun waitForAnalysis(client: HttpClient, credentials: String) {
        val metadataFile = reportTaskFile.get().asFile
        if (!metadataFile.isFile) {
            throw GradleException("SonarQube analysis metadata was not found at $metadataFile.")
        }

        val metadata = Properties().apply {
            metadataFile.inputStream().use(::load)
        }
        val ceTaskUrl = metadata.getProperty("ceTaskUrl")
            ?: throw GradleException("SonarQube analysis metadata did not contain ceTaskUrl.")
        val timeoutNanoseconds = TimeUnit.SECONDS.toNanos(SonarConstants.ANALYSIS_TIMEOUT_SECONDS)
        val deadline = System.nanoTime() + timeoutNanoseconds

        while (true) {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(ceTaskUrl))
                .header("Authorization", "Basic $credentials")
                .timeout(Duration.ofSeconds(SonarConstants.REQUEST_TIMEOUT_SECONDS))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() !in 200..299) {
                throw GradleException(
                    "SonarQube analysis status request failed with HTTP ${response.statusCode()}: ${response.body()}"
                )
            }

            val payload = parsePayload(response.body())
            val task = payload["task"] as? Map<*, *>
                ?: throw GradleException("SonarQube analysis status response did not contain a task.")
            when (val status = task["status"]?.toString()) {
                "SUCCESS" -> return
                "PENDING", "IN_PROGRESS" -> {
                    if (System.nanoTime() >= deadline) {
                        throw GradleException(
                            "Timed out waiting ${SonarConstants.ANALYSIS_TIMEOUT_SECONDS} seconds for SonarQube analysis."
                        )
                    }
                    sleepBeforeRetry()
                }
                "FAILED", "CANCELED" -> {
                    val errorMessage = task["errorMessage"]?.toString()?.let { ": $it" }.orEmpty()
                    throw GradleException("SonarQube analysis finished with status $status$errorMessage")
                }
                else -> throw GradleException("SonarQube analysis returned unknown status: $status")
            }
        }
    }

    private fun sleepBeforeRetry() {
        try {
            Thread.sleep(SonarConstants.STATUS_POLL_INTERVAL_MILLISECONDS)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw GradleException("Interrupted while waiting for SonarQube analysis.", exception)
        }
    }

    private fun buildMeasuresRequest(
        sonarHostUrl: String,
        sonarProjectKey: String,
        credentials: String,
    ): HttpRequest {
        val metricKeys = listOf(
            "coverage",
            "line_coverage",
            "branch_coverage",
            "new_coverage",
            "lines_to_cover",
            "uncovered_lines",
        ).joinToString(",")
        val query = "component=${urlEncode(sonarProjectKey)}&metricKeys=${urlEncode(metricKeys)}"

        return HttpRequest.newBuilder()
            .uri(URI.create("$sonarHostUrl/api/measures/component?$query"))
            .header("Authorization", "Basic $credentials")
            .timeout(Duration.ofSeconds(SonarConstants.REQUEST_TIMEOUT_SECONDS))
            .GET()
            .build()
    }

    private fun parsePayload(responseBody: String): Map<*, *> {
        return JsonSlurper().parseText(responseBody) as? Map<*, *>
            ?: throw GradleException("SonarQube coverage response was not a JSON object.")
    }

    private fun measureValue(measure: Map<*, *>): String? {
        val value = measure["value"]?.toString()
        if (value != null) {
            return value
        }

        val period = measure["period"] as? Map<*, *>
        return period?.get("value")?.toString()
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

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}
