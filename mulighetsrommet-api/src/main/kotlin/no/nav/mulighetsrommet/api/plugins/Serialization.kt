package no.nav.mulighetsrommet.api.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            JsonIgnoreUnknownKeys,
        )
    }
}
