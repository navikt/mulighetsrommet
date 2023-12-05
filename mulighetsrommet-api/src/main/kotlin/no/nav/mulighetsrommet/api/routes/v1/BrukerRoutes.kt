package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.TiltakshistorikkService
import no.nav.mulighetsrommet.auditlog.AuditLog
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import org.koin.ktor.ext.inject

fun Route.brukerRoutes() {
    val brukerService: BrukerService by inject()
    val historikkService: TiltakshistorikkService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/bruker") {
        post {
            val request = call.receive<GetBrukerRequest>()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), request.norskIdent)

            val accessToken = call.getAccessToken()
            call.respond(brukerService.hentBrukerdata(request.norskIdent, accessToken))
        }
    }

    route("/api/v1/internal/bruker/historikk") {
        post {
            val request = call.receive<GetHistorikkForBrukerRequest>()
            val norskIdent = request.norskIdent
            val navIdent = getNavIdent()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), norskIdent) {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' forsøkte, men fikk ikke sett tiltakshistorikken for bruker med ident: '$norskIdent'.",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
            }

            historikkService.hentHistorikkForBruker(norskIdent).let {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' har sett på tiltakshistorikken for bruker med ident: '$norskIdent'.",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)

                call.respond(it)
            }
        }
    }
}

@Serializable
data class GetBrukerRequest(
    val norskIdent: String,
)

@Serializable
data class GetHistorikkForBrukerRequest(
    val norskIdent: String,
)

private fun createAuditMessage(msg: String, navIdent: String, norskIdent: String): CefMessage {
    return CefMessage.builder()
        .applicationName("modia")
        .loggerName("mulighetsrommet-api")
        .event(CefMessageEvent.ACCESS)
        .name("Arbeidsmarkedstiltak - Vis tiltakshistorikk")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navIdent)
        .destinationUserId(norskIdent)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", msg)
        .build()
}
