plugins {
  `kotlin-dsl`
}

gradlePlugin {
  plugins {
    register("architectureCheck") {
      id = "io.github.trethore.architecture-check"
      implementationClass = "io.github.trethore.buildlogic.architecture.ArchitectureCheckPlugin"
    }
  }
}
