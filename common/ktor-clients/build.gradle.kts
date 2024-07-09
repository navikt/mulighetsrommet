plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.common.metrics)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.core)
    api(libs.ktor.client.contentNegotiation)
    api(libs.ktor.client.logging)
    api(libs.ktor.serialization.json)
}
