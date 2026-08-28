plugins {
  alias(libs.plugins.fabric.loom)
  id("io.github.trethore.fabric-package")
}

grapheneTarget {
  javaVersion.set(25)
  fabricApiVersion.set(libs.versions.fabric.api.v262)
}

dependencies {
  minecraft(libs.minecraft.v262)
  implementation(libs.fabric.loader)
  implementation(libs.fabric.api.v262)
}
