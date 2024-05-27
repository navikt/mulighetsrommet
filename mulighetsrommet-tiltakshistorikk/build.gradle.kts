plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("no.nav.mulighetsrommet.tiltakshistorikk.ApplicationKt")
}

tasks.shadowJar {
    // Trengs for å få med implementasjonen av services fra bl.a. flyway
    mergeServiceFiles()
}

dependencies {
    implementation(projects.common.domain)
    implementation(projects.common.database)
    implementation(projects.common.slack)
    implementation(projects.common.kafka)
    testImplementation(testFixtures(projects.common.database))
    implementation(projects.common.ktor)
    testImplementation(testFixtures(projects.common.ktor))
    implementation(projects.common.metrics)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.arrow.core.serialization)

    // Logging
    implementation(libs.bundles.logging)

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
    testImplementation(libs.ktor.server.tests)

    implementation(libs.nav.common.tokenClient)
    constraints {
        implementation("net.minidev:json-smart:2.5.1") {
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
}
