plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-test-fixtures`
}

base {
    archivesName = "mulighetsrommet-api-persistence"
}

dependencies {
    implementation(projects.mulighetsrommetApi.admin)
    implementation(projects.mulighetsrommetApi.domain)
    implementation(projects.common.domain)
    implementation(projects.common.database)
    implementation(projects.common.databaseHelpers)
    implementation(projects.common.kafka)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(projects.common.kafka)

    testFixturesImplementation(testFixtures(projects.common.database))

    testImplementation(libs.arrow.core)
    testImplementation(testFixtures(projects.common.database))
    testImplementation(testFixtures(projects.mulighetsrommetApi.domain))
    testImplementation(projects.common.slack)
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.kotest.assertions.table)
    testImplementation(libs.assertj.db)
    testImplementation(libs.mockk)
    testImplementation(libs.nav.mockOauth2Server)
}

tasks.test {
    useJUnitPlatform()
}
