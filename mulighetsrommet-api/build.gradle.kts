/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.flywaydb.flyway")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.addAll("no-wildcard-imports")
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
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

dependencies {
    val ktorVersion = "1.6.2"
    val koinVersion = "3.1.5"
    val kotestVersion = "5.2.2"
    val hopliteVersion = "1.4.16"
    implementation(project(":mulighetsrommet-domain"))
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-webjars:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.jetbrains.exposed:exposed-java-time:0.34.2")
    implementation("org.jetbrains.exposed:exposed-core:0.34.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.34.2")
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("org.kodein.di:kodein-di-framework-ktor-server-jvm:7.6.0")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    implementation("org.flywaydb:flyway-core:8.5.5")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("com.github.seratch:kotliquery:1.6.2")
    runtimeOnly("org.webjars:swagger-ui:4.1.2")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.10")
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    shadowJar {

        manifest {
            attributes(Pair("Main-Class", "no.nav.mulighetsrommet.api.ApplicationKt"))
        }
    }
}

//java.sourceCompatibility = JavaVersion.VERSION_1_8

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

flyway {
    url = System.getenv("DB_URL")
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
}
