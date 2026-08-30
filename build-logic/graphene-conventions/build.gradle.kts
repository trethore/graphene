plugins {
  `kotlin-dsl`
}

repositories {
  maven {
    name = "Fabric"
    url = uri("https://maven.fabricmc.net/")
  }
}

dependencies {
  implementation(project(":architecture-check"))
  implementation(libs.fabric.loom.gradle.plugin)
}

gradlePlugin {
  plugins {
    register("fabricPackageConventions") {
      id = "io.github.trethore.fabric-package"
      implementationClass = "io.github.trethore.buildlogic.graphene.FabricPackageConventionsPlugin"
    }
    register("mavenPublishingConventions") {
      id = "io.github.trethore.maven-publishing"
      implementationClass =
          "io.github.trethore.buildlogic.graphene.MavenPublishingConventionsPlugin"
    }
    register("debugClientConventions") {
      id = "io.github.trethore.debug-client"
      implementationClass = "io.github.trethore.buildlogic.graphene.DebugClientConventionsPlugin"
    }
  }
}
