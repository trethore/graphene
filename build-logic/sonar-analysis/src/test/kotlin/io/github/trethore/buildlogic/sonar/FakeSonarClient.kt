package io.github.trethore.buildlogic.sonar

import java.io.File

internal data class SonarRequest(
    val path: String,
    val parameters: Map<String, String>,
    val responseName: String,
)

internal class FakeSonarClient(
    private val responder: (SonarRequest) -> Map<*, *>,
) : SonarClient {
    val requests = mutableListOf<SonarRequest>()
    val waitedForFiles = mutableListOf<File>()

    override fun get(
        path: String,
        parameters: Map<String, String>,
        responseName: String,
    ): Map<*, *> {
        val request = SonarRequest(path, parameters, responseName)
        requests += request
        return responder(request)
    }

    override fun waitForAnalysis(reportTaskFile: File) {
        waitedForFiles += reportTaskFile
    }
}

internal fun measures(vararg values: Pair<String, Any>): List<Map<String, Any>> {
    return values.map { (metric, value) -> mapOf("metric" to metric, "value" to value) }
}
