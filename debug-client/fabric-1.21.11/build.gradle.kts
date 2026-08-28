plugins {
  alias(libs.plugins.fabric.loom.remap)
  id("io.github.trethore.debug-client")
}

grapheneTarget {
  javaVersion.set(21)
  fabricApiVersion.set(libs.versions.fabric.api.v12111)
}

dependencies {
  minecraft(libs.minecraft.v12111)
  mappings(loom.officialMojangMappings())
  modImplementation(libs.fabric.loader)
  modImplementation(libs.fabric.api.v12111)
}
