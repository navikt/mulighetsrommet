plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("no.nav.mulighetsrommet.sanity.ApplicationKt")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Needed to use the `@OptIn` annotation for experimental features
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {
    implementation(project(":common:domain"))
    implementation(project(":common:database"))
    implementation(project(":common:ktor"))

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    val ktorVersion = "2.2.1"
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    implementation("org.slf4j:slf4j-api:2.0.5")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
}
