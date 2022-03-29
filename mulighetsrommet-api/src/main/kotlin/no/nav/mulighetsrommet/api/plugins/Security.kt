package no.nav.mulighetsrommet.api.plugins

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.sessions.Sessions

fun Application.configureSecurity() {
    install(Sessions) {
    }
}
