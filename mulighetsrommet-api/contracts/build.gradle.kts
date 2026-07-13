plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

base {
    archivesName = "mulighetsrommet-api-contracts"
}

dependencies {
    implementation(projects.mulighetsrommetApi.domain)
    implementation(projects.common.domain)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
}
