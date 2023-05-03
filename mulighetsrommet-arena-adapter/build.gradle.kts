plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.flyway)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("no.nav.mulighetsrommet.arena.adapter.ApplicationKt")
}

flyway {
    url = System.getenv("DB_URL")
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
    cleanDisabled = false
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Needed to use the `@OptIn` annotation for experimental features
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {
    implementation(projects.common.domain)
    implementation(projects.common.database)
    testImplementation(testFixtures(projects.common.database))
    implementation(projects.common.kafka)
    implementation(projects.common.ktor)
    testImplementation(testFixtures(projects.common.ktor))
    implementation(projects.common.ktorClients)
    implementation(projects.common.slack)
    implementation(projects.common.metrics)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)

    // Ktor
    testImplementation(libs.ktor.client.mock)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.authJwt)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.ktor.server.metricsMicrometer)
    implementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.tests)

    implementation(libs.nav.common.tokenClient)
    constraints {
        implementation("net.minidev:json-smart:2.4.10") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
    }

    // Test
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.db)
    testImplementation(libs.nav.mockOauth2Server)

    // Dependency injection
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.logback.logstashLogbackEncoder)
    implementation(libs.slf4j)

    implementation(libs.dbScheduler)
}
