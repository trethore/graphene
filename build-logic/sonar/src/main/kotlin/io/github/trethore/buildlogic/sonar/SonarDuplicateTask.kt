package io.github.trethore.buildlogic.sonar

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class SonarDuplicateTask : DefaultTask() {
    private companion object {
        const val DUPLICATED_BLOCKS = "duplicated_blocks"
        const val DUPLICATED_FILES = "duplicated_files"
        const val DUPLICATED_LINES = "duplicated_lines"
        const val DUPLICATED_LINES_DENSITY = "duplicated_lines_density"
        const val FILE_METRIC_KEYS = "$DUPLICATED_BLOCKS,$DUPLICATED_LINES,$DUPLICATED_LINES_DENSITY"
        const val NOT_AVAILABLE = "not available"
        const val SUMMARY_METRIC_KEYS = "$FILE_METRIC_KEYS,$DUPLICATED_FILES"
    }

    @get:Input
    abstract val hostUrl: Property<String>

    @get:Input
    abstract val projectKey: Property<String>

    @get:Internal
    abstract val token: Property<String>

    @get:Internal
    abstract val reportTaskFile: RegularFileProperty

    @TaskAction
    fun showDuplicates() {
        val sonarProjectKey = projectKey.get()
        val client = SonarApiClient.create(hostUrl.get(), token.orNull)
        client.waitForAnalysis(reportTaskFile.get().asFile)

        val summary = fetchSummary(client, sonarProjectKey)
        logger.lifecycle("Duplicated blocks: ${summary[DUPLICATED_BLOCKS] ?: NOT_AVAILABLE}")
        logger.lifecycle("Duplicated lines: ${summary[DUPLICATED_LINES] ?: NOT_AVAILABLE}")
        logger.lifecycle("Duplicated lines: ${formatPercentage(summary[DUPLICATED_LINES_DENSITY])}")
        logger.lifecycle("Duplicated files: ${summary[DUPLICATED_FILES] ?: NOT_AVAILABLE}")

        if (summary[DUPLICATED_FILES]?.toIntOrNull() == 0) {
            return
        }

        val duplicatedFiles = fetchDuplicatedFiles(client, sonarProjectKey)
        if (duplicatedFiles.isEmpty()) {
            return
        }

        logger.lifecycle("")
        logger.lifecycle("Duplicated files:")
        duplicatedFiles.forEach { duplicatedFile ->
            logger.lifecycle("  ${duplicatedFile.path}")
            logger.lifecycle("    Duplicated blocks: ${duplicatedFile.blocks}")
            logger.lifecycle("    Duplicated lines: ${duplicatedFile.lines}")
            logger.lifecycle("    Duplicated lines: ${formatPercentage(duplicatedFile.density)}")
        }
    }

    private fun fetchSummary(
        client: SonarApiClient,
        sonarProjectKey: String,
    ): Map<String, String> {
        val payload = client.get(
            path = "/api/measures/component",
            parameters = mapOf("component" to sonarProjectKey, "metricKeys" to SUMMARY_METRIC_KEYS),
            responseName = "duplication summary",
        )
        val component = payload["component"] as? Map<*, *>
            ?: throw GradleException("SonarQube duplication summary response did not contain a component.")

        return sonarMeasureValues(component)
    }

    private fun fetchDuplicatedFiles(
        client: SonarApiClient,
        sonarProjectKey: String,
    ): List<DuplicatedFile> {
        val duplicatedFiles = mutableListOf<DuplicatedFile>()
        var page = 1

        while (true) {
            if (page > SonarConstants.MAX_PAGES) {
                throw GradleException(
                    "SonarQube duplication response exceeded ${SonarConstants.MAX_PAGES} pages."
                )
            }

            val payload = client.get(
                path = "/api/measures/component_tree",
                parameters = mapOf(
                    "component" to sonarProjectKey,
                    "metricKeys" to FILE_METRIC_KEYS,
                    "qualifiers" to "FIL",
                    "strategy" to "leaves",
                    "metricSort" to DUPLICATED_LINES,
                    "metricSortFilter" to "withMeasuresOnly",
                    "s" to "metric",
                    "asc" to "false",
                    "p" to page.toString(),
                    "ps" to SonarConstants.PAGE_SIZE.toString(),
                ),
                responseName = "duplication files",
            )
            val components = (payload["components"] as? List<*>)
                .orEmpty()
                .filterIsInstance<Map<*, *>>()

            components.mapNotNullTo(duplicatedFiles, ::duplicatedFile)

            val paging = payload["paging"] as? Map<*, *>
                ?: throw GradleException("SonarQube duplication files response did not contain paging information.")
            val total = (paging["total"] as? Number)?.toInt()
                ?: throw GradleException("SonarQube duplication files response did not contain a total.")
            val pageSize = (paging["pageSize"] as? Number)?.toInt()
                ?: throw GradleException("SonarQube duplication files response did not contain a page size.")

            if (page * pageSize >= total) {
                return duplicatedFiles
            }

            page += 1
        }
    }

    private fun duplicatedFile(component: Map<*, *>): DuplicatedFile? {
        val measures = sonarMeasureValues(component)
        val blocks = measures[DUPLICATED_BLOCKS]?.toIntOrNull() ?: 0
        val lines = measures[DUPLICATED_LINES]?.toIntOrNull() ?: 0
        if (blocks == 0 && lines == 0) {
            return null
        }

        val path = component["path"]?.toString()
            ?: component["name"]?.toString()
            ?: component["key"]?.toString()
            ?: "unknown"
        val density = measures[DUPLICATED_LINES_DENSITY]

        return DuplicatedFile(path, blocks, lines, density)
    }

    private fun formatPercentage(value: String?): String = value?.let { "$it%" } ?: NOT_AVAILABLE

    private data class DuplicatedFile(
        val path: String,
        val blocks: Int,
        val lines: Int,
        val density: String?,
    )
}
