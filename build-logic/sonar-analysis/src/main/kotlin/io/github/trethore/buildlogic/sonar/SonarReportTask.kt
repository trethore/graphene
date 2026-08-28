package io.github.trethore.buildlogic.sonar

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

enum class SonarReportKind {
    ISSUES,
    COVERAGE,
    DUPLICATES,
}

abstract class SonarReportTask : DefaultTask() {
    @get:Input
    abstract val hostUrl: Property<String>

    @get:Input
    abstract val projectKey: Property<String>

    @get:Input
    abstract val reportKind: Property<SonarReportKind>

    @get:Internal
    abstract val token: Property<String>

    @get:Internal
    abstract val reportTaskFile: RegularFileProperty

    @TaskAction
    fun report() {
        val client = SonarApiClient.create(hostUrl.get(), token.orNull)
        client.waitForAnalysis(reportTaskFile.get().asFile)
        val projectKey = projectKey.get()
        val lines = when (reportKind.get()) {
            SonarReportKind.ISSUES -> SonarIssuesRenderer.render(SonarIssuesLoader(client).load(projectKey))
            SonarReportKind.COVERAGE -> SonarCoverageRenderer.render(SonarCoverageLoader(client).load(projectKey))
            SonarReportKind.DUPLICATES -> SonarDuplicatesRenderer.render(
                SonarDuplicatesLoader(client).load(projectKey)
            )
        }
        lines.forEach(logger::lifecycle)
    }
}
