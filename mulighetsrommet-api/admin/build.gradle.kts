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
    testFixturesImplementation(projects.mulighetsrommetApi.domain)
    testImplementation(testFixtures(projects.mulighetsrommetApi.domain))
    testFixturesImplementation(testFixtures(projects.mulighetsrommetApi.domain))
    implementation(projects.common.domain)
    testFixturesImplementation(projects.common.domain)
    implementation(projects.common.validation)
    implementation(projects.common.spreadsheet)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)
    implementation(libs.caffeine)
    implementation(libs.slf4j)

    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.mockk)
    testFixturesImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
