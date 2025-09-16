plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.common.domain)
    implementation(projects.common.ktorClients)
    testImplementation(testFixtures(projects.common.ktor))

    // Kotlin
    implementation(libs.arrow.core)
    implementation(libs.arrow.core.serialization)

    // Ktor
    api(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.mock)
    api(libs.ktor.serialization.json)

    // Test
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.table)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.assertj.db)
}
