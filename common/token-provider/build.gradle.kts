plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.nav.common.tokenClient)
    constraints {
        implementation("net.minidev:json-smart:2.5.1") {
            because("sikkerhetshull i transitiv avhengighet rapportert via snyk")
        }
    }
}
