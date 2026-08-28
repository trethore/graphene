import org.gradle.api.tasks.testing.Test

subprojects {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

configure(listOf(project(":architecture-check"), project(":sonar-analysis"))) {
  pluginManager.withPlugin("org.gradle.kotlin.kotlin-dsl") {
    dependencies {
      "testImplementation"(kotlin("test-junit5"))
      "testRuntimeOnly"(libs.junit.platform.launcher)
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
    }
  }
}
