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
    implementation(projects.common.database)
    testImplementation(testFixtures(projects.common.database))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    api(libs.nav.common.kafka)
    implementation(libs.shedlock.jdbc)

    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.kotest.extensions.testcontainers)
    testImplementation(libs.mockk)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.assertj.db)
}
