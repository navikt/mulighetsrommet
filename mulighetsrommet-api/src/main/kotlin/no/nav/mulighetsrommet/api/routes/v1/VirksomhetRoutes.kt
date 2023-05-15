package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.clients.brreg.OrgnummerUtil
import no.nav.mulighetsrommet.api.services.BrregService
import org.koin.ktor.ext.inject

fun Route.virksomhetRoutes() {
    val brregService: BrregService by inject()

    route("api/v1/internal/virksomhet") {
        get("{orgnr}") {
            val orgnr = call.parameters.getOrFail("orgnr")

            if (!OrgnummerUtil.erOrgnr(orgnr)) {
                throw BadRequestException("Verdi sendt inn er ikke et organisasjonsnummer. Organisasjonsnummer er 9 siffer og bare tall.")
            }

            call.respond(brregService.hentEnhet(orgnr))
        }

        get("sok/{sok}") {
            val sokestreng = call.parameters.getOrFail("sok")

            if (sokestreng.isBlank()) {
                throw BadRequestException("'sok' kan ikke være en tom streng")
            }

            call.respond(brregService.sokEtterEnhet(sokestreng))
        }
    }
}
