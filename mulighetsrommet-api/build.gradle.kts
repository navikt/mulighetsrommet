import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}

application {
    mainClass.set("no.nav.mulighetsrommet.api.ApplicationKt")
}

tasks.register<JavaExec>("generateOpenApi") {
    group = "documentation"
    description = "Generates all configured OpenAPI specs by running a dedicated generator."

    mainClass.set("no.nav.mulighetsrommet.api.GenerateOpenApiKt")
    classpath = sourceSets.main.get().runtimeClasspath

    // Specs to generate (name of spec -> file output path)
    args = listOf(
        "public",
        "src/main/resources/web/openapi-public.yaml",

        "tiltaksadministrasjon",
        "../frontend/tiltaksadministrasjon-api-client/openapi.yaml",

        "veilederflate",
        "../frontend/mulighetsrommet-veileder-flate/openapi.yaml",

        "arrangorflate",
        "../frontend/arrangor-flate/openapi.yaml",
    )
}

dependencies {
    implementation(projects.common.nais)
    implementation(projects.common.database)
    implementation(projects.common.databaseHelpers)
    testImplementation(testFixtures(projects.common.database))
    implementation(projects.common.domain)
    implementation(projects.common.brreg)
    implementation(projects.common.kafka)
    implementation(projects.common.ktor)
    testImplementation(testFixtures(projects.common.ktor))
    implementation(projects.common.ktorClients)
    implementation(projects.common.tokenProvider)
    implementation(projects.common.metrics)
    implementation(projects.common.slack)
    implementation(projects.common.tasks)
    implementation(projects.common.tiltaksokonomiClient)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.arrow.core.serialization)

    // Logging
    implementation(libs.bundles.logging)

    // Ktor
    implementation(libs.ktor.client.mock)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.authJwt)
    implementation(libs.ktor.server.autoHeadResponse)
    implementation(libs.ktor.server.cachingHeaders)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.conditionalHeaders)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.metricsMicrometer)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.sessions)
    testImplementation(libs.ktor.server.testHost)

    implementation(libs.bundles.ktor.openapi)

    // GCP
    implementation(libs.google.cloud.storage)

    // Cache
    implementation(libs.caffeine)

    // Metrics
    implementation(libs.prometheus.caffeine)

    implementation(libs.nav.common.auditLog)

    // Dependency injection
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Excel
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Unleash
    implementation(libs.unleash)

    // Tilgangskontroll
    implementation(libs.nav.poaoTilgang.client)
    constraints {
        implementation("org.yaml:snakeyaml:2.5") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
        implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.13") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
    }

    implementation(libs.shedlock.jdbc)

    // Test
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.kotest.assertions.table)
    testImplementation(libs.assertj.db)
    testImplementation(libs.mockk)
    testImplementation(libs.nav.mockOauth2Server)
}
