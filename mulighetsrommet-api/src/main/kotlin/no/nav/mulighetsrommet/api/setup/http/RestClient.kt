package no.nav.mulighetsrommet.api.setup.http

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.serialization.kotlinx.json.*

internal val defaultHttpClient = HttpClient(CIO) {
    expectSuccess = false
    install(ContentNegotiation) {
        jackson {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
        }
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
