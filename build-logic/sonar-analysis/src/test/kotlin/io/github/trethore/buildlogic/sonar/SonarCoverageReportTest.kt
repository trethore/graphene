package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SonarCoverageReportTest {
    @Test
    fun `loads and renders uncovered line ranges`() {
        val client = FakeSonarClient { request ->
            when (request.path) {
                "/api/measures/component" -> mapOf(
                    "component" to mapOf(
                        "measures" to measures(
                            "coverage" to "80.0",
                            "line_coverage" to "85.0",
                            "branch_coverage" to "70.0",
                            "lines_to_cover" to "20",
                            "uncovered_lines" to "3",
                        )
                    )
                )
                "/api/measures/component_tree" -> mapOf(
                    "components" to listOf(
                        mapOf(
                            "key" to "graphene:src/Foo.java",
                            "path" to "src/Foo.java",
                            "measures" to measures(
                                "line_coverage" to "78.6",
                                "lines_to_cover" to "14",
                                "uncovered_lines" to "3",
                            ),
                        ),
                        mapOf(
                            "key" to "graphene:src/Complete.java",
                            "path" to "src/Complete.java",
                            "measures" to measures(
                                "line_coverage" to "100.0",
                                "lines_to_cover" to "8",
                                "uncovered_lines" to "0",
                            ),
                        ),
                    ),
                    "paging" to mapOf("total" to 2),
                )
                "/api/sources/lines" -> mapOf(
                    "sources" to listOf(
                        mapOf("line" to 8, "lineHits" to 0),
                        mapOf("line" to 9, "lineHits" to 0),
                        mapOf("line" to 10, "lineHits" to 2),
                        mapOf("line" to 12, "lineHits" to 0),
                        mapOf("line" to 13),
                    )
                )
                else -> error("Unexpected request: $request")
            }
        }

        val report = SonarCoverageLoader(client).load("graphene")

        assertEquals(
            listOf(SonarLineRange(8, 9), SonarLineRange(12, 12)),
            report.files.single().uncoveredLineRanges,
        )
        assertEquals(
            listOf(
                "Overall: 80.0%",
                "Lines: 17/20 covered, 3 uncovered (85.0%)",
                "Branches: 70.0%",
                "src/Foo.java: 11/14 covered, to cover: 8-9, 12",
            ),
            SonarCoverageRenderer.render(report),
        )
        assertEquals("uncovered_lines", client.requests[1].parameters["metricSort"])
        assertEquals("lines_to_cover,uncovered_lines", client.requests[1].parameters["metricKeys"])
        assertEquals("false", client.requests[1].parameters["asc"])
        assertEquals("graphene:src/Foo.java", client.requests[2].parameters["key"])
        assertFalse(client.requests.first().parameters["metricKeys"].orEmpty().contains("new_coverage"))
    }

    @Test
    fun `does not load files when all lines are covered`() {
        val client = FakeSonarClient { request ->
            assertEquals("/api/measures/component", request.path)
            mapOf(
                "component" to mapOf(
                    "measures" to measures(
                        "line_coverage" to "100.0",
                        "lines_to_cover" to "10",
                        "uncovered_lines" to "0",
                    )
                )
            )
        }

        val report = SonarCoverageLoader(client).load("graphene")

        assertEquals(1, client.requests.size)
        assertEquals(emptyList(), report.files)
        assertFalse(SonarCoverageRenderer.render(report).any { line -> line.contains("to cover:") })
    }

    @Test
    fun `renders missing measures`() {
        val client = FakeSonarClient {
            mapOf("component" to mapOf("measures" to emptyList<Any>()))
        }

        val report = SonarCoverageLoader(client).load("graphene")

        assertEquals(
            listOf("No coverage measures found."),
            SonarCoverageRenderer.render(report),
        )
    }

    @Test
    fun `rejects inconsistent coverage counts`() {
        val client = FakeSonarClient {
            mapOf(
                "component" to mapOf(
                    "measures" to measures(
                        "lines_to_cover" to "2",
                        "uncovered_lines" to "3",
                    )
                )
            )
        }

        assertFailsWith<GradleException> {
            SonarCoverageLoader(client).load("graphene")
        }
    }
}
