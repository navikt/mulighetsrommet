package no.nav.mulighetsrommet.api.veilederflate.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.domain.dto.Deltakelse
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.DeltakelserMelding
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.TiltakshistorikkService
import no.nav.mulighetsrommet.api.veilederflate.BrukerService
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
                    msg = "Nav-ansatt med ident: '$navIdent' har sett på $type tiltaksdeltakelser for bruker med ident: '$norskIdent'.",
                    topic = "Vis tiltakshistorikk",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
            }

            call.respond(response)
        }

        post("deltakelse-for-gjennomforing") {
            val (norskIdent, tiltaksgjennomforingId) = call.receive<GetAktivDeltakelseForBrukerRequest>()
            val obo = AccessType.OBO(call.getAccessToken())

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), norskIdent)

            val deltakelser = historikkService.getGruppetiltakDeltakelser(norskIdent, obo)

            val response = deltakelser.aktive
                .firstOrNull {
                    it is Deltakelse.DeltakelseGruppetiltak && it.gjennomforingId == tiltaksgjennomforingId
                }
                ?: HttpStatusCode.NoContent

            call.respond(response)
        }
    }
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
    val deltakelser: List<Deltakelse>,
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
