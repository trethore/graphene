package io.github.trethore.buildlogic.graphene

import org.gradle.api.provider.Property

abstract class GrapheneTargetExtension {
  abstract val javaVersion: Property<Int>
  abstract val fabricApiVersion: Property<String>
}
