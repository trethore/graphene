package io.github.trethore.buildlogic.sonar

object SonarConstants {
    const val ANALYSIS_TIMEOUT_SECONDS = 300L
    const val COVERAGE_TASK_NAME = "sonarCoverage"
    const val DEFAULT_HOST_URL = "http://localhost:9000"
    const val HOST_URL_ENV = "SONAR_HOST_URL"
    const val ISSUES_TASK_NAME = "sonarIssues"
    const val MAX_ISSUE_PAGES = 1_000
    const val PAGE_SIZE = 500
    const val PLUGIN_ID = "org.sonarqube"
    const val REQUEST_TIMEOUT_SECONDS = 30L
    const val STATUS_POLL_INTERVAL_MILLISECONDS = 1_000L
    const val SONAR_TASK_NAME = "sonar"
    const val TASK_GROUP = "verification"
    const val TOKEN_ENV = "SONAR_TOKEN"
}
