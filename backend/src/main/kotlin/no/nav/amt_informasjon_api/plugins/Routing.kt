package no.nav.amt_informasjon_api.plugins

import io.ktor.features.*
import io.ktor.application.*

fun Application.configureRouting() {
    install(AutoHeadResponse)
}
