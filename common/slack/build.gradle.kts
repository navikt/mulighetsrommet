@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

dependencies {
    // Slack-SDK
    implementation(libs.slack.client)
}
