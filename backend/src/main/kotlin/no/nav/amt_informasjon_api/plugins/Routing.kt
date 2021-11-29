package no.nav.amt_informasjon_api.plugins

import io.ktor.application.*
import io.ktor.features.*

fun Application.configureRouting() {
    install(AutoHeadResponse)
}
