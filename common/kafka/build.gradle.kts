plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.common.database)
    testImplementation(testFixtures(projects.common.database))
    implementation(projects.common.metrics)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    api(libs.nav.common.kafka)
    implementation(libs.shedlock.jdbc)

    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.kotest.extensions.testcontainers)
    testImplementation(libs.kotest.extensions.testcontainers.kafka)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj.db)
}
