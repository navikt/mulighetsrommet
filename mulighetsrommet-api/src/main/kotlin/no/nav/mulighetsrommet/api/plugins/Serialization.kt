package no.nav.mulighetsrommet.api.plugins

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.serialization.json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}
