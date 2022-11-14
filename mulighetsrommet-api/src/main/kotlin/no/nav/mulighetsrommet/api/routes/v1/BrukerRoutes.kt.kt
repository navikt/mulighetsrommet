package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.services.HistorikkService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.audit_log.AuditLog
import org.koin.ktor.ext.inject

fun Route.brukerRoutes() {
    val auditLog = AuditLog.auditLogger
    val brukerService: BrukerService by inject()
    val historikkService: HistorikkService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/bruker") {
        get {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavIdent(), getNorskIdent())
            val fnr = call.request.queryParameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val accessToken = call.getAccessToken()
            call.respond(brukerService.hentBrukerdata(fnr, accessToken))
        }
    }

    route("/api/v1/bruker/historikk") {
        get {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavIdent(), getNorskIdent())
            val fnr = call.request.queryParameters["fnr"] ?: return@get call.respondText(
                "Mangler eller ugyldig fnr",
                status = HttpStatusCode.BadRequest
            )
            val accessToken = call.getAccessToken()
            auditLog.log(createAuditMessage("NAV-ansatt med ident: '${getNavIdent()}' forsøker å se på tiltakshistorikken for bruker med ident: '${getNorskIdent()}'."))
            historikkService.hentHistorikkForBruker(fnr, accessToken)?.let {
                auditLog.log(createAuditMessage("NAV-ansatt med ident: '${getNavIdent()}' har sett på tiltakshistorikken for bruker med ident: '${getNorskIdent()}'."))
                call.respond(it)
            }
                ?: run {
                    auditLog.log(createAuditMessage("NAV-ansatt med ident: '${getNavIdent()}' fikk ikke sett på tiltakshistorikken for bruker med ident: '${getNorskIdent()}'."))
                    call.respondText(
                        "Klarte ikke hente historikk for bruker",
                        status = HttpStatusCode.InternalServerError
                    )
                }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.createAuditMessage(msg: String): CefMessage? {
    return CefMessage.builder()
        .applicationName("mulighetsrommet-api")
        .event(CefMessageEvent.ACCESS)
        .name("Arbeidsmarkedstiltak")
        .severity(CefMessageSeverity.INFO)
        .sourceUserId(getNavIdent())
        .destinationUserId(getNorskIdent())
        .timeEnded(System.currentTimeMillis())
        .extension(
            "msg",
            msg
        )
        .build()
}
