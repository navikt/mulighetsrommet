package no.nav.mulighetsrommet.ktor.clients

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.MDC

fun httpJsonClient(engine: HttpClientEngine = CIO.create()) = HttpClient(engine) {
    expectSuccess = false

    install(Logging) {
        level = LogLevel.INFO
    }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }

    defaultRequest {
        System.getenv("NAIS_APP_NAME")?.let {
            header("Nav-Consumer-Id", it)
        }

        MDC.get("call-id")?.let {
            header(HttpHeaders.XRequestId, it)
        }
    }
}
