val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val prometeus_version: String by project
val kotest_version: String by project
val mockk_version: String by project
val ktlint_version: String by project
val hikari_version: String by project
val postgresql_version: String by project
val flyway_version: String by project
val hoplite_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.10"
    id("org.flywaydb.flyway") version "8.5.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.addAll("no-wildcard-imports")
}

group = "no.nav.mulighetsrommet.kafka"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

flyway {
    url = "jdbc:postgresql://localhost:5442/mulighetsrommet-kafka-db"
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktor_version")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometeus_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")
    implementation("org.postgresql:postgresql:$postgresql_version")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("com.sksamuel.hoplite:hoplite-core:$hoplite_version")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hoplite_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:$mockk_version")
    testImplementation("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation("io.kotest:kotest-assertions-core:$kotest_version")
}
