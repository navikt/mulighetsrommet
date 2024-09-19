import org.openapitools.generator.gradle.plugin.tasks.ValidateTask

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.flyway)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("no.nav.mulighetsrommet.api.ApplicationKt")
}

val validateOpenapiSpec = tasks.register<ValidateTask>("validateOpenapiSpec") {
    inputSpec.set("$rootDir/mulighetsrommet-api/src/main/resources/web/openapi.yaml")
    recommend.set(true)
}

val validateOpenapiExternalSpec = tasks.register<ValidateTask>("validateOpenapiExternalSpec") {
    inputSpec.set("$rootDir/mulighetsrommet-api/src/main/resources/web/openapi-external.yaml")
    recommend.set(true)
}

val validateOpenapiSpecs = tasks.register<Task>("validateOpenapiSpecs") {
    dependsOn(validateOpenapiSpec.name, validateOpenapiExternalSpec.name)
}

tasks.build {
    dependsOn(validateOpenapiSpecs.name)
}

tasks.shadowJar {
    isZip64 = true
    // Trengs for å få med implementasjonen av services fra bl.a. flyway
    mergeServiceFiles()
}

flyway {
    url = System.getenv("DB_URL")
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
    cleanDisabled = false
}

dependencies {
    implementation(projects.common.database)
    testImplementation(testFixtures(projects.common.database))
    implementation(projects.common.domain)
    implementation(projects.common.kafka)
    implementation(projects.common.ktor)
    testImplementation(testFixtures(projects.common.ktor))
    implementation(projects.common.ktorClients)
    implementation(projects.common.tokenProvider)
    implementation(projects.common.metrics)
    implementation(projects.common.slack)
    implementation(projects.common.tasks)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.arrow.core.serialization)

    // Logging
    implementation(libs.bundles.logging)

    // Ktor
    testImplementation(libs.ktor.client.mock)
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
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.swagger)
    testImplementation(libs.ktor.server.testHost)

    // GCP
    implementation(libs.google.cloud.storage)

    // Cache
    implementation(libs.caffeine)

    // Metrics
    implementation(libs.prometheus.caffeine)

    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.tokenClient)
    implementation(libs.nav.common.client)

    // Dependency injection
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Excel
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Unleash
    implementation(libs.unleash)

    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.tokenClient)
    constraints {
        implementation("net.minidev:json-smart:2.5.1") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
    }

    // Tilgangskontroll
    implementation(libs.nav.poaoTilgang.client)
    constraints {
        implementation("org.yaml:snakeyaml:2.3") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
        implementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.95") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
    }

    // Test
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.assertj.db)
    testImplementation(libs.mockk)
    testImplementation(libs.nav.mockOauth2Server)

    // DB-scheduler
    implementation(libs.dbScheduler)
}
