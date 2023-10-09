import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

group = "no.nav.mulighetsrommet"
version = "0.0.1"

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.flyway) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.openapi.generator) apply false
}

allprojects {
    // Apply ktlint for all projects
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<KtlintExtension> {
        version.set("1.0.0")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    tasks.withType<JavaCompile> {
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        failFast = System.getenv("CI") == "true"

        useJUnitPlatform()

        // Run tests in parallel
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1

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
            // Needed for NAV-packages
            url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        }
    }
}
