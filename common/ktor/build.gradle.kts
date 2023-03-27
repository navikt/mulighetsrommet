plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-test-fixtures`
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Needed to use the `@OptIn` annotation for experimental features
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {
    val ktorVersion = "2.2.4"
    testFixturesImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testFixturesImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testFixturesImplementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    // Metrikker
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.4")

    // Audit-logging
    implementation("com.github.navikt.common-java-modules:audit-log:3.2023.03.22_12.48-00fcbdc8f455")
    constraints {
        val logbackVerison = "1.4.6"
        implementation("ch.qos.logback:logback-core:$logbackVerison") {
            because("logback-syslog4j drar med seg en eldre versjon med sikkerhetshull")
        }
        implementation("ch.qos.logback:logback-classic:$logbackVerison") {
            because("logback-syslog4j drar med seg en eldre versjon med sikkerhetshull")
        }
    }

    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.5")

    val hopliteVersion = "2.7.2"
    api("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    api("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
}
