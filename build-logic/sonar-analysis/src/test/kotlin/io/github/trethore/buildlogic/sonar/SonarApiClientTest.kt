package io.github.trethore.buildlogic.sonar

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.api.GradleException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SonarApiClientTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    private val servers = mutableListOf<HttpServer>()

    @AfterEach
    fun stopServers() {
        servers.forEach { server -> server.stop(0) }
    }

    @Test
    fun `sends authenticated requests with encoded query parameters`() {
        var authorization: String? = null
        var rawQuery: String? = null
        val server = startServer { exchange ->
            authorization = exchange.requestHeaders.getFirst("Authorization")
            rawQuery = exchange.requestURI.rawQuery
            respond(exchange, 200, """{"component":{"measures":[]}}""")
        }
        val token = "secret-token"
        val client = SonarApiClient.create("${serverUrl(server)}/", token)

        val response = client.get(
            path = "/api/measures/component",
            parameters = mapOf(
                "component" to "graphene project",
                "metricKeys" to "line_coverage,uncovered_lines",
            ),
            responseName = "coverage",
        )

        assertTrue(response.containsKey("component"))
        assertEquals(
            "Basic " + Base64.getEncoder().encodeToString("$token:".toByteArray(StandardCharsets.UTF_8)),
            authorization,
        )
        assertEquals(
            "component=graphene+project&metricKeys=line_coverage%2Cuncovered_lines",
            rawQuery,
        )
    }

    @Test
    fun `reports non-successful responses`() {
        val server = startServer { exchange -> respond(exchange, 503, "Unavailable") }
        val client = SonarApiClient.create(serverUrl(server), "token")

        val exception = assertFailsWith<GradleException> {
            client.get("/api/test", emptyMap(), "test response")
        }

        assertTrue(exception.message.orEmpty().contains("HTTP 503"))
        assertTrue(exception.message.orEmpty().contains("Unavailable"))
    }

    @Test
    fun `reports invalid JSON responses`() {
        val server = startServer { exchange -> respond(exchange, 200, "not-json") }
        val client = SonarApiClient.create(serverUrl(server), "token")

        val exception = assertFailsWith<GradleException> {
            client.get("/api/test", emptyMap(), "test response")
        }

        assertTrue(exception.message.orEmpty().contains("was not valid JSON"))
    }

    @Test
    fun `waits for successful analysis`() {
        val server = startServer { exchange ->
            respond(exchange, 200, """{"task":{"status":"SUCCESS"}}""")
        }
        val reportFile = temporaryDirectory.resolve("report-task.txt")
        reportFile.writeText("ceTaskUrl=${serverUrl(server)}/api/ce/task?id=1")

        SonarApiClient.create(serverUrl(server), "token").waitForAnalysis(reportFile.toFile())
    }

    @Test
    fun `reports failed analysis`() {
        val server = startServer { exchange ->
            respond(
                exchange,
                200,
                """{"task":{"status":"FAILED","errorMessage":"Analysis error"}}""",
            )
        }
        val reportFile = temporaryDirectory.resolve("report-task.txt")
        reportFile.writeText("ceTaskUrl=${serverUrl(server)}/api/ce/task?id=1")

        val exception = assertFailsWith<GradleException> {
            SonarApiClient.create(serverUrl(server), "token").waitForAnalysis(reportFile.toFile())
        }

        assertTrue(exception.message.orEmpty().contains("FAILED: Analysis error"))
    }

    @Test
    fun `requires a token and analysis metadata`() {
        assertFailsWith<GradleException> {
            SonarApiClient.create("http://localhost", null)
        }

        val client = SonarApiClient.create("http://localhost", "token")
        assertFailsWith<GradleException> {
            client.waitForAnalysis(temporaryDirectory.resolve("missing.txt").toFile())
        }
    }

    private fun startServer(handler: (HttpExchange) -> Unit): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/", handler)
        server.start()
        servers += server
        return server
    }

    private fun serverUrl(server: HttpServer): String = "http://127.0.0.1:${server.address.port}"

    private fun respond(
        exchange: HttpExchange,
        status: Int,
        body: String,
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { responseBody -> responseBody.write(bytes) }
    }
}
