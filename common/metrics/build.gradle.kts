plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Metrikker
    api(libs.micrometer.registry.prometheus)
}
