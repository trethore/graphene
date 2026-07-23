package io.github.trethore.buildlogic.sonar

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SonarDuplicatesReportTest {
    @Test
    fun `skips duplicate details when no files are affected`() {
        val client = FakeSonarClient { request ->
            assertEquals("/api/measures/component", request.path)
            mapOf(
                "component" to mapOf(
                    "measures" to measures(
                        "duplicated_files" to "0",
                        "duplicated_lines" to "0",
                        "duplicated_lines_density" to "0.0",
                    )
                )
            )
        }

        val report = SonarDuplicatesLoader(client).load("graphene")

        assertEquals(1, client.requests.size)
        assertEquals(
            listOf(
                "Duplication summary:",
                "  Duplicate groups: 0",
                "  Affected files: 0",
                "  Duplicated lines: 0",
                "  Duplication density: 0.0%",
            ),
            SonarDuplicatesRenderer.render(report),
        )
    }

    @Test
    fun `deduplicates groups returned for multiple files`() {
        val client = FakeSonarClient { request ->
            when (request.path) {
                "/api/measures/component" -> duplicationSummary()
                "/api/measures/component_tree" -> duplicatedFiles()
                "/api/duplications/show" -> duplicateGroup()
                else -> error("Unexpected request: $request")
            }
        }

        val report = SonarDuplicatesLoader(client).load("graphene")

        assertEquals(1, report.groups.size)
        assertEquals(2, client.requests.count { it.path == "/api/duplications/show" })
        assertEquals(
            listOf(
                "Duplication summary:",
                "  Duplicate groups: 1",
                "  Affected files: 2",
                "  Duplicated lines: 6",
                "  Duplication density: 4.2%",
                "",
                "Duplicate groups:",
                "  1. 3 lines, 2 occurrences",
                "     - src/A.java:10:1 (lines 10-12, 3 lines)",
                "     - src/B.java:20:1 (lines 20-22, 3 lines)",
            ),
            SonarDuplicatesRenderer.render(report),
        )
        assertFalse(SonarDuplicatesRenderer.render(report).contains("Duplicates to fix:"))
    }

    private fun duplicationSummary(): Map<String, Any> {
        return mapOf(
            "component" to mapOf(
                "measures" to measures(
                    "duplicated_files" to "2",
                    "duplicated_lines" to "6",
                    "duplicated_lines_density" to "4.2",
                )
            )
        )
    }

    private fun duplicatedFiles(): Map<String, Any> {
        return mapOf(
            "components" to listOf(
                mapOf(
                    "key" to "graphene:A",
                    "path" to "src/A.java",
                    "measures" to measures("duplicated_lines" to "3"),
                ),
                mapOf(
                    "key" to "graphene:B",
                    "path" to "src/B.java",
                    "measures" to measures("duplicated_lines" to "3"),
                ),
            ),
            "paging" to mapOf("total" to 2),
        )
    }

    private fun duplicateGroup(): Map<String, Any> {
        return mapOf(
            "files" to mapOf(
                "1" to mapOf("key" to "graphene:A", "name" to "A.java"),
                "2" to mapOf("key" to "graphene:B", "name" to "B.java"),
            ),
            "duplications" to listOf(
                mapOf(
                    "blocks" to listOf(
                        mapOf("_ref" to "1", "from" to 10, "size" to 3),
                        mapOf("_ref" to "2", "from" to 20, "size" to 3),
                    )
                )
            ),
        )
    }
}
