package no.nav.tiltak.okonomi.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            JsonIgnoreUnknownKeys,
        )
    }
}
