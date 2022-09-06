package no.nav.mulighetsrommet.api.setup.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

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
    }
}
