package no.nav.mulighetsrommet.api.plugins

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse

fun Application.configureRouting() {
    install(AutoHeadResponse)
}
