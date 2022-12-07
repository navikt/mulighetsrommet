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
    val ktorVersion = "2.1.3"
    testFixturesImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testFixturesImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testFixturesImplementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
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
