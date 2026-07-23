package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException
import java.io.File

internal interface SonarClient {
    fun get(
        path: String,
        parameters: Map<String, String>,
        responseName: String,
    ): Map<*, *>

    fun waitForAnalysis(reportTaskFile: File)
}

internal fun <T> SonarClient.getComponentTreeComponents(
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
