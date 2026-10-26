plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-test-fixtures`
}

base {
    archivesName = "mulighetsrommet-api-admin"
}

dependencies {
    implementation(projects.mulighetsrommetApi.domain)
    implementation(projects.mulighetsrommetApi.contracts)
    testFixturesImplementation(projects.mulighetsrommetApi.domain)
    testImplementation(testFixtures(projects.mulighetsrommetApi.domain))
    testFixturesImplementation(testFixtures(projects.mulighetsrommetApi.domain))
    implementation(projects.common.domain)
    testFixturesImplementation(projects.common.domain)
    implementation(projects.common.validation)
    implementation(projects.common.spreadsheet)
    implementation(projects.common.tokenProvider)
    implementation(projects.mulighetsrommetApi.clients)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)
    implementation(libs.caffeine)
    implementation(libs.slf4j)

    // Integrasjonstester mot ekte database bruker persistence og testoppsettet derfra
    testImplementation(projects.mulighetsrommetApi.persistence)
    testImplementation(testFixtures(projects.mulighetsrommetApi.persistence))
    testImplementation(projects.common.database)
    testImplementation(testFixtures(projects.common.database))
    testImplementation(projects.common.kafka)
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.mockk)
    testFixturesImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
