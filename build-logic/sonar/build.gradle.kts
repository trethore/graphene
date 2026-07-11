plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

dependencies {
  implementation(
      "org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:${providers.gradleProperty("sonar_gradle_plugin_version").get()}"
  )
}

gradlePlugin {
  plugins {
    register("sonarConventions") {
      id = "example.sonar"
      implementationClass = "io.github.trethore.buildlogic.sonar.SonarConventionsPlugin"
    }
  }
}
