package no.nav.amt_informasjon_api.plugins

import io.ktor.application.*
import io.ktor.sessions.*

fun Application.configureSecurity() {
    install(Sessions) {
    }
}
