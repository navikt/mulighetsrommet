plugins {
    kotlin("jvm")
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
    implementation(projects.common.slack)
    testFixturesImplementation(projects.common.slack)

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.arrow-kt:arrow-core:1.1.5")

    // Database
    api("org.flywaydb:flyway-core:9.15.1")
    api("com.zaxxer:HikariCP:5.0.1")
    api("org.postgresql:postgresql:42.5.4")
    api("com.github.seratch:kotliquery:1.9.0")
    implementation("io.dropwizard.metrics:metrics-healthchecks:4.2.17")
    implementation("io.dropwizard.metrics:metrics-core:4.2.17")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.11.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.6")

    // Test
    testFixturesImplementation("io.kotest:kotest-runner-junit5:5.5.5")
    testFixturesImplementation("org.assertj:assertj-db:2.0.2")
}
