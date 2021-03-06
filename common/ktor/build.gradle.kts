plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

dependencies {
    val ktorVersion = "2.0.1"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    val hopliteVersion = "2.4.0"
    api("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    api("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")

    // Sentry error logging
    implementation("io.sentry:sentry:6.1.0")
}
