plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

dependencies {
    // Slack-SDK
    val slackVersion = "1.27.3"
    implementation("com.slack.api:slack-api-client:$slackVersion")
}
