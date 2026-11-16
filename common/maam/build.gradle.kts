plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.common.database)
    implementation(projects.common.databaseHelpers)
    implementation(projects.common.domain)
    implementation(projects.common.kafka)
    implementation(projects.common.nais)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.core)

    testImplementation(testFixtures(projects.common.database))
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.server.testHost)
    testImplementation(libs.ktor.server.contentNegotiation)
    testImplementation(libs.ktor.client.contentNegotiation)
    testImplementation(libs.ktor.serialization.json)
}
