plugins {
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Needed to use the `@OptIn` annotation for experimental features
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {
    implementation(projects.common.metrics)
    testFixturesImplementation(libs.ktor.client.core)
    testFixturesImplementation(libs.ktor.client.mock)
    testFixturesImplementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.callId)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.metricsMicrometer)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.authJwt)

    // Metrikker
    implementation(libs.micrometer.registry.prometheus)

    // Audit-logging
    implementation(libs.nav.common.auditLog)
    constraints {
        implementation(libs.logback.core) {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
        implementation(libs.logback.classic) {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
    }

    // Cache
    implementation(libs.caffeine)

    // Config
    api(libs.hoplite.core)
    api(libs.hoplite.yaml)
}
