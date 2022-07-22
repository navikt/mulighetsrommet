
plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.flywaydb.flyway")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.johnrengelman.shadow")
    id("com.adarshr.test-logger")
    id("com.github.node-gradle.node") version "3.4.0"
}

application {
    mainClass.set("no.nav.mulighetsrommet.arena.adapter.ApplicationKt")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

repositories {
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
    // Needed to get no.nav.common-java-modules to work. Deps from other repos
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":mulighetsrommet-domain"))

    val ktorVersion = "2.0.3"
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    val hopliteVersion = "2.1.5"
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")

    val navCommonModules = "2.2022.05.05_06.41-84855089824b"
    implementation("no.nav.common:kafka:$navCommonModules")
    implementation("no.nav.common:token-client:$navCommonModules")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    implementation("org.slf4j:slf4j-api:1.7.36")

    implementation("com.github.seratch:kotliquery:1.8.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.flywaydb:flyway-core:8.5.5")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc:4.34.0")

    // Test
    val kotestVersion = "5.3.1"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("org.assertj:assertj-db:1.1.1")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.32.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.10")
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")

    // Health Check
    implementation("io.dropwizard.metrics:metrics-healthchecks:4.0.3")
    implementation("io.dropwizard.metrics:metrics-core:3.2.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

node {
    version.set("16.16.0")
    download.set(false)
    workDir.set(File("src/main/resources/web"))
    npmWorkDir.set(File("src/main/resources/web"))
    nodeProjectDir.set(File("src/main/resources/web"))
}

tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmBuild") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "build"))
    args.set(listOf("--", "--out-dir", "$buildDir/npm-output"))
    outputs.dir("$buildDir/npm-output")
}

flyway {
    url = System.getenv("DB_URL")
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
}
