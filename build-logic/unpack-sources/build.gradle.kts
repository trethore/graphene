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
    register("unpackSources") {
      id = "example.unpack-sources"
      implementationClass = "io.github.trethore.buildlogic.unpacksources.UnpackSourcesPlugin"
    }
  }
}
