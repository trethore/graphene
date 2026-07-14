package io.github.trethore.buildlogic.unpacksources

import javax.inject.Inject

open class ReferencesExtension @Inject constructor() {
    var unpackNestedJars: Boolean = false

    internal val gitReferences = mutableListOf<GitReference>()

    @Suppress("unused")
    fun git(
        url: String,
        branch: String,
        name: String? = null,
        commit: String? = null,
        sparsePaths: List<String> = emptyList(),
    ) {
        gitReferences += GitReference(
            name = name,
            url = url,
            branch = branch,
            commit = commit,
            sparsePaths = sparsePaths.distinct(),
        )
    }
}
