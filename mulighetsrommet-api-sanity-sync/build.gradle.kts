@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("no.nav.mulighetsrommet.sanity.ApplicationKt")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Needed to use the `@OptIn` annotation for experimental features
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {
    implementation(projects.common.domain)
    implementation(projects.common.database)
    implementation(projects.common.ktor)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.json)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.logback.logstashLogbackEncoder)
    implementation(libs.slf4j)
}
