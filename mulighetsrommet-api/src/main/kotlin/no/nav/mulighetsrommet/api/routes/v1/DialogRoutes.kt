package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.DialogRequest
import no.nav.mulighetsrommet.api.services.DialogService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.auditlog.AuditLog
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import org.koin.ktor.ext.inject

fun Route.dialogRoutes() {
    val dialogService: DialogService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/dialog") {
        post {
            val request = call.receive<DialogRequest>()
            val norskIdent = request.norskIdent
            val navIdent = getNavIdent()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), norskIdent)

            val accessToken = call.getAccessToken()
            val response = dialogService.sendMeldingTilDialogen(accessToken, request.copy(fnr = norskIdent))
            response?.let {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' har delt informasjon om tiltaket '${request.overskrift}' til bruker med ident: '$norskIdent'.",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
                call.respond(response)
            } ?: run {
                val message = createAuditMessage(
                    msg = "NAV-ansatt med ident: '$navIdent' fikk ikke delt informasjon om tiltaket '${request.overskrift}' til bruker med ident: '$norskIdent'.",
                    navIdent = navIdent,
                    norskIdent = norskIdent,
                )
                AuditLog.auditLogger.log(message)
                call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}

private fun createAuditMessage(msg: String, navIdent: NavIdent, norskIdent: String): CefMessage {
    return CefMessage.builder()
        .applicationName("modia")
        .loggerName("mulighetsrommet-api")
        .event(CefMessageEvent.CREATE)
        .name("Arbeidsmarkedstiltak - Del med bruker")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(navIdent.value)
        .destinationUserId(norskIdent)
        .timeEnded(System.currentTimeMillis())
        .extension("msg", msg)
        .build()
}
