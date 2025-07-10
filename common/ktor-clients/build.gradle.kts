plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.common.metrics)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.core)
    api(libs.ktor.client.contentNegotiation)
    api(libs.ktor.client.logging)
    testImplementation(libs.ktor.client.mock)
    api(libs.ktor.serialization.json)

    // Test
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
}
