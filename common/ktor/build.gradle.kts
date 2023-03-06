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
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.3")

    // Audit-logging
    implementation("com.github.navikt.common-java-modules:audit-log:2023.02.08_14.02-e5f1a7a4b9e4")

    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.5")

    val hopliteVersion = "2.4.0"
    api("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    api("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
}
