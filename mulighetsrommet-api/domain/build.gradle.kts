plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

base {
    archivesName = "mulighetsrommet-api-domain"
}

dependencies {
    implementation(projects.common.domain)
}

tasks.test {
    useJUnitPlatform()
}
