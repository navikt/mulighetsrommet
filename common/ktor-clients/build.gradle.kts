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
    api(libs.ktor.client.cio)
    api(libs.ktor.client.core)
    api(libs.ktor.client.contentNegotiation)
    api(libs.ktor.client.logging)
    api(libs.ktor.serialization.json)
}
