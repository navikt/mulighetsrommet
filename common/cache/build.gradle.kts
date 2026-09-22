plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.caffeine)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotest.junit)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
}
