plugins {
  alias(libs.plugins.fabric.loom)
  id("io.github.trethore.debug-client")
}

grapheneTarget {
  javaVersion.set(25)
  fabricApiVersion.set(libs.versions.fabric.api.v2612)
}

dependencies {
  minecraft(libs.minecraft.v2612)
  implementation(libs.fabric.loader)
  implementation(libs.fabric.api.v2612)
}
