plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.apache.poi)
    api(libs.apache.poi.ooxml)

    implementation(projects.common.domain)

    // Test
    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.nav.mockOauth2Server)
}
