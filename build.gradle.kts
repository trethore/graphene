import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.jetbrains.qodana.tasks.QodanaScanTask

plugins {
  alias(libs.plugins.qodana)
  alias(libs.plugins.spotless)
  id("io.github.trethore.sonar")
}

val modVersion = providers.gradleProperty("mod_version").get()

spotless {
  java {
    target("**/src/**/*.java")
    targetExclude("references/**", "**/build/**")
    importOrder()
    removeUnusedImports()
    palantirJavaFormat(libs.versions.palantir.java.format.get())
    trimTrailingWhitespace()
    endWithNewline()
    toggleOffOn()
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
    biome(libs.versions.biome.get())
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
    leadingTabsToSpaces()
    endWithNewline()
  }
}

allprojects {
  version = modVersion
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

tasks.named<QodanaScanTask>("qodanaScan") {
  arguments.addAll(
      "--config",
      "config/qodana/qodana.yaml",
      "--env",
      "JAVA_TOOL_OPTIONS=-Dorg.gradle.projectcachedir=/data/cache/gradle/project-cache",
      "--print-problems",
      "--disable-update-checks",
  )
}

subprojects {
  plugins.withType<JavaPlugin> {
    dependencies {
      "testImplementation"(libs.junit.jupiter)
      "testRuntimeOnly"(libs.junit.platform.launcher)
    }

    tasks.withType<Test>().configureEach {
      useJUnitPlatform()
    }
  }
}
