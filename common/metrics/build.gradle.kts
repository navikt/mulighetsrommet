plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.micrometer.core)
    api(libs.micrometer.registry.prometheus)
}
