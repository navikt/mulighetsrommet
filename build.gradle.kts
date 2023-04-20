import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.mulighetsrommet"
version = "0.0.1"

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.flyway) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    tasks.withType<JavaCompile> {
        targetCompatibility = "17"
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

        maven {
            // Needed for kafka
            url = uri("https://packages.confluent.io/maven/")
        }

        maven {
            // Needed for common-java-modules
            url = uri("https://jitpack.io")
        }
    }
}
