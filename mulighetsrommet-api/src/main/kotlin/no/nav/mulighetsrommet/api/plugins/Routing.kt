package no.nav.mulighetsrommet.api.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.autohead.AutoHeadResponse

fun Application.configureRouting() {
    install(AutoHeadResponse)
}
