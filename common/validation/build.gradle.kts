plugins {
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(projects.common.domain)

    // Kotlin
    implementation(libs.arrow.core)

    // Test
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.table)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.assertj.db)
}
