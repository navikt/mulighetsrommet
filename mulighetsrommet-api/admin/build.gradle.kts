plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

base {
    archivesName = "mulighetsrommet-api-admin"
}

dependencies {
    implementation(projects.mulighetsrommetApi.domain)
    implementation(projects.common.domain)
    implementation(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnitPlatform()
}
