plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

dependencies {
    val ktorVersion = "2.1.0"
    implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // Metrikker
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.3")

    // Audit-logging
    val navCommonModules = "2.2022.11.10_08.37-7216bb5b1ede"
    implementation("no.nav.common:audit-log:$navCommonModules")

    // Cache
    val caffeineVersion = "3.1.2"
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    val hopliteVersion = "2.4.0"
    api("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    api("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
}
