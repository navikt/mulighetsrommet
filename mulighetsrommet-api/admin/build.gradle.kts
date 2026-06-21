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
    implementation(projects.common.domain)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)

    testFixturesImplementation(projects.mulighetsrommetApi.domain)
    testFixturesImplementation(libs.mockk)

    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
