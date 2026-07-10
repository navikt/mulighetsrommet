plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-test-fixtures`
}

base {
    archivesName = "mulighetsrommet-api-domain"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

dependencies {
    implementation(projects.common.domain)
    implementation(libs.kotlinx.serialization.json)

    testFixturesImplementation(projects.common.domain)
}

tasks.test {
    useJUnitPlatform()
}
