package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SonarClientTest {
    @Test
    fun `reads component tree pages and transforms components`() {
        val client = FakeSonarClient { request ->
            when (request.parameters["p"]) {
                "1" -> mapOf(
                    "components" to listOf(mapOf("name" to "one")),
                    "paging" to mapOf("total" to 2),
                )
                "2" -> mapOf(
                    "components" to listOf(mapOf("name" to "two")),
                    "paging" to mapOf("total" to 2),
                )
                else -> error("Unexpected request: $request")
            }
        }

        val names = client.getComponentTreeComponents(
            parameters = mapOf("component" to "graphene"),
            responseName = "files",
        ) { component -> component["name"]?.toString() }

        assertEquals(listOf("one", "two"), names)
        assertEquals(SonarConstants.PAGE_SIZE.toString(), client.requests.first().parameters["ps"])
    }

    @Test
    fun `rejects an empty component page before the reported total`() {
        val client = FakeSonarClient {
            mapOf(
                "components" to emptyList<Any>(),
                "paging" to mapOf("total" to 1),
            )
        }

        assertFailsWith<GradleException> {
            client.getComponentTreeComponents(emptyMap(), "files") { it }
        }
    }

    @Test
    fun `reads direct and period measure values`() {
        val values = sonarMeasureValues(
            mapOf(
                "measures" to listOf(
                    mapOf("metric" to "coverage", "value" to "80.0"),
                    mapOf("metric" to "new_coverage", "period" to mapOf("value" to "90.0")),
                    mapOf("value" to "ignored"),
                )
            )
        )

        assertEquals(
            mapOf("coverage" to "80.0", "new_coverage" to "90.0"),
            values,
        )
    }
}
