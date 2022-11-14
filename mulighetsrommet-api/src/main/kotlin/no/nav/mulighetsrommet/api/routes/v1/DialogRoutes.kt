package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.DialogRequest
import no.nav.mulighetsrommet.api.services.DialogService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.audit_log.AuditLog
import org.koin.ktor.ext.inject

fun Route.dialogRoutes() {
    val auditLog = AuditLog.auditLogger
    val dialogService: DialogService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/dialog") {
        post {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavIdent(), getNorskIdent())
            val dialogRequest = call.receive<DialogRequest>()
            val fnr = call.request.queryParameters["fnr"] ?: return@post call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val accessToken = call.getAccessToken()
            auditLog.log(createAuditMessage(dialogRequest))
            val response = dialogService.sendMeldingTilDialogen(fnr, accessToken, dialogRequest)
            response?.let { call.respond(response) } ?: call.respond(HttpStatusCode.Conflict)
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.createAuditMessage(
    dialogRequest: DialogRequest
): CefMessage? {
    return CefMessage.builder()
        .applicationName("mulighetsrommet-api")
        .event(CefMessageEvent.CREATE)
        .name("Arbeidsmarkedstiltak")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(getNavIdent())
        .destinationUserId(getNorskIdent())
        .timeEnded(System.currentTimeMillis())
        .extension(
            "msg",
            "NAV-ansatt med ident: '${getNavIdent()}' har delt informasjon om tiltaket '${dialogRequest.overskrift}' til bruker med ident: '${getNorskIdent()}'."
        )
        .build()
}
