
plugins {
    val kotlinVersion = "1.6.10"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.flywaydb.flyway") version "8.5.5" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1" apply false
}

group = "no.nav.mulighetsrommet"
version = "0.0.1"

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
    }
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
        // Needed to get no.nav.common-java-modules to work. Deps from other repos
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }

    val implementation by configurations
    val testImplementation by configurations
    dependencies {
        val kotestVersion = "5.1.0"
        implementation("org.flywaydb:flyway-core:8.5.5")
        implementation("com.zaxxer:HikariCP:5.0.1")
        implementation("org.postgresql:postgresql:42.3.3")
        testImplementation("io.mockk:mockk:1.12.3")
        testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
        testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    }
}
