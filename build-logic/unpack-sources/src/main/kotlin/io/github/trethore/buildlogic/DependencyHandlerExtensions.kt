package io.github.trethore.buildlogic

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

@Suppress("unused")
fun DependencyHandler.unpack(dependency: Dependency?): Dependency {
    return add("unpack", requireNotNull(dependency) { "unpack dependency must not be null" }) as Dependency
}
