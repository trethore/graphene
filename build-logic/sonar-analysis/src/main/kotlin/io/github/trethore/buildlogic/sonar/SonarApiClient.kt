package io.github.trethore.buildlogic.sonar

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import java.io.File
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

internal class SonarApiClient private constructor(
    private val hostUrl: String,
    private val authorizationHeader: String,
    private val httpClient: HttpClient,
) {
    companion object {
        fun create(hostUrl: String, token: String?): SonarApiClient {
            if (token.isNullOrBlank()) {
                throw GradleException(
                    "${SonarConstants.TOKEN_ENV} is missing. Copy .env.example to .env and set a token."
                )
            }

            val credentials = Base64.getEncoder().encodeToString("$token:".toByteArray(StandardCharsets.UTF_8))
            val httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(SonarConstants.REQUEST_TIMEOUT_SECONDS))
                .build()
            return SonarApiClient(hostUrl.trimEnd('/'), "Basic $credentials", httpClient)
        }
    }

    fun get(
        path: String,
        parameters: Map<String, String>,
        responseName: String,
    ): Map<*, *> {
        val url = buildUrl("$hostUrl/${path.trimStart('/')}", parameters)
        return send(url, responseName)
    }

    fun <T> getComponentTreeComponents(
        parameters: Map<String, String>,
        responseName: String,
        transform: (Map<*, *>) -> T?,
    ): List<T> {
        val transformedComponents = mutableListOf<T>()
        var componentCount = 0
        var page = 1

        while (true) {
            if (page > SonarConstants.MAX_PAGES) {
                throw GradleException(
                    "SonarQube $responseName response exceeded ${SonarConstants.MAX_PAGES} pages."
                )
            }

            val payload = get(
                path = "/api/measures/component_tree",
                parameters = parameters + mapOf(
                    "p" to page.toString(),
                    "ps" to SonarConstants.PAGE_SIZE.toString(),
                ),
                responseName = responseName,
            )
            val pageComponents = (payload["components"] as? List<*>)
                .orEmpty()
                .filterIsInstance<Map<*, *>>()
            componentCount += pageComponents.size
            pageComponents.mapNotNullTo(transformedComponents, transform)

            val paging = payload["paging"] as? Map<*, *>
                ?: throw GradleException("SonarQube $responseName response did not contain paging information.")
            val total = (paging["total"] as? Number)?.toInt()
                ?: throw GradleException("SonarQube $responseName response did not contain a total.")

            if (componentCount >= total) {
                return transformedComponents
            }
            if (pageComponents.isEmpty()) {
                throw GradleException(
                    "SonarQube returned an empty $responseName page before the reported total was reached."
                )
            }

            page += 1
        }
    }

    fun waitForAnalysis(reportTaskFile: File) {
        if (!reportTaskFile.isFile) {
            throw GradleException("SonarQube analysis metadata was not found at $reportTaskFile.")
        }

        val metadata = Properties().apply {
            reportTaskFile.inputStream().use(::load)
        }
        val ceTaskUrl = metadata.getProperty("ceTaskUrl")
            ?: throw GradleException("SonarQube analysis metadata did not contain ceTaskUrl.")
        val timeoutNanoseconds = TimeUnit.SECONDS.toNanos(SonarConstants.ANALYSIS_TIMEOUT_SECONDS)
        val deadline = System.nanoTime() + timeoutNanoseconds

        while (true) {
            val payload = send(ceTaskUrl, "analysis status")
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

    private fun send(url: String, responseName: String): Map<*, *> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", authorizationHeader)
            .timeout(Duration.ofSeconds(SonarConstants.REQUEST_TIMEOUT_SECONDS))
            .GET()
            .build()
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw GradleException("Interrupted while requesting SonarQube $responseName.", exception)
        }

        if (response.statusCode() !in 200..299) {
            throw GradleException(
                "SonarQube $responseName request failed with HTTP ${response.statusCode()}: ${response.body()}"
            )
        }

        return JsonSlurper().parseText(response.body()) as? Map<*, *>
            ?: throw GradleException("SonarQube $responseName response was not a JSON object.")
    }

    private fun sleepBeforeRetry() {
        try {
            Thread.sleep(SonarConstants.STATUS_POLL_INTERVAL_MILLISECONDS)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw GradleException("Interrupted while waiting for SonarQube analysis.", exception)
        }
    }

    private fun buildUrl(url: String, parameters: Map<String, String>): String {
        if (parameters.isEmpty()) {
            return url
        }

        val query = parameters.entries.joinToString("&") { (name, value) ->
            "${urlEncode(name)}=${urlEncode(value)}"
        }
        return "$url?$query"
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
}

internal fun sonarMeasureValues(component: Map<*, *>): Map<String, String> {
    return (component["measures"] as? List<*>)
        .orEmpty()
        .filterIsInstance<Map<*, *>>()
        .mapNotNull { measure ->
            val metric = measure["metric"]?.toString() ?: return@mapNotNull null
            val value = measure["value"]?.toString()
                ?: (measure["period"] as? Map<*, *>)?.get("value")?.toString()
                ?: return@mapNotNull null
            metric to value
        }
        .toMap()
}
