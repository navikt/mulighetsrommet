plugins {
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.common.domain)

    // Kotlin
    implementation(libs.arrow.core)
}
