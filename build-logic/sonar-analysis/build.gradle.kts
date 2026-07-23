plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

dependencies {
  implementation(
      "org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:${providers.gradleProperty("sonar_gradle_plugin_version").get()}"
  )
  testImplementation(kotlin("test-junit5"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}

gradlePlugin {
  plugins {
    register("sonarConventions") {
      id = "example.sonar"
      implementationClass = "io.github.trethore.buildlogic.sonar.SonarConventionsPlugin"
    }
  }
}
