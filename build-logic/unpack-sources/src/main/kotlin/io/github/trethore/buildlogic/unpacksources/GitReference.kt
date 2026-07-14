package io.github.trethore.buildlogic.unpacksources

internal data class GitReference(
    val name: String?,
    val url: String,
    val branch: String,
    val commit: String?,
    val sparsePaths: List<String>,
) {
    init {
        sparsePaths.forEach { path ->
            require(path.isNotBlank()) { "Sparse Git paths must not be blank." }
            require(!path.startsWith('/')) { "Sparse Git paths must be relative: $path" }
            require(path.split('/').none { segment -> segment == ".." }) {
                "Sparse Git paths must not contain parent traversal: $path"
            }
        }
    }

    fun serialize(): String {
        val fields = listOf(
            encode(name),
            encode(url),
            encode(branch),
            encode(commit),
            sparsePaths.joinToString(LIST_SEPARATOR) { path -> encode(path) },
        )
        return fields.joinToString(SEPARATOR)
    }

    companion object {
        private const val NULL_VALUE = "%00"
        private const val LIST_SEPARATOR = "\u001E"
        private const val SEPARATOR = "\u001F"

        fun deserialize(value: String): GitReference {
            val fields = value.split(SEPARATOR)
            require(fields.size == 4 || fields.size == 5) { "Invalid serialized Git reference." }
            return GitReference(
                name = decode(fields[0]),
                url = requireNotNull(decode(fields[1])),
                branch = requireNotNull(decode(fields[2])),
                commit = decode(fields[3]),
                sparsePaths = if (fields.size == 5 && fields[4].isNotEmpty()) {
                    fields[4].split(LIST_SEPARATOR).map { path -> requireNotNull(decode(path)) }
                } else {
                    emptyList()
                },
            )
        }

        private fun encode(value: String?): String {
            return value
                ?.replace("%", "%25")
                ?.replace(LIST_SEPARATOR, "%1E")
                ?.replace(SEPARATOR, "%1F")
                ?: NULL_VALUE
        }

        private fun decode(value: String): String? {
            if (value == NULL_VALUE) {
                return null
            }
            return value
                .replace("%1F", SEPARATOR)
                .replace("%1E", LIST_SEPARATOR)
                .replace("%25", "%")
        }
    }
}
