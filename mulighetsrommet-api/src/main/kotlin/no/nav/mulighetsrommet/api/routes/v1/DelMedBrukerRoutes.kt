package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.services.DelMedBrukerService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.delMedBrukerRoutes() {
    val delMedBrukerService by inject<DelMedBrukerService>()
    val poaoTilgang: PoaoTilgangService by inject()

    route("del-med-bruker") {
        post {
            val request = call.receive<GetDelMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            delMedBrukerService.getDeltMedBruker(request.norskIdent, request.id)
                .onRight {
                    if (it == null) {
                        call.respondText(
                            status = HttpStatusCode.NoContent,
                            text = "Fant ikke innslag om at veileder har delt tiltak med bruker tidligere",
                        )
                    } else {
                        call.respond(it)
                    }
                }
                .onLeft {
                    call.respondText(
                        status = HttpStatusCode.InternalServerError,
                        text = "Klarte ikke finne innslag om at veileder har delt tiltak med bruker tidligere",
                    )
                }
        }

        post("alle") {
            val request = call.receive<GetAlleDeltMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            delMedBrukerService.getAlleDistinkteTiltakDeltMedBruker(request.norskIdent)
                .onRight {
                    if (it == null) {
                        call.respondText(
                            status = HttpStatusCode.NoContent,
                            text = "Fant ingen innslag om at veileder har delt noen tiltak med bruker tidligere",
                        )
                    } else {
                        call.respond(it)
                    }
                }
                .onLeft {
                    call.respondText(
                        status = HttpStatusCode.InternalServerError,
                        text = "Klarte ikke finne innslag om at veileder har delt noen tiltak med bruker tidligere",
                    )
                }
        }

        post("historikk") {
            val request = call.receive<GetAlleDeltMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            delMedBrukerService.getDelMedBrukerHistorikk(request.norskIdent)
                .onRight {
                    call.respond(it)
                }
                .onLeft {
                    call.respondText(
                        status = HttpStatusCode.InternalServerError,
                        text = "Klarte ikke finne innslag om at veileder har delt tiltak med bruker tidligere",
                    )
                }
        }
    }
}

@Serializable
data class GetDelMedBrukerRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val norskIdent: NorskIdent,
)

@Serializable
data class GetAlleDeltMedBrukerRequest(
    val norskIdent: NorskIdent,
)
