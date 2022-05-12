plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.flywaydb.flyway")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.johnrengelman.shadow")
    id("com.adarshr.test-logger")
}

application {
    mainClass.set("no.nav.mulighetsrommet.api.ApplicationKt")
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

    val ktorVersion = "2.0.1"
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-webjars:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-conditional-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    val hopliteVersion = "1.4.16"
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")

    val koinVersion = "3.2.0"
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    val kotestVersion = "5.3.0"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.flywaydb:flyway-core:8.5.5")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("com.github.seratch:kotliquery:1.6.2")
    implementation("no.nav.security:mock-oauth2-server:0.4.3")
    runtimeOnly("org.webjars:swagger-ui:4.1.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.10")
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation("no.nav.security:mock-oauth2-server:0.4.6")
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

// java.sourceCompatibility = JavaVersion.VERSION_1_8

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

flyway {
    url = System.getenv("DB_URL")
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
}
