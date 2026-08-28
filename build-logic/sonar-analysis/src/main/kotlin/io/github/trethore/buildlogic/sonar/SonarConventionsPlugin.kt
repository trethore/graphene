package io.github.trethore.buildlogic.sonar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.sonarqube.gradle.SonarExtension

@Suppress("unused")
class SonarConventionsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(SonarConstants.PLUGIN_ID)

        val localEnv by lazy { project.readLocalEnv() }
        fun envValue(name: String) = project.providers.provider {
            project.providers.environmentVariable(name).orNull?.takeIf(String::isNotBlank)
                ?: localEnv[name]?.takeIf(String::isNotBlank)
        }

        val sonarToken = envValue(SonarConstants.TOKEN_ENV)
        val sonarHostUrl = envValue(SonarConstants.HOST_URL_ENV)
            .orElse(SonarConstants.DEFAULT_HOST_URL)
        val sonarMetadataFile = project.layout.buildDirectory.file("sonar/report-task.txt")
        val sonarConfiguration =
            SonarConfiguration.load(project.rootProject.file("config/sonar/analysis.json"))

        project.extensions.configure<SonarExtension> {
            properties {
                sonarConfiguration.properties.forEach { (name, value) -> property(name, value) }

                val issueExclusionIds = sonarConfiguration.issueExclusions.indices.map { "json${it + 1}" }
                if (issueExclusionIds.isNotEmpty()) {
                    property("sonar.issue.ignore.multicriteria", issueExclusionIds)
                    sonarConfiguration.issueExclusions.forEachIndexed { index, exclusion ->
                        val id = issueExclusionIds[index]
                        property("sonar.issue.ignore.multicriteria.$id.ruleKey", exclusion.ruleKey)
                        property("sonar.issue.ignore.multicriteria.$id.resourceKey", exclusion.filePattern)
                    }
                }

                property("sonar.projectKey", project.rootProject.name)
                property("sonar.projectName", project.rootProject.name)
                property("sonar.host.url", sonarHostUrl.get())
                property("sonar.scanner.metadataFilePath", sonarMetadataFile.get().asFile.absolutePath)

                val token = sonarToken.orNull
                if (!token.isNullOrBlank()) {
                    property("sonar.token", token)
                }
            }
        }

        fun registerReportTask(name: String, taskDescription: String, reportKind: SonarReportKind) {
            project.tasks.register<SonarReportTask>(name) {
                group = SonarConstants.TASK_GROUP
                description = taskDescription
                dependsOn(SonarConstants.SONAR_TASK_NAME)
                hostUrl.set(sonarHostUrl)
                projectKey.set(project.rootProject.name)
                this.reportKind.set(reportKind)
                reportTaskFile.set(sonarMetadataFile)
                token.set(sonarToken)
            }
        }

        registerReportTask(
            SonarConstants.ISSUES_TASK_NAME,
            "Runs SonarQube analysis and lists unresolved issues for this project.",
            SonarReportKind.ISSUES,
        )
        registerReportTask(
            SonarConstants.COVERAGE_TASK_NAME,
            "Runs SonarQube analysis and shows coverage for this project.",
            SonarReportKind.COVERAGE,
        )
        registerReportTask(
            SonarConstants.DUPLICATES_TASK_NAME,
            "Runs SonarQube analysis and reports duplicated code for this project.",
            SonarReportKind.DUPLICATES,
        )

        val sonarComposeFile = project.rootProject.file("compose.sonar.yml")
        project.tasks.register<Exec>(SonarConstants.SONAR_UP_TASK_NAME) {
            group = SonarConstants.TASK_GROUP_LOCAL
            description = "Starts the local SonarQube instance."
            commandLine("docker", "compose", "-f", sonarComposeFile, "up", "-d")
        }
        project.tasks.register<Exec>(SonarConstants.SONAR_DOWN_TASK_NAME) {
            group = SonarConstants.TASK_GROUP_LOCAL
            description = "Stops the local SonarQube instance."
            commandLine("docker", "compose", "-f", sonarComposeFile, "down")
        }

        project.subprojects.forEach { subproject ->
            subproject.plugins.withType<JavaPlugin> {
                val subprojectClasses = subproject.tasks.named(JavaPlugin.CLASSES_TASK_NAME)
                val subprojectTest = subproject.tasks.named(JavaPlugin.TEST_TASK_NAME)
                subproject.pluginManager.apply("jacoco")
                val subprojectCoverageReport = subproject.tasks.named<JacocoReport>("jacocoTestReport") {
                    dependsOn(subprojectTest)
                    reports.xml.required.set(true)
                }
                project.tasks.named(SonarConstants.SONAR_TASK_NAME) {
                    dependsOn(subprojectClasses, subprojectCoverageReport)
                }
            }
        }
    }

    private fun Project.readLocalEnv(): Map<String, String> {
        val envFile = rootProject.file(".env")
        if (!envFile.isFile) {
            return emptyMap()
        }

        return envFile.readLines()
            .asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator < 0) {
                    null
                } else {
                    val key = line.substring(0, separator).trim()
                    val value = line.substring(separator + 1).trim().trim('"', '\'')
                    key to value
                }
            }
            .toMap()
    }
}
