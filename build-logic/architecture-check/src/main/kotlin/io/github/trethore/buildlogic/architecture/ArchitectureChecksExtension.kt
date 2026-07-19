package io.github.trethore.buildlogic.architecture

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

open class ArchitectureChecksExtension
@Inject
constructor(
    private val project: Project,
    private val aggregateTask: TaskProvider<Task>,
) {
  @Suppress("unused")
  fun register(name: String, action: Action<CheckImportsTask>): TaskProvider<CheckImportsTask> {
    val taskName = "check${name.replaceFirstChar(Char::uppercaseChar)}Architecture"
    val task = project.tasks.register(taskName, CheckImportsTask::class.java, action)
    aggregateTask.configure { dependsOn(task) }
    return task
  }
}
