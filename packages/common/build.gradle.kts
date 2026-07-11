import io.github.trethore.buildlogic.unpack

plugins {
  `java-library`
  `maven-publish`
}

dependencies {
  unpack(implementation("io.github.trethore:jcefgithub:146.0.10.3"))
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
