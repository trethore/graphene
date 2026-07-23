package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SonarConfigurationTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `loads properties and issue exclusions`() {
        val configurationFile = temporaryDirectory.resolve("analysis.json")
        configurationFile.writeText(
            """
            {
              "properties": {
                "sonar.inclusions": ["**/*.java"],
                "sonar.gradle.scanAll": false
              },
              "issueExclusions": [
                {
                  "ruleKey": "java:S4032",
                  "filePattern": "**/*"
                }
              ]
            }
            """.trimIndent()
        )

        val configuration = SonarConfiguration.load(configurationFile.toFile())

        assertEquals(listOf("**/*.java"), configuration.properties["sonar.inclusions"])
        assertEquals(false, configuration.properties["sonar.gradle.scanAll"])
        assertEquals(
            listOf(SonarIssueExclusion("java:S4032", "**/*")),
            configuration.issueExclusions,
        )
    }

    @Test
    fun `rejects unknown configuration keys`() {
        val configurationFile = temporaryDirectory.resolve("analysis.json")
        configurationFile.writeText("""{"unknown": true}""")

        assertFailsWith<GradleException> {
            SonarConfiguration.load(configurationFile.toFile())
        }
    }

    @Test
    fun `rejects direct multicriteria properties`() {
        val configurationFile = temporaryDirectory.resolve("analysis.json")
        configurationFile.writeText(
            """
            {
              "properties": {
                "sonar.issue.ignore.multicriteria": "ignored"
              }
            }
            """.trimIndent()
        )

        assertFailsWith<GradleException> {
            SonarConfiguration.load(configurationFile.toFile())
        }
    }
}
