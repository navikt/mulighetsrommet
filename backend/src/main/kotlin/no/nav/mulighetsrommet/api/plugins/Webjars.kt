package no.nav.mulighetsrommet.api.plugins

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.webjars.Webjars

fun Application.configureWebjars() {
    install(Webjars) {
        path = "assets"
    }
}
