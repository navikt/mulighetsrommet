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
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerError
import no.nav.mulighetsrommet.api.domain.dto.DeltakerKort
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.NotFound
import no.nav.mulighetsrommet.api.responses.ServerError
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.services.DeltakelserMelding
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.TiltakshistorikkService
import no.nav.mulighetsrommet.auditlog.AuditLog
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.koin.ktor.ext.inject
import java.util.*

fun Route.brukerRoutes() {
    val brukerService: BrukerService by inject()
    val historikkService: TiltakshistorikkService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("bruker") {
        post {
            val request = call.receive<GetBrukerRequest>()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            val obo = AccessType.OBO(call.getAccessToken())
            call.respond(brukerService.hentBrukerdata(request.norskIdent, obo))
        }

        post("tiltakshistorikk") {
            val (norskIdent, type) = call.receive<GetDeltakelserForBrukerRequest>()
            val navIdent = getNavIdent()
            val obo = AccessType.OBO(call.getAccessToken())

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), norskIdent)

            val tiltakshistorikk = historikkService.hentHistorikk(norskIdent, obo)

            val response = GetDeltakelserForBrukerResponse(
                meldinger = tiltakshistorikk.meldinger,
                deltakelser = when (type) {
                    DeltakelsesType.AKTIVE -> tiltakshistorikk.aktive
                    DeltakelsesType.HISTORISKE -> tiltakshistorikk.historiske
                },
            )

            if (response.deltakelser.isNotEmpty()) {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' har sett på $type tiltaksdeltakelser for bruker med ident: '$norskIdent'.",
                    topic = "Vis tiltakshistorikk",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
            }

            call.respond(response)
        }

        post("historikk") {
            val (norskIdent, type) = call.receive<GetDeltakelserForBrukerRequest>()
            val navIdent = getNavIdent()
            val obo = AccessType.OBO(call.getAccessToken())

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), norskIdent)

            val historikk = historikkService.hentHistorikk(norskIdent, obo).let {
                if (type == DeltakelsesType.AKTIVE) {
                    it.aktive
                } else {
                    it.historiske
                }
            }

            if (historikk.isNotEmpty()) {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' har sett på $type tiltaksdeltakelser for bruker med ident: '$norskIdent'.",
                    topic = "Vis tiltakshistorikk",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
            }

            call.respond(historikk)
        }

        post("deltakelse-for-gjennomforing") {
            val (norskIdent, tiltaksgjennomforingId) = call.receive<GetAktivDeltakelseForBrukerRequest>()
            val obo = AccessType.OBO(call.getAccessToken())

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), norskIdent)

            val deltakelser = historikkService.getGruppetiltakDeltakelser(norskIdent, obo)

            val response = deltakelser.aktive
                .firstOrNull {
                    it is DeltakerKort.DeltakerKortGruppetiltak && it.gjennomforingId == tiltaksgjennomforingId
                }
                ?: HttpStatusCode.NoContent

            call.respond(response)
        }
    }
}

private fun toStatusResponseError(it: AmtDeltakerError) = when (it) {
    AmtDeltakerError.NotFound -> NotFound()
    AmtDeltakerError.BadRequest -> BadRequest()
    AmtDeltakerError.Error -> ServerError()
}

@Serializable
data class GetBrukerRequest(
    val norskIdent: NorskIdent,
)

enum class DeltakelsesType {
    AKTIVE,
    HISTORISKE,
}

@Serializable
data class GetDeltakelserForBrukerRequest(
    val norskIdent: NorskIdent,
    val type: DeltakelsesType,
)

@Serializable
data class GetDeltakelserForBrukerResponse(
    val meldinger: Set<DeltakelserMelding>,
    val deltakelser: List<DeltakerKort>,
)

@Serializable
data class GetAktivDeltakelseForBrukerRequest(
    val norskIdent: NorskIdent,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID,
)

private fun createAuditMessage(msg: String, topic: String, navIdent: NavIdent, norskIdent: NorskIdent): CefMessage {
    return CefMessage.builder()
        .applicationName("modia")
        .loggerName("mulighetsrommet-api")
        .event(CefMessageEvent.ACCESS)
        .name("Arbeidsmarkedstiltak - $topic")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navIdent.value)
        .destinationUserId(norskIdent.value)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", msg)
        .build()
}
