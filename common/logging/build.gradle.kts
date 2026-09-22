plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.slf4j)

    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
}
