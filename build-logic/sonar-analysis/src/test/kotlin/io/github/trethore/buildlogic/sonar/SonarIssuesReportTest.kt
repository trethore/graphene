package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SonarIssuesReportTest {
    @Test
    fun `loads all pages and renders all impacts and precise locations`() {
        val client = FakeSonarClient { request ->
            when (request.parameters["p"]) {
                "1" -> mapOf(
                    "total" to 2,
                    "issues" to listOf(
                        mapOf(
                            "component" to "graphene:src/Z.java",
                            "textRange" to mapOf(
                                "startLine" to 12,
                                "endLine" to 12,
                                "startOffset" to 4,
                                "endOffset" to 17,
                            ),
                            "severity" to "MAJOR",
                            "type" to "BUG",
                            "rule" to "java:S1",
                            "message" to "First\nissue",
                            "impacts" to listOf(
                                mapOf("severity" to "HIGH", "softwareQuality" to "RELIABILITY"),
                                mapOf("severity" to "MEDIUM", "softwareQuality" to "MAINTAINABILITY"),
                            ),
                        )
                    ),
                )
                "2" -> mapOf(
                    "total" to 2,
                    "issues" to listOf(
                        mapOf(
                            "component" to "graphene:src/A.java",
                            "line" to 7,
                            "severity" to "MINOR",
                            "type" to "CODE_SMELL",
                            "rule" to "java:S2",
                            "message" to "Second issue",
                        )
                    ),
                )
                else -> error("Unexpected request: $request")
            }
        }

        val report = SonarIssuesLoader(client).load("graphene")

        assertEquals(2, client.requests.size)
        assertEquals(
            listOf(
                "src/A.java:7 [MINOR CODE_SMELL] java:S2 - Second issue",
                "src/Z.java:12:5-12:18 [MEDIUM MAINTAINABILITY, HIGH RELIABILITY] java:S1 - First issue",
            ),
            SonarIssuesRenderer.render(report),
        )
    }

    @Test
    fun `renders an empty issue report`() {
        val client = FakeSonarClient {
            mapOf("total" to 0, "issues" to emptyList<Any>())
        }

        val report = SonarIssuesLoader(client).load("graphene")

        assertEquals(
            listOf("No unresolved issues found."),
            SonarIssuesRenderer.render(report),
        )
    }

    @Test
    fun `rejects issues without required fields`() {
        val client = FakeSonarClient {
            mapOf(
                "total" to 1,
                "issues" to listOf(mapOf("component" to "graphene:src/A.java")),
            )
        }

        assertFailsWith<GradleException> {
            SonarIssuesLoader(client).load("graphene")
        }
    }
}
