package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.dialog.DialogRequest
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogClient
import no.nav.mulighetsrommet.api.clients.dialog.VeilarbdialogError
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.DelMedBrukerService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.auditlog.AuditLog
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import org.koin.ktor.ext.inject
import java.util.*

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

                    val audit = createAuditMessage(
                        msg = "NAV-ansatt med ident: '$navIdent' har delt informasjon om tiltaket '${request.overskrift}' til bruker med ident: '${request.fnr}'.",
                        navIdent = navIdent,
                        norskIdent = request.fnr,
                    )
                    AuditLog.auditLogger.log(audit)

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

            delMedBrukerService.getDeltMedBruker(request.norskIdent, request.tiltakId)
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

private fun createAuditMessage(msg: String, navIdent: NavIdent, norskIdent: NorskIdent): CefMessage {
    return CefMessage.builder()
        .applicationName("modia")
        .loggerName("mulighetsrommet-api")
        .event(CefMessageEvent.CREATE)
        .name("Arbeidsmarkedstiltak - Del med bruker")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navIdent.value)
        .destinationUserId(norskIdent.value)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", msg)
        .build()
}
