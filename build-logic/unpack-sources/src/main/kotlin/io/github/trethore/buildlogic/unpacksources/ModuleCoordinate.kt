package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.artifacts.ExternalModuleDependency

internal data class ModuleCoordinate(
    val group: String,
    val name: String,
    val version: String,
) {
    val label: String = "$group:$name:$version"

    companion object {
        fun from(dependency: ExternalModuleDependency): ModuleCoordinate {
            val version = dependency.version
            require(!version.isNullOrBlank()) {
                "unpack dependency ${dependency.group}:${dependency.name} must declare a concrete version."
            }
            require(isConcreteVersion(version)) {
                "unpack dependency ${dependency.group}:${dependency.name}:$version must use a concrete version."
            }
            return ModuleCoordinate(dependency.group, dependency.name, version)
        }

        fun parse(label: String): ModuleCoordinate {
            val segments = label.split(':', limit = 3)
            require(segments.size == 3) { "Invalid module coordinate: $label" }
            return ModuleCoordinate(segments[0], segments[1], segments[2])
        }

        private fun isConcreteVersion(version: String): Boolean {
            return version != "+" &&
                !version.endsWith(".+") &&
                !version.startsWith("latest.") &&
                !version.startsWith('[') &&
                !version.startsWith('(') &&
                !version.endsWith(']') &&
                !version.endsWith(')')
        }
    }
}
