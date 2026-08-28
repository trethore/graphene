plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(libs.sonarqube.gradle.plugin)
}

gradlePlugin {
  plugins {
    register("sonarConventions") {
      id = "io.github.trethore.sonar"
      implementationClass = "io.github.trethore.buildlogic.sonar.SonarConventionsPlugin"
    }
  }
}
