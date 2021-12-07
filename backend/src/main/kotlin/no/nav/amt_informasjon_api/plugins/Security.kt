package no.nav.amt_informasjon_api.plugins

import io.ktor.sessions.*
import io.ktor.application.*

fun Application.configureSecurity() {
    install(Sessions) {
    }
}
