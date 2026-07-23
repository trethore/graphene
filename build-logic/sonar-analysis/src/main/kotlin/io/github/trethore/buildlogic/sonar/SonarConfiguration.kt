package io.github.trethore.buildlogic.sonar

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import java.io.File

internal data class SonarConfiguration(
    val properties: Map<String, Any>,
    val issueExclusions: List<SonarIssueExclusion>,
) {
    companion object {
        fun load(file: File): SonarConfiguration {
            if (!file.isFile) {
                throw GradleException("Sonar configuration was not found at $file.")
            }

            val root = try {
                JsonSlurper().parse(file) as? Map<*, *>
            } catch (exception: RuntimeException) {
                throw GradleException("Could not parse Sonar configuration at $file.", exception)
            } ?: throw GradleException("Sonar configuration at $file must be a JSON object.")

            validateKeys(root, setOf("properties", "issueExclusions"), "Sonar configuration")
            return SonarConfiguration(
                properties = readProperties(root["properties"]),
                issueExclusions = readIssueExclusions(root["issueExclusions"]),
            )
        }

        private fun readProperties(value: Any?): Map<String, Any> {
            if (value == null) {
                return emptyMap()
            }

            val properties = value as? Map<*, *>
                ?: throw GradleException("Sonar configuration 'properties' must be a JSON object.")
            return properties.entries.associate { (rawName, rawValue) ->
                val name = rawName as? String
                    ?: throw GradleException("Sonar configuration property names must be strings.")
                if (!name.startsWith("sonar.")) {
                    throw GradleException("Sonar configuration property '$name' must start with 'sonar.'.")
                }
                if (name.startsWith("sonar.issue.ignore.multicriteria")) {
                    throw GradleException(
                        "Configure '$name' through the Sonar configuration 'issueExclusions' array."
                    )
                }
                name to readPropertyValue(name, rawValue)
            }
        }

        private fun readPropertyValue(name: String, value: Any?): Any {
            return when (value) {
                is String, is Number, is Boolean -> value
                is List<*> -> value.map { element ->
                    when (element) {
                        is String, is Number, is Boolean -> element
                        else -> throw GradleException(
                            "Sonar configuration property '$name' contains an unsupported array value."
                        )
                    }
                }
                else -> throw GradleException(
                    "Sonar configuration property '$name' must be a string, number, boolean, or array."
                )
            }
        }

        private fun readIssueExclusions(value: Any?): List<SonarIssueExclusion> {
            if (value == null) {
                return emptyList()
            }

            val exclusions = value as? List<*>
                ?: throw GradleException("Sonar configuration 'issueExclusions' must be a JSON array.")
            return exclusions.mapIndexed { index, rawExclusion ->
                val exclusion = rawExclusion as? Map<*, *>
                    ?: throw GradleException(
                        "Sonar configuration issue exclusion ${index + 1} must be a JSON object."
                    )
                validateKeys(
                    exclusion,
                    setOf("ruleKey", "filePattern"),
                    "Sonar configuration issue exclusion ${index + 1}",
                )
                SonarIssueExclusion(
                    ruleKey = requiredString(exclusion, "ruleKey", index),
                    filePattern = requiredString(exclusion, "filePattern", index),
                )
            }
        }

        private fun requiredString(values: Map<*, *>, name: String, index: Int): String {
            return (values[name] as? String)?.takeIf(String::isNotBlank)
                ?: throw GradleException(
                    "Sonar configuration issue exclusion ${index + 1} requires a non-empty '$name'."
                )
        }

        private fun validateKeys(values: Map<*, *>, allowedKeys: Set<String>, description: String) {
            val unknownKeys = values.keys.filterIsInstance<String>().filterNot(allowedKeys::contains)
            if (unknownKeys.isNotEmpty()) {
                throw GradleException("$description contains unknown keys: ${unknownKeys.joinToString()}.")
            }
        }
    }
}

internal data class SonarIssueExclusion(
    val ruleKey: String,
    val filePattern: String,
)
