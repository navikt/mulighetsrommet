plugins {
    application
    jacoco
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("no.nav.mulighetsrommet.arena_ords_proxy.ApplicationKt")
}

tasks.withType<Test> {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

repositories {
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
}

dependencies {
    implementation(project(":common:ktor"))

    val ktorVersion = "2.0.2"
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    val kotestVersion = "5.3.0"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-wiremock:1.0.3")
    testImplementation("io.mockk:mockk:1.12.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("io.micrometer:micrometer-registry-prometheus:1.8.3")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    implementation("org.slf4j:slf4j-api:1.7.36")
}
