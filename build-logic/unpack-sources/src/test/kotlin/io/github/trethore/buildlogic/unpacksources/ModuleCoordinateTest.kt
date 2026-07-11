package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModuleCoordinateTest {
    @Test
    fun `accepts Fabric versions containing build metadata`() {
        val dependency = dependency("net.fabricmc.fabric-api:fabric-api:0.141.4+1.21.11")

        assertEquals(
            "net.fabricmc.fabric-api:fabric-api:0.141.4+1.21.11",
            ModuleCoordinate.from(dependency).label,
        )
    }

    @Test
    fun `rejects dynamic versions`() {
        listOf("+", "1.+", "latest.release", "[1.0,2.0)").forEach { version ->
            val dependency = dependency("org.example:library:$version")

            assertFailsWith<IllegalArgumentException> {
                ModuleCoordinate.from(dependency)
            }
        }
    }

    private fun dependency(notation: String): ExternalModuleDependency {
        val project = ProjectBuilder.builder().build()
        return project.dependencies.create(notation) as ExternalModuleDependency
    }
}
