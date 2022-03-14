package no.nav.mulighetsrommet.api

import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication

fun <R> withMulighetsrommetApp(test: TestApplicationEngine.() -> R): R {
    return withTestApplication({
        config()
        module()
    }) {
        test()
    }
}

fun Application.config() {
    (environment.config as MapApplicationConfig).apply {
        put("ktor.kafka.enable", "false")
    }
}
