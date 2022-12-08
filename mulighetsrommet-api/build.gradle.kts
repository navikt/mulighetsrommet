plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.flywaydb.flyway")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("no.nav.mulighetsrommet.api.ApplicationKt")
}

ktlint {
    disabledRules.addAll("no-wildcard-imports")
}

flyway {
    url = System.getenv("DB_URL")
    user = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
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
    implementation(project(":common:domain"))
    implementation(project(":common:ktor"))
    implementation(project(":common:database"))
    testImplementation(testFixtures(project(":common:database")))

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.16")
    implementation("io.arrow-kt:arrow-core:1.1.2")

    val ktorVersion = "2.2.1"
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    implementation("io.ktor:ktor-server-caching-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-conditional-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    implementation("io.ktor:ktor-server-webjars:$ktorVersion")

    // Cache
    val caffeineVersion = "3.1.2"
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    implementation("io.prometheus:simpleclient_caffeine:0.16.0")

    implementation("io.micrometer:micrometer-registry-prometheus:1.10.2")

    val koinVersion = "3.2.0"
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    val navCommonModules = "2.2022.11.16_15.18-421ec713e2a0"
    implementation("no.nav.common:token-client:$navCommonModules")
    implementation("no.nav.common:audit-log:$navCommonModules")
    implementation("no.nav.common:kafka:$navCommonModules")

    // Tilgangskontroll
    val poaoTilgangClient = "2022.11.23_08.13-5d6b41f94eb1"
    implementation("com.github.navikt.poao-tilgang:client:$poaoTilgangClient")

    // Test
    val kotestVersion = "5.4.2"
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.3.0")
    testImplementation("org.assertj:assertj-db:2.0.2")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.10")
    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("no.nav.security:mock-oauth2-server:0.5.6")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("net.logstash.logback:logstash-logback-encoder:7.2")
    implementation("org.slf4j:slf4j-api:2.0.5")

    // OpenAPI
    // PS: Hvis man oppdaterer denne må man også rename mappen til riktig versjon i resources
    runtimeOnly("org.webjars:swagger-ui:4.14.0")
}
