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
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.DialogRequest
import no.nav.mulighetsrommet.api.services.DialogService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.auditlog.AuditLog
import org.koin.ktor.ext.inject

fun Route.dialogRoutes() {
    val auditLog = AuditLog.auditLogger
    val dialogService: DialogService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/dialog") {
        post {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent())
            val dialogRequest = call.receive<DialogRequest>()
            val accessToken = call.getAccessToken()
            val response = dialogService.sendMeldingTilDialogen(getNorskIdent(), accessToken, dialogRequest)
            response?.let {
                auditLog.log(createAuditMessage("NAV-ansatt med ident: '${getNavIdent()}' har delt informasjon om tiltaket '${dialogRequest.overskrift}' til bruker med ident: '${getNorskIdent()}'."))
                call.respond(response)
            } ?: run {
                auditLog.log(createAuditMessage("NAV-ansatt med ident: '${getNavIdent()}' fikke ikke delt informasjon om tiltaket '${dialogRequest.overskrift}' til bruker med ident: '${getNorskIdent()}'."))
                call.respond(HttpStatusCode.Conflict)
            }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.createAuditMessage(
    msg: String,
): CefMessage? {
    return CefMessage.builder()
        .applicationName("modia")
        .loggerName("mulighetsrommet-api")
        .event(CefMessageEvent.CREATE)
        .name("Arbeidsmarkedstiltak - Del med bruker")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(getNavIdent())
        .destinationUserId(getNorskIdent())
        .timeEnded(System.currentTimeMillis())
        .extension(
            "msg",
            msg,
        )
        .build()
}
