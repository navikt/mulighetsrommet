plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("no.nav.mulighetsrommet.arena.adapter.ApplicationKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Needed to use the `@OptIn` annotation for experimental features
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.shadowJar {
    // Trengs for å få med implementasjonen av services fra bl.a. flyway
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

dependencies {
    implementation(projects.common.nais)
    implementation(projects.common.tokenProvider)
    implementation(projects.common.domain)
    implementation(projects.common.database)
    testImplementation(testFixtures(projects.common.database))
    implementation(projects.common.kafka)
    implementation(projects.common.ktor)
    testImplementation(testFixtures(projects.common.ktor))
    implementation(projects.common.ktorClients)
    implementation(projects.common.slack)
    implementation(projects.common.metrics)
    implementation(projects.common.tasks)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)

    // Logging
    implementation(libs.bundles.logging)

    // Ktor
    implementation(libs.ktor.client.mock)
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
    testImplementation(libs.ktor.server.testHost)

    // Test
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.kotest.assertions.table)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.db)
    testImplementation(libs.nav.mockOauth2Server)

    // Dependency injection
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
}
