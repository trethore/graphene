import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test

plugins {
  `maven-publish`
  id("com.diffplug.spotless") version "8.8.0"
  id("example.unpack-sources")
  id("example.sonar")
}

spotless {
  java {
    target("**/src/**/*.java")
    targetExclude("references/**", "**/build/**")
    googleJavaFormat()
    removeUnusedImports()
    formatAnnotations()
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    targetExclude("references/**", "**/build/**", ".gradle/**")
    ktfmt()
  }

  format("misc") {
    target("**/*.md", ".gitignore")
    targetExclude("references/**", "**/build/**", ".gradle/**")
    trimTrailingWhitespace()
    endWithNewline()
  }
}

allprojects {
  version = providers.gradleProperty("mod_version").get()
  group = providers.gradleProperty("maven_group").get()

  repositories {
    mavenCentral()
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net/")
    }
    maven {
      name = "Mojang"
      url = uri("https://libraries.minecraft.net/")
    }
  }
}

subprojects {
  plugins.withType<JavaPlugin> {
    dependencies {
      "testImplementation"(
          "org.junit.jupiter:junit-jupiter:${providers.gradleProperty("junit_version").get()}"
      )
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
    }
  }
}

references {
  unpackNestedJars = true

  git(
      url = "git@github.com:trethore/graphene.git",
      branch = "main",
  )
  git(
      url = "https://github.com/trethore/jcef.git",
      branch = "master",
  )
  git(
      url = "https://github.com/chromiumembedded/cef.git",
      branch = "master",
  )

  // Optional Git references can be added like this:
  // git(
  //     url = "https://github.com/FabricMC/fabric.git",
  //     branch = "main",
  //     commit = null,
  // )
}
