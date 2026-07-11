import io.github.trethore.buildlogic.unpack

plugins {
  `java-library`
  `maven-publish`
}

val jcefGithubVersion = "146.0.10.3"

dependencies {
  implementation("io.github.trethore:jcefgithub:${jcefGithubVersion}:all-relocated") {
    isTransitive = false
  }
  unpack(create("io.github.trethore:jcefgithub:${jcefGithubVersion}:source"))
}

tasks.withType<JavaCompile>().configureEach {
  options.release = 21
}

java {
  withSourcesJar()

  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      from(components["java"])
    }
  }
}
