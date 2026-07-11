package io.github.trethore.buildlogic.unpacksources

internal data class GitReference(
    val name: String?,
    val url: String,
    val branch: String,
    val commit: String?,
) {
    fun serialize(): String {
        return listOf(name, url, branch, commit).joinToString(SEPARATOR) { value ->
            value?.replace("%", "%25")?.replace(SEPARATOR, "%1F") ?: NULL_VALUE
        }
    }

    companion object {
        private const val NULL_VALUE = "%00"
        private const val SEPARATOR = "\u001F"

        fun deserialize(value: String): GitReference {
            val fields = value.split(SEPARATOR)
            require(fields.size == 4) { "Invalid serialized Git reference." }
            return GitReference(
                name = decode(fields[0]),
                url = requireNotNull(decode(fields[1])),
                branch = requireNotNull(decode(fields[2])),
                commit = decode(fields[3]),
            )
        }

        private fun decode(value: String): String? {
            if (value == NULL_VALUE) {
                return null
            }
            return value.replace("%1F", SEPARATOR).replace("%25", "%")
        }
    }
}
