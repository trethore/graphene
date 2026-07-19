package io.github.trethore.buildlogic.architecture

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CheckImportsTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sources: ConfigurableFileCollection

  @get:Input abstract val forbiddenImports: ListProperty<String>

  @get:Input abstract val allowedImports: ListProperty<String>

  @get:Input abstract val failureMessage: Property<String>

  init {
    group = "verification"
    allowedImports.convention(emptyList())
    failureMessage.convention("Forbidden imports violate this project's architecture.")
  }

  @TaskAction
  fun checkImports() {
    val forbiddenPrefixes = forbiddenImports.get().toList()
    val allowedTypes = allowedImports.get().toSet()
    val violations =
        sources.files.sortedBy { it.path }.flatMap { sourceFile ->
          ImportScanner.findViolations(sourceFile, forbiddenPrefixes, allowedTypes).map { violation ->
            "${sourceFile.relativeTo(project.projectDir)}:${violation.lineNumber}: ${violation.line}"
          }
        }

    check(violations.isEmpty()) {
      buildString {
        appendLine(failureMessage.get())
        appendLine()
        violations.forEach(::appendLine)
      }
    }
  }
}
