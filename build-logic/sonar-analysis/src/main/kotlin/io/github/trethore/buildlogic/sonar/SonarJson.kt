package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException

internal data class SonarComponentIdentity(
    val key: String,
    val path: String,
)

internal fun Map<*, *>.requiredSonarObject(field: String, responseName: String): Map<*, *> {
    return this[field] as? Map<*, *>
        ?: throw GradleException("SonarQube $responseName response did not contain a valid '$field' object.")
}

internal fun Map<*, *>.requiredSonarArray(field: String, responseName: String): List<*> {
    return this[field] as? List<*>
        ?: throw GradleException("SonarQube $responseName response did not contain a valid '$field' array.")
}

internal fun Map<*, *>.requiredSonarString(field: String, responseName: String): String {
    return optionalSonarString(field, responseName)
        ?: throw GradleException("SonarQube $responseName response did not contain a valid '$field' value.")
}

internal fun Map<*, *>.optionalSonarString(field: String, responseName: String): String? {
    val value = this[field] ?: return null
    return (value as? String)?.takeIf(String::isNotBlank)
        ?: throw GradleException("SonarQube $responseName response contained an invalid '$field' value.")
}

internal fun Map<*, *>.optionalSonarInt(field: String, responseName: String): Int? {
    val value = this[field] ?: return null
    val parsedValue = when (value) {
        is Number, is String -> value.toString().toIntOrNull()
        else -> null
    }
    return parsedValue
        ?: throw GradleException("SonarQube $responseName response contained an invalid '$field' value: $value")
}

internal fun Map<*, *>.requiredSonarInt(field: String, responseName: String): Int {
    return optionalSonarInt(field, responseName)
        ?: throw GradleException("SonarQube $responseName response did not contain '$field'.")
}

internal fun requireNonNegativeSonarValue(value: Int, field: String, responseName: String): Int {
    if (value < 0) {
        throw GradleException("SonarQube $responseName response contained a negative '$field' value: $value")
    }
    return value
}

internal fun Map<String, String>.optionalSonarMetricInt(metric: String, responseName: String): Int? {
    val rawValue = this[metric] ?: return null
    val value = rawValue.toIntOrNull()
        ?: throw GradleException("SonarQube $responseName response contained an invalid '$metric' value: $rawValue")
    return requireNonNegativeSonarValue(value, metric, responseName)
}

internal fun Map<*, *>.sonarComponentIdentity(responseName: String): SonarComponentIdentity {
    val key = requiredSonarString("key", responseName)
    val path = optionalSonarString("path", responseName)
        ?: optionalSonarString("name", responseName)
        ?: key
    return SonarComponentIdentity(key, path)
}
