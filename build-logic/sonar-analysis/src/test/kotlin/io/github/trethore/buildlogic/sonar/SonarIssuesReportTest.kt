package io.github.trethore.buildlogic.sonar

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SonarIssuesReportTest {
    @Test
    fun `loads every page and renders legacy and impact fields`() {
        val client = FakeSonarClient { request ->
            when (request.parameters["p"]) {
                "1" -> mapOf(
                    "total" to 2,
                    "issues" to listOf(
                        mapOf(
                            "component" to "graphene:src/Foo.java",
                            "line" to 12,
                            "severity" to "MAJOR",
                            "type" to "BUG",
                            "rule" to "java:S1",
                            "message" to "First\nissue",
                        )
                    ),
                )
                "2" -> mapOf(
                    "total" to 2,
                    "issues" to listOf(
                        mapOf(
                            "component" to "graphene:src/Bar.java",
                            "rule" to "java:S2",
                            "message" to "Second issue",
                            "impacts" to listOf(
                                mapOf(
                                    "severity" to "HIGH",
                                    "softwareQuality" to "MAINTAINABILITY",
                                )
                            ),
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
                "Unresolved SonarQube issues for graphene: 2",
                "MAJOR BUG src/Foo.java:12 java:S1 - First issue",
                "HIGH MAINTAINABILITY src/Bar.java:- java:S2 - Second issue",
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
            listOf("No unresolved SonarQube issues found for graphene."),
            SonarIssuesRenderer.render(report),
        )
    }
}
