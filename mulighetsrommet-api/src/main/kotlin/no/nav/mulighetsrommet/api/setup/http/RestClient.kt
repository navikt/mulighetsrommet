package no.nav.mulighetsrommet.api.setup.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.MDC

internal val baseClient = HttpClient(CIO) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }
    defaultRequest {
        header("Nav-Consumer-Id", "mulighetsrommet-api")
        MDC.get("call-id")?.let { header(HttpHeaders.XRequestId, it) }
    }
}
