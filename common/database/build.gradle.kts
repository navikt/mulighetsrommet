plugins {
    kotlin("jvm")
    `java-test-fixtures`
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

dependencies {
    implementation("io.arrow-kt:arrow-core:1.1.2")

    // Database
    api("org.flywaydb:flyway-core:8.5.5")
    api("com.zaxxer:HikariCP:5.0.1")
    api("org.postgresql:postgresql:42.3.3")
    api("com.github.seratch:kotliquery:1.6.2")
    implementation("io.dropwizard.metrics:metrics-healthchecks:4.0.3")
    implementation("io.dropwizard.metrics:metrics-core:3.2.1")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.6.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.slf4j:slf4j-api:2.0.0")

    // Test
    val kotestVersion = "5.3.1"
    testFixturesImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testFixturesImplementation("org.assertj:assertj-db:2.0.2")
}
