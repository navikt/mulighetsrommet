package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.DelMedBrukerService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.domain.models.DelMedBruker
import no.nav.mulighetsrommet.ktor.extensions.getNonEmptyPathParameter
import no.nav.mulighetsrommet.ktor.extensions.getNonEmptyQueryParameter
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.koin.ktor.ext.inject

fun Route.delMedBrukerRoutes() {
    val delMedBrukerService by inject<DelMedBrukerService>()
    val poaoTilgang: PoaoTilgangService by inject()

    route("/api/v1/internal/delMedBruker") {
        post {
            val payload = call.receive<DelMedBruker>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), payload.norskIdent)

            delMedBrukerService.lagreDelMedBruker(payload)
                .tap {
                    call.respond(it)
                }
                .tapLeft {
                    SecureLog.logger.error("Klarte ikke lagre informasjon om deling med bruker", it.error)
                    call.respondText(
                        "Klarte ikke lagre informasjon om deling med bruker",
                        status = HttpStatusCode.InternalServerError
                    )
                }
        }

        get("{sanityId}") {
            val sanityId = call.getNonEmptyPathParameter("sanityId")
            val fnr = call.getNonEmptyQueryParameter("fnr")

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), fnr)

            delMedBrukerService.getDeltMedBruker(fnr, sanityId)
                .tap {
                    if (it == null) {
                        call.respondText(
                            status = HttpStatusCode.NoContent,
                            text = "Fant ikke innslag om at veileder har delt tiltak med bruker tidligere"
                        )
                    } else {
                        call.respond(it)
                    }
                }
                .tapLeft {
                    call.respondText(
                        status = HttpStatusCode.InternalServerError,
                        text = "Klarte ikke innslag om at veileder har delt tiltak med bruker tidligere"
                    )
                }
        }
    }
}
