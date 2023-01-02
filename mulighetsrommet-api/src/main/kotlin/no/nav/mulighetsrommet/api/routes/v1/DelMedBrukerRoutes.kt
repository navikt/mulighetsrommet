package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.DelMedBrukerService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.domain.models.DelMedBruker
import no.nav.mulighetsrommet.secure_log.SecureLog
import org.koin.ktor.ext.inject

fun Route.delMedBrukerRoutes() {
    val secureLog = SecureLog.logger
    val delMedBrukerService by inject<DelMedBrukerService>()
    val poaoTilgang: PoaoTilgangService by inject()

    route("/api/v1/internal/delMedBruker") {
        post {
            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent())
            val payload = call.receive<DelMedBruker>()
            delMedBrukerService.lagreDelMedBruker(payload).map {
                call.respond(it)
            }.mapLeft {
                secureLog.error("Klarte ikke lagre informasjon om deling med bruker", it.error)
                call.respondText(
                    "Klarte ikke lagre informasjon om deling med bruker",
                    status = HttpStatusCode.InternalServerError
                )
            }
        }

        get("{tiltaksnummer}") {
            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent())
            val fnr = call.request.queryParameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr til bruker",
                status = HttpStatusCode.BadRequest
            )

            val tiltaksnummer = call.parameters["tiltaksnummer"] ?: ""

            delMedBrukerService.getDeltMedBruker(fnr, tiltaksnummer).map {
                if (it == null) {
                    call.respondText(
                        status = HttpStatusCode.NoContent,
                        text = "Fant ikke innslag om at veileder har delt tiltak med bruker tidligere"
                    )
                }
                call.respond(it!!)
            }.mapLeft {
                call.respondText(
                    status = HttpStatusCode.NoContent,
                    text = "Fant ikke innslag om at veileder har delt tiltak med bruker tidligere"
                )
            }
        }
    }
}
