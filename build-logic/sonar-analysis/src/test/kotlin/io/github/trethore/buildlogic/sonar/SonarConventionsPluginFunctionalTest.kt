package io.github.trethore.buildlogic.sonar

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class SonarConventionsPluginFunctionalTest {
    @TempDir
    lateinit var projectDirectory: Path

    @Test
    fun `wires reporting tasks and java subproject coverage`() {
        projectDirectory.resolve("settings.gradle").writeText(
            """
            rootProject.name = 'test-project'
            include 'app'
            """.trimIndent()
        )
        projectDirectory.resolve("build.gradle").writeText(
            """
            plugins {
                id 'example.sonar'
            }

            tasks.register('verifySonarWiring') {
                doLast {
                    def sonarTask = tasks.named('sonar').get()
                    def expectedTasks = [
                        sonarIssues: 'Runs SonarQube analysis and lists unresolved issues for this project.',
                        sonarCoverage: 'Runs SonarQube analysis and shows coverage for this project.',
                        sonarDuplicates: 'Runs SonarQube analysis and reports duplicated code for this project.'
                    ]
                    def expectedReportKinds = [
                        sonarIssues: 'ISSUES',
                        sonarCoverage: 'COVERAGE',
                        sonarDuplicates: 'DUPLICATES'
                    ]
                    expectedTasks.each { name, expectedDescription ->
                        def reportTask = tasks.named(name).get()
                        assert reportTask.group == 'verification'
                        assert reportTask.description == expectedDescription
                        assert reportTask.reportKind.get().name() == expectedReportKinds[name]
                        assert reportTask.taskDependencies.getDependencies(reportTask).contains(sonarTask)
                    }

                    def sonarComposeFile = rootProject.file('compose.sonar.yml').absolutePath
                    def sonarUp = tasks.named('sonarUp').get()
                    assert sonarUp.group == 'sonar'
                    assert sonarUp.description == 'Starts the local SonarQube instance.'
                    assert sonarUp.commandLine == ['docker', 'compose', '-f', sonarComposeFile, 'up', '-d']
                    def sonarDown = tasks.named('sonarDown').get()
                    assert sonarDown.group == 'sonar'
                    assert sonarDown.description == 'Stops the local SonarQube instance.'
                    assert sonarDown.commandLine == ['docker', 'compose', '-f', sonarComposeFile, 'down']

                    def appProject = project(':app')
                    assert appProject.plugins.hasPlugin('jacoco')
                    def classesTask = appProject.tasks.named('classes').get()
                    def coverageTask = appProject.tasks.named('jacocoTestReport').get()
                    assert coverageTask.reports.xml.required.get()
                    def sonarDependencies = sonarTask.taskDependencies.getDependencies(sonarTask)
                    assert sonarDependencies.contains(classesTask)
                    assert sonarDependencies.contains(coverageTask)
                }
            }
            """.trimIndent()
        )
        projectDirectory.resolve("app").createDirectories()
        projectDirectory.resolve("app/build.gradle").writeText("plugins { id 'java' }")
        projectDirectory.resolve("config/sonar").createDirectories()
        projectDirectory.resolve("config/sonar/analysis.json").writeText("{}")

        val result = GradleRunner.create()
            .withProjectDir(projectDirectory.toFile())
            .withPluginClasspath()
            .withArguments("verifySonarWiring", "--stacktrace")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":verifySonarWiring")?.outcome)
    }
}
