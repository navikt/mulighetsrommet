package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.*
import io.ktor.server.webjars.*


fun Application.configureWebjars() {
    install(Webjars) {
        path = "assets"
    }
}
