plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-test-fixtures`
}

base {
    archivesName = "mulighetsrommet-api-veilederflate"
}

dependencies {
    implementation(projects.mulighetsrommetApi.domain)
    implementation(projects.mulighetsrommetApi.admin)
    implementation(projects.mulighetsrommetApi.clients)

    implementation(projects.common.domain)
    implementation(projects.common.database)
    implementation(projects.common.ktor)
    implementation(projects.common.ktorClients)
    implementation(projects.common.tokenProvider)
    implementation(projects.common.tiltakshistorikkClient)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)
    implementation(libs.caffeine)
    implementation(libs.slf4j)

    testFixturesImplementation(projects.mulighetsrommetApi.admin)
    testFixturesImplementation(projects.mulighetsrommetApi.domain)
    testFixturesImplementation(testFixtures(projects.mulighetsrommetApi.domain))
    testFixturesImplementation(libs.mockk)

    testImplementation(testFixtures(projects.common.database))
    testImplementation(testFixtures(projects.common.ktor))
    testImplementation(testFixtures(projects.mulighetsrommetApi.domain))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
