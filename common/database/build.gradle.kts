plugins {
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Needed to use the `@OptIn` annotation for experimental features
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(projects.common.slack)
    testFixturesImplementation(projects.common.slack)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)

    // Database
    api(libs.flyway.core)
    api(libs.flyway.database.postgresql)
    api(libs.hikaricp)
    api(libs.postgresql)
    api(libs.kotliquery)
    implementation(libs.metrics.core)
    implementation(libs.metrics.healthchecks)
    implementation(libs.google.cloud.postgresSocketFactory)

    // Logging
    implementation(libs.slf4j)

    // Test
    testFixturesImplementation(libs.kotest.junit)
    testFixturesImplementation(libs.assertj.db)
}
