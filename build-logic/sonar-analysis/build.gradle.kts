plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(libs.sonarqube.gradle.plugin)
  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
  useJUnitPlatform()
}

gradlePlugin {
  plugins {
    register("sonarConventions") {
      id = "io.github.trethore.sonar"
      implementationClass = "io.github.trethore.buildlogic.sonar.SonarConventionsPlugin"
    }
  }
}
