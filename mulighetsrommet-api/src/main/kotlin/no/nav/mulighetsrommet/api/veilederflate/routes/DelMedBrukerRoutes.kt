package no.nav.mulighetsrommet.api.veilederflate.routes

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.dialog.DialogRequest
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogError
import no.nav.mulighetsrommet.api.plugins.getNavAnsattEntraObjectId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.veilederflate.models.DelMedBrukerDto
import no.nav.mulighetsrommet.api.veilederflate.models.TiltakDeltMedBruker
import no.nav.mulighetsrommet.api.veilederflate.services.DelMedBrukerInsertDbo
import no.nav.mulighetsrommet.api.veilederflate.services.DelMedBrukerService
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.koin.ktor.ext.inject
import java.util.*

fun Route.delMedBrukerRoutes() {
    val dialogClient: VeilarbdialogClient by inject()
    val poaoTilgang: PoaoTilgangService by inject()
    val delMedBrukerService: DelMedBrukerService by inject()

    route("del-med-bruker") {
        post({
            summary = "Del tiltak med bruker"
            description = "Deler tiltak med bruker ved å sende melding til dialogen. Krever tilgang til brukeren."
            tags = setOf("Del med bruker")
            operationId = "delTiltakMedBruker"
            request {
                body<DelTiltakMedBrukerRequest> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltak har blitt delt med bruker"
                    body<DelTiltakMedBrukerResponse>()
                }
                code(HttpStatusCode.Conflict) {
                    description = "Tiltak ble ikke delt med bruker"
                }
                default {
                    description = "Feil ved deling av tiltak med bruker"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<DelTiltakMedBrukerRequest>()
            val navIdent = getNavIdent()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattEntraObjectId(), request.fnr)

            if (request.sanityId == null && request.gjennomforingId == null) {
                throw BadRequestException("sanityId eller gjennomforingId må inkluderes")
            }

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
                    val dbo = DelMedBrukerInsertDbo(
                        norskIdent = request.fnr,
                        navIdent = navIdent,
                        dialogId = dialogResponse.id,
                        sanityId = request.sanityId,
                        gjennomforingId = request.gjennomforingId,
                        tiltakstypeId = request.tiltakstypeId,
                        deltFraEnhet = request.deltFraEnhet,
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
                                message = "Kan ikke dele tiltak med bruker, utbetaling for digital kommunikasjon ikke oppfylt.",
                            )
                        }
                    }
                }
        }

        post("status", {
            summary = "Hent status for deling av tiltak med bruker"
            description =
                "Henter informasjon om et tiltak er delt med en bruker basert på norskIdent og tiltakId. Krever tilgang til brukeren."
            tags = setOf("Del med bruker")
            operationId = "getDelMedBruker"
            request {
                body<GetDelMedBrukerRequest> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltak er delt med bruker"
                    body<DelMedBrukerDto>()
                }
                code(HttpStatusCode.NoContent) {
                    description = "Ingen informasjon funnet for tiltaket"
                }
                default {
                    description = "Feil ved henting av status"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<GetDelMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattEntraObjectId(), request.norskIdent)

            val deltMedBruker = delMedBrukerService.getTiltakDeltMedBruker(request.norskIdent, request.tiltakId)
                ?: return@post call.respond(HttpStatusCode.NoContent)

            call.respond(deltMedBruker)
        }

        post("alle", {
            summary = "Hent alle tiltak delt med bruker"
            description =
                "Henter siste informasjon om alle tiltak delt med en bruker basert på norskIdent. Krever tilgang til brukeren."
            tags = setOf("Del med bruker")
            operationId = "getAlleTiltakDeltMedBruker"
            request {
                body<GetAlleDeltMedBrukerRequest> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Siste informasjon om alle tiltak delt med bruker"
                    body<List<DelMedBrukerDto>>()
                }
                code(HttpStatusCode.NoContent) {
                    description = "Ingen informasjon funnet"
                }
                default {
                    description = "Feil ved henting av delte tiltak"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<GetAlleDeltMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattEntraObjectId(), request.norskIdent)

            call.respond(delMedBrukerService.getAlleDistinkteTiltakDeltMedBruker(request.norskIdent))
        }

        post("historikk", {
            summary = "Hent historikk for tiltak delt med bruker"
            description =
                "Henter historikk om alle tiltak delt med en bruker basert på norskIdent. Krever tilgang til brukeren."
            tags = setOf("Del med bruker")
            operationId = "getHistorikkForDeltMedBruker"
            request {
                body<GetAlleDeltMedBrukerRequest> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Historikk for tiltak delt med bruker"
                    body<List<TiltakDeltMedBruker>>()
                }
                code(HttpStatusCode.NoContent) {
                    description = "Ingen informasjon funnet"
                }
                default {
                    description = "Feil ved henting av historikk"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<GetAlleDeltMedBrukerRequest>()

            poaoTilgang.verifyAccessToUserFromVeileder(getNavAnsattEntraObjectId(), request.norskIdent)

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
    val tiltakstypeId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID?,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val deltFraEnhet: NavEnhetNummer,
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
