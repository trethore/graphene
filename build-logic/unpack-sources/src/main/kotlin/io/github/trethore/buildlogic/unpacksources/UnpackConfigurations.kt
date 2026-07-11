package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

internal object UnpackConfigurations {
    fun createUnpackConfiguration(project: Project): Configuration {
        return project.configurations.create(UnpackSourcesConstants.UNPACK_CONFIGURATION_NAME) {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false
            description = "Libraries selected for unpackSources."
        }
    }

    fun createCfrConfiguration(project: Project): Configuration {
        val configuration = project.configurations.create(UnpackSourcesConstants.CFR_CONFIGURATION_NAME) {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false
            description = "Internal CFR dependency used by unpackSources when source jars are unavailable."
        }
        project.dependencies.add(
            UnpackSourcesConstants.CFR_CONFIGURATION_NAME,
            "org.benf:cfr:${UnpackSourcesConstants.CFR_VERSION}",
        )
        return configuration
    }
}
