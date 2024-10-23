package no.nav.mulighetsrommet.api.veilederflate.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.dialog.DialogRequest
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogError
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.veilederflate.DelMedBrukerService
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.koin.ktor.ext.inject
import java.util.*

fun Route.delMedBrukerRoutes() {
    val dialogClient: VeilarbdialogClient by inject()
    val poaoTilgang: PoaoTilgangService by inject()
    val delMedBrukerService: DelMedBrukerService by inject()

    route("del-med-bruker") {
        post {
            val request = call.receive<DelTiltakMedBrukerRequest>()
            val navIdent = getNavIdent()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.fnr)

            val obo = AccessType.OBO(call.getAccessToken())
            val dialogRequest = request.run {
                DialogRequest(
                    fnr = fnr,
                    overskrift = overskrift,
                    tekst = tekst,
                    venterPaaSvarFraBruker = venterPaaSvarFraBruker,
                )
            }
            dialogClient.sendMeldingTilDialogen(obo, dialogRequest)
                .onRight { dialogResponse ->
                    val dbo = DelMedBrukerDbo(
                        norskIdent = request.fnr,
                        navident = navIdent.value,
                        dialogId = dialogResponse.id,
                        sanityId = request.sanityId,
                        tiltaksgjennomforingId = request.tiltaksgjennomforingId,
                    )
                    delMedBrukerService.lagreDelMedBruker(dbo)

                    val response = DelTiltakMedBrukerResponse(
                        dialogId = dialogResponse.id,
                    )
                    call.respond(response)
                }
                .onLeft {
                    when (it) {
                        VeilarbdialogError.Error -> {
                            call.respond(
                                status = HttpStatusCode.InternalServerError,
                                message = "Kunne ikke sende melding til dialogen",
                            )
                        }

                        VeilarbdialogError.OppfyllerIkkeKravForDigitalKommunikasjon -> {
                            call.respond(
                                status = HttpStatusCode.Conflict,
                                message = "Kan ikke dele tiltak med bruker, krav for digital kommunikasjon ikke oppfylt.",
                            )
                        }
                    }
                }
        }

        post("status") {
            val request = call.receive<GetDelMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            val deltMedBruker = delMedBrukerService.getDeltMedBruker(request.norskIdent, request.tiltakId)
                ?: call.respondText(
                    status = HttpStatusCode.NoContent,
                    text = "Fant ikke innslag om at veileder har delt tiltak med bruker tidligere",
                )
            call.respond(deltMedBruker)
        }

        post("alle") {
            val request = call.receive<GetAlleDeltMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            call.respond(delMedBrukerService.getAlleDistinkteTiltakDeltMedBruker(request.norskIdent))
        }

        post("historikk") {
            val request = call.receive<GetAlleDeltMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            call.respond(delMedBrukerService.getDelMedBrukerHistorikk(request.norskIdent))
        }
    }
}

@Serializable
data class DelTiltakMedBrukerRequest(
    val overskrift: String,
    val tekst: String,
    val venterPaaSvarFraBruker: Boolean,
    val fnr: NorskIdent,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID?,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
)

@Serializable
data class DelTiltakMedBrukerResponse(
    val dialogId: String,
)

@Serializable
data class GetDelMedBrukerRequest(
    @Serializable(with = UUIDSerializer::class)
    val tiltakId: UUID,
    val norskIdent: NorskIdent,
)

@Serializable
data class GetAlleDeltMedBrukerRequest(
    val norskIdent: NorskIdent,
)
