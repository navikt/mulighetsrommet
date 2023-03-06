import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.mulighetsrommet"
version = "0.0.1"

plugins {
    val kotlinVersion = "1.8.0"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("org.flywaydb.flyway") version "9.15.1" apply false
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

allprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        testLogging {
            showCauses = true
            showExceptions = true
            exceptionFormat = TestExceptionFormat.SHORT
            events(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }

    repositories {
        mavenCentral()
    }
}
