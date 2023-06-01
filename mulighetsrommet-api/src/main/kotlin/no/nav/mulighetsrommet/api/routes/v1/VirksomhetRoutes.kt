package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.clients.brreg.OrgnummerUtil
import no.nav.mulighetsrommet.api.services.VirksomhetService
import no.nav.mulighetsrommet.api.utils.getVirksomhetFilter
import org.koin.ktor.ext.inject

fun Route.virksomhetRoutes() {
    val virksomhetService: VirksomhetService by inject()

    route("api/v1/internal/virksomhet") {
        get {
            val filter = getVirksomhetFilter()
            call.respond(virksomhetService.getAll(filter))
        }

        get("{orgnr}") {
            val orgnr = call.parameters.getOrFail("orgnr")

            if (!OrgnummerUtil.erOrgnr(orgnr)) {
                throw BadRequestException("Verdi sendt inn er ikke et organisasjonsnummer. Organisasjonsnummer er 9 siffer og bare tall.")
            }

            val enhet = virksomhetService.hentEnhet(orgnr)
            if (enhet == null) {
                call.respond(HttpStatusCode.NotFound, "Fant ikke enhet med orgnr: $orgnr")
            } else {
                call.respond(enhet)
            }
        }

        get("sok/{sok}") {
            val sokestreng = call.parameters.getOrFail("sok")

            if (sokestreng.isBlank()) {
                throw BadRequestException("'sok' kan ikke være en tom streng")
            }

            call.respond(virksomhetService.sokEtterEnhet(sokestreng))
        }
    }
}
