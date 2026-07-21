import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
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

  format("javascript") {
    target(
        "debug-client/shared/**/*.js",
        "packages/common/src/main/resources/**/*.js",
    )
    biome("2.1.0")
    trimTrailingWhitespace()
    endWithNewline()
  }

  format("html") {
    target(
        "debug-client/shared/**/*.html",
        "packages/common/src/main/resources/**/*.html",
    )
    eclipseWtp(EclipseWtpFormatterStep.HTML)
    trimTrailingWhitespace()
    endWithNewline()
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
      "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
    }
  }
}

references {
  unpackNestedJars = true

  git(
      url = "https://github.com/trethore/jcef.git",
      branch = "master",
      commit = "5855f3c197e3134cbf1499a60b2ac90129d6e502",
  )
  git(
      url = "https://github.com/chromiumembedded/cef.git",
      branch = "master",
      commit = "82195616d8405e6081a0d90924707b82aa9e4141",
  )
  git(
      url = "https://chromium.googlesource.com/chromium/src.git",
      branch = "146.0.7680.179",
      commit = "347d8fd10aba5b885fb19ba5ea809b39b94afd0b",
      sparsePaths =
          listOf(
              "chrome/browser/file_select_helper.cc",
              "chrome/browser/file_select_helper.h",
          ),
  )

  // Optional Git references can be added like this:
  // git(
  //     url = "https://github.com/FabricMC/fabric.git",
  //     branch = "main",
  //     commit = null,
  // )
}
