package io.github.trethore.buildlogic.sonar

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class SonarDuplicatesTask : DefaultTask() {
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
        val client = SonarApiClient.create(hostUrl.get(), token.orNull)
        client.waitForAnalysis(reportTaskFile.get().asFile)
        val report = SonarDuplicatesLoader(client).load(projectKey.get())
        SonarDuplicatesRenderer.render(report).forEach(logger::lifecycle)
    }
}
