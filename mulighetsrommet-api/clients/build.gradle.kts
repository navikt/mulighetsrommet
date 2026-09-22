plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

base {
    archivesName = "mulighetsrommet-api-clients"
}

dependencies {
    implementation(projects.mulighetsrommetApi.domain)

    implementation(projects.common.domain)
    implementation(projects.common.logging)
    implementation(projects.common.cache)
    implementation(projects.common.serialization)
    implementation(projects.common.ktorClients)
    implementation(projects.common.tokenProvider)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)
    implementation(libs.caffeine)
    implementation(libs.slf4j)

    testImplementation(testFixtures(projects.common.ktor))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
}

tasks.test {
    useJUnitPlatform()
}
