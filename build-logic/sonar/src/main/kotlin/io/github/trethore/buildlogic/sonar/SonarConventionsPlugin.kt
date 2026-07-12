package io.github.trethore.buildlogic.sonar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
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

        project.extensions.configure<SonarExtension> {
            properties {
                property("sonar.projectKey", project.rootProject.name)
                property("sonar.projectName", project.rootProject.name)
                property("sonar.host.url", sonarHostUrl.get())
                property("sonar.inclusions", "**/*.java")
                property("sonar.exclusions", "references/**,**/build/**")
                property("sonar.gradle.scanAll", "false")

                val token = sonarToken.orNull
                if (!token.isNullOrBlank()) {
                    property("sonar.token", token)
                }
            }
        }

        project.tasks.register<SonarIssuesTask>(SonarConstants.ISSUES_TASK_NAME) {
            group = SonarConstants.TASK_GROUP
            description = "Runs SonarQube analysis and lists unresolved issues for this project."
            dependsOn(SonarConstants.SONAR_TASK_NAME)
            hostUrl.set(sonarHostUrl)
            projectKey.set(project.rootProject.name)
            token.set(sonarToken)
        }

        project.subprojects.forEach { subproject ->
            subproject.plugins.withType<JavaPlugin> {
                val subprojectClasses = subproject.tasks.named(JavaPlugin.CLASSES_TASK_NAME)
                val subprojectTest = subproject.tasks.named(JavaPlugin.TEST_TASK_NAME)
                project.tasks.named(SonarConstants.SONAR_TASK_NAME) {
                    dependsOn(subprojectClasses, subprojectTest)
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
