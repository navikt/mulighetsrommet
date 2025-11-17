plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.common.domain)
    implementation(projects.common.tokenProvider)
    implementation(projects.common.ktorClients)

    // Kotlin
    implementation(libs.arrow.core)
    implementation(libs.arrow.core.serialization)
}
