plugins {
    kotlin("jvm")
    `java-test-fixtures`
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

dependencies {
    implementation("io.arrow-kt:arrow-core:1.1.3")

    // Database
    api("org.flywaydb:flyway-core:9.8.3")
    api("com.zaxxer:HikariCP:5.0.1")
    api("org.postgresql:postgresql:42.5.1")
    api("com.github.seratch:kotliquery:1.9.0")
    implementation("io.dropwizard.metrics:metrics-healthchecks:4.2.13")
    implementation("io.dropwizard.metrics:metrics-core:4.2.13")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.7.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation(project(":common:slack"))

    // Test
    val kotestVersion = "5.3.1"
    testFixturesImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testFixturesImplementation("org.assertj:assertj-db:2.0.2")

    testFixturesImplementation(project(":common:slack"))
}
