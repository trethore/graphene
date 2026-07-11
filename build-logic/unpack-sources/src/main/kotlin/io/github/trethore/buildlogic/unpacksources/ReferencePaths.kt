package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest

internal object ReferencePaths {
    fun relativeToRoot(project: Project, file: File): String {
        return file.relativeTo(project.rootProject.rootDir).path
    }

    fun safePathSegment(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "reference" }
    }

    fun shortHash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.take(6).joinToString("") { byte -> "%02x".format(byte) }
    }
}
