package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.plugins.getNavAnsattAzureId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.getNorskIdent
import no.nav.mulighetsrommet.api.services.BrukerService
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.services.TiltakshistorikkService
import no.nav.mulighetsrommet.api.utils.getAccessToken
import no.nav.mulighetsrommet.auditlog.AuditLog
import no.nav.mulighetsrommet.securelog.SecureLog
import org.koin.ktor.ext.inject

fun Route.brukerRoutes() {
    val auditLog = AuditLog.auditLogger
    val secureLog = SecureLog.logger
    val brukerService: BrukerService by inject()
    val historikkService: TiltakshistorikkService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("/api/v1/internal/bruker") {
        get {
            val fnr = getNorskIdent()
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), fnr)

            val accessToken = call.getAccessToken()
            call.respond(brukerService.hentBrukerdata(fnr, accessToken))
        }
    }

    route("/api/v1/internal/bruker/historikk") {
        get {
            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattAzureId(), getNorskIdent()) {
                auditLog.log(createAuditMessage("NAV-ansatt med ident: '${getNavIdent()}' forsøkte, men fikk ikke sett tiltakshistorikken for bruker med ident: '${getNorskIdent()}'."))
                secureLog.warn("NAV-ansatt med ident: '${getNavIdent()}' har ikke tilgang til bruker med ident: '${getNorskIdent()}'")
            }
            historikkService.hentHistorikkForBruker(getNorskIdent()).let {
                auditLog.log(createAuditMessage("NAV-ansatt med ident: '${getNavIdent()}' har sett på tiltakshistorikken for bruker med ident: '${getNorskIdent()}'."))
                call.respond(it)
            }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.createAuditMessage(msg: String): CefMessage? {
    return CefMessage.builder()
        .applicationName("modia")
        .loggerName("mulighetsrommet-api")
        .event(CefMessageEvent.ACCESS)
        .name("Arbeidsmarkedstiltak - Vis tiltakshistorikk")
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
