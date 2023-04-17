@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.flyway)
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("no.nav.mulighetsrommet.api.ApplicationKt")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

flyway {
    url = System.getenv("DB_URL")
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
}

dependencies {
    implementation(projects.common.domain)
    implementation(projects.common.ktor)
    implementation(projects.common.ktorClients)
    implementation(projects.common.database)
    implementation(projects.common.slack)
    implementation(projects.common.kafka)
    testImplementation(testFixtures(projects.common.database))

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)

    // Ktor
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.logging)
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
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.swagger)
    testImplementation(libs.ktor.server.testHost)

    // Cache
    implementation(libs.caffeine)

    // Metrics
    implementation(libs.prometheus.caffeine)
    implementation(libs.micrometer.registry.prometheus)

    implementation(libs.nav.common.auditLog)
    implementation(libs.nav.common.tokenClient)

    // Dependency injection
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    implementation(libs.nav.common.auditLog)
    constraints {
        implementation(libs.logback.core) {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
        implementation(libs.logback.classic) {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
    }
    implementation(libs.nav.common.tokenClient)
    constraints {
        implementation("net.minidev:json-smart:2.4.9") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
    }

    // Tilgangskontroll
    implementation(libs.nav.poaoTilgang.client)
    constraints {
        implementation("org.yaml:snakeyaml:2.0") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
        implementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.73") {
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

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.logback.logstashLogbackEncoder)
    implementation(libs.slf4j)

    // DB-scheduler
    implementation(libs.dbScheduler)
}
