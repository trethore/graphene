package io.github.trethore.buildlogic.unpacksources

import java.io.File

internal object ArchiveFiles {
    fun isArchive(file: File): Boolean {
        return file.extension.lowercase() in UnpackSourcesConstants.ARCHIVE_EXTENSIONS
    }
}
