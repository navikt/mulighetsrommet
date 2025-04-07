plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(kotlin("reflect"))

    implementation(libs.bundles.logging)

    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
}
