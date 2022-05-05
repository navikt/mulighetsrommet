package no.nav.mulighetsrommet.arena_ords_proxy.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*


fun Application.configureErrorHandling() {
    install(StatusPages) {

    }
}
