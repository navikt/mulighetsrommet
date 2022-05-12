group = "no.nav.mulighetsrommet"
version = "0.0.1"

plugins {
    val kotlinVersion = "1.6.21"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.flywaydb.flyway") version "8.5.10" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
    id("com.adarshr.test-logger") version "3.2.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
