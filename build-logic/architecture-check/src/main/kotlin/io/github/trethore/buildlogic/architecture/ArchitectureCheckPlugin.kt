package io.github.trethore.buildlogic.architecture

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

@Suppress("unused")
class ArchitectureCheckPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val aggregateTask =
        project.tasks.register("checkArchitecture") {
          group = LifecycleBasePlugin.VERIFICATION_GROUP
          description = "Checks the project's architectural dependency boundaries."
        }

    project.extensions.create(
        "architectureChecks",
        ArchitectureChecksExtension::class.java,
        project,
        aggregateTask,
    )

    project.plugins.withId("base") {
      project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
        dependsOn(aggregateTask)
      }
    }
  }
}
