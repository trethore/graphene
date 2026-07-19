package io.github.trethore.buildlogic.architecture

import java.io.File

internal object ImportScanner {
  fun findViolations(
      sourceFile: File,
      forbiddenPrefixes: List<String>,
      allowedTypes: Set<String> = emptySet(),
  ): List<ImportViolation> =
      sourceFile.readLines().mapIndexedNotNull { index, line ->
        val importedType = importedType(line) ?: return@mapIndexedNotNull null
        if (importedType in allowedTypes) {
          return@mapIndexedNotNull null
        }
        if (forbiddenPrefixes.none(importedType::startsWith)) {
          return@mapIndexedNotNull null
        }

        ImportViolation(index + 1, line.trim())
      }

  private fun importedType(line: String): String? {
    val trimmedLine = line.trim()
    if (!trimmedLine.startsWith("import ")) {
      return null
    }

    return trimmedLine
        .removePrefix("import ")
        .removePrefix("static ")
        .removeSuffix(";")
        .trim()
  }
}

internal data class ImportViolation(val lineNumber: Int, val line: String)
