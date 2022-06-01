package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun Application.configureSecurity() {
    install(Sessions) {
    }
}
