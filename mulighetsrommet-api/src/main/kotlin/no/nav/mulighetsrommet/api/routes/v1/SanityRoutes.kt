package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.VeilederflateService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.api.utils.getTiltaksgjennomforingsFilter
import org.koin.ktor.ext.inject

fun Route.sanityRoutes() {
    val veilederflateService: VeilederflateService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/sanity") {
        get("/innsatsgrupper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())

            val innsatsgrupper = veilederflateService.hentInnsatsgrupper()

            call.respond(innsatsgrupper)
        }

        get("/tiltakstyper") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())

            val tiltakstyper = veilederflateService.hentTiltakstyper()

            call.respond(tiltakstyper)
        }

        get("/tiltaksgjennomforinger") {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent())
            val result = veilederflateService.hentTiltaksgjennomforingerForBrukerBasertPaEnhetOgFylke(
                getNorskIdent(),
                call.getAccessToken(),
                getTiltaksgjennomforingsFilter(),
            )
            call.respond(result)
        }

        get("/tiltaksgjennomforing/{id}") {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent())
            val id = call.parameters.getOrFail("id")
            val result = veilederflateService.hentTiltaksgjennomforingMedBrukerdata(
                id,
                getNorskIdent(),
                call.getAccessToken(),
            )

            call.respond(result)
        }

        get("/tiltaksgjennomforing/preview/{id}") {
            poaoTilgangService.verfiyAccessToModia(getNavAnsattAzureId())
            val id = call.parameters.getOrFail("id")
            val result = veilederflateService.hentPreviewTiltaksgjennomforing(
                id,
            )
            call.respond(result)
        }
    }
}
