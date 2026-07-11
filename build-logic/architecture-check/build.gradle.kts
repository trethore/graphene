plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

dependencies {
  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}

gradlePlugin {
  plugins {
    register("architectureCheck") {
      id = "io.github.trethore.architecture-check"
      implementationClass = "io.github.trethore.buildlogic.architecture.ArchitectureCheckPlugin"
    }
  }
}
