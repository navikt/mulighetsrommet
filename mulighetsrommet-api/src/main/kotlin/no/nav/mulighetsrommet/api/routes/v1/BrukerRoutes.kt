package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerError
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.BadRequest
import no.nav.mulighetsrommet.api.routes.v1.responses.NotFound
import no.nav.mulighetsrommet.api.routes.v1.responses.ServerError
import no.nav.mulighetsrommet.api.routes.v1.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.TiltakshistorikkService
import no.nav.mulighetsrommet.auditlog.AuditLog
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import org.koin.ktor.ext.inject

fun Route.brukerRoutes() {
    val brukerService: BrukerService by inject()
    val historikkService: TiltakshistorikkService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/intern/bruker") {
        post {
            val request = call.receive<GetBrukerRequest>()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            val obo = AccessType.OBO(call.getAccessToken())
            call.respond(brukerService.hentBrukerdata(request.norskIdent, obo))
        }
    }

    route("/api/v1/intern/bruker/historikk") {
        post {
            val (norskIdent) = call.receive<GetHistorikkForBrukerRequest>()
            val navIdent = getNavIdent()
            val obo = AccessType.OBO(call.getAccessToken())

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), norskIdent) {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' forsøkte, men fikk ikke sett tiltakshistorikken for bruker med ident: '$norskIdent'.",
                    topic = "Vis tiltakshistorikk",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
            }

            historikkService.hentHistorikkForBruker(norskIdent, obo).let {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' har sett på tiltakshistorikken for bruker med ident: '$norskIdent'.",
                    topic = "Vis tiltakshistorikk",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)

                call.respond(it)
            }
        }

        post("ny") {
            val (norskIdent) = call.receive<GetHistorikkForBrukerRequest>()
            val navIdent = getNavIdent()
            val obo = AccessType.OBO(call.getAccessToken())

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), norskIdent) {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' forsøkte, men fikk ikke sett deltakelser for bruker med ident: '$norskIdent'.",
                    topic = "Se deltakelser",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
            }

            val response = historikkService.hentDeltakelserFraKomet(norskIdent, obo).onRight {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' har sett deltakelser for bruker med ident: '$norskIdent'.",
                    topic = "Se deltakelser",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
            }.mapLeft { toStatusResponseError(it) }

            call.respondWithStatusResponse(response)
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

@Serializable
data class GetHistorikkForBrukerRequest(
    val norskIdent: NorskIdent,
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
