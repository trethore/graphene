package io.github.trethore.buildlogic.sonar

internal const val SONAR_NOT_AVAILABLE = "not available"

internal fun formatSonarPercentage(value: String?): String = value?.let { "$it%" } ?: SONAR_NOT_AVAILABLE
