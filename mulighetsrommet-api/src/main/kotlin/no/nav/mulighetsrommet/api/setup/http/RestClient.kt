package no.nav.mulighetsrommet.api.setup.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

internal val defaultHttpClient = HttpClient(CIO) {
    expectSuccess = false
    install(ContentNegotiation) {
        json()
    }
}

fun baseClient(): HttpClient {
    return HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) {
            json()
        }
    }
}
