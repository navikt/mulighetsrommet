plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

repositories {
    // Needed to get no.nav.common-java-modules to work. Deps from other repos
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":common:database"))
    testImplementation(testFixtures(project(":common:database")))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    val navCommonModules = "2.2023.01.02_13.51-1c6adeb1653b"
    implementation("no.nav.common:kafka:$navCommonModules")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:4.41.0")

    val kotestVersion = "5.4.2"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.3.0")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:1.3.4")
    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("org.testcontainers:kafka:1.17.6")
    testImplementation("org.assertj:assertj-db:2.0.2")
}
