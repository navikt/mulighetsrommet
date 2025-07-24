package no.nav.mulighetsrommet.api.veilederflate.routes

import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.common.audit_log.cef.CefMessage
import no.nav.common.audit_log.cef.CefMessageEvent
import no.nav.common.audit_log.cef.CefMessageSeverity
import no.nav.mulighetsrommet.api.plugins.getNavAnsattEntraObjectId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.services.PoaoTilgangService
import no.nav.mulighetsrommet.api.veilederflate.models.Deltakelse
import no.nav.mulighetsrommet.api.veilederflate.services.BrukerService
import no.nav.mulighetsrommet.api.veilederflate.services.DeltakelserMelding
import no.nav.mulighetsrommet.api.veilederflate.services.TiltakshistorikkService
import no.nav.mulighetsrommet.auditlog.AuditLog
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.koin.ktor.ext.inject
import java.util.*

fun Route.brukerRoutes() {
    val brukerService: BrukerService by inject()
    val historikkService: TiltakshistorikkService by inject()
    val poaoTilgangService: PoaoTilgangService by inject()

    route("bruker") {
        post({
            summary = "Hent brukerdata for en bruker"
            description = "Henter brukerdata for en bruker basert på norskIdent. Krever tilgang til brukeren."
            tags = setOf("Bruker")
            operationId = "getBrukerdata"
            request {
                body<GetBrukerRequest> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Data om bruker"
                    body<BrukerService.Brukerdata>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<GetBrukerRequest>()

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattEntraObjectId(), request.norskIdent)

            val obo = AccessType.OBO(call.getAccessToken())
            call.respond(brukerService.hentBrukerdata(request.norskIdent, obo))
        }

        post("tiltakshistorikk", {
            summary = "Hent tiltakshistorikk for en bruker"
            description =
                "Henter aktive eller historiske tiltaksdeltakelser for en bruker basert på norskIdent og type. Krever tilgang til brukeren."
            tags = setOf("Historikk")
            operationId = "getTiltakshistorikk"
            request {
                body<GetDeltakelserForBrukerRequest> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Tiltakshistorikk for bruker"
                    body<GetDeltakelserForBrukerResponse>()
                }
                default {
                    description = "Feil ved henting av tiltakshistorikk"
                    body<ProblemDetail>()
                }
            }
        }) {
            val (norskIdent, type) = call.receive<GetDeltakelserForBrukerRequest>()
            val navIdent = getNavIdent()
            val obo = AccessType.OBO(call.getAccessToken())

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattEntraObjectId(), norskIdent)

            val tiltakshistorikk = historikkService.hentHistorikk(norskIdent, obo)

            val response = GetDeltakelserForBrukerResponse(
                meldinger = tiltakshistorikk.meldinger,
                deltakelser = when (type) {
                    BrukerDeltakelseType.AKTIVE -> tiltakshistorikk.aktive
                    BrukerDeltakelseType.HISTORISKE -> tiltakshistorikk.historiske
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

        post("deltakelse", {
            summary = "Hent aktiv deltakelse for en bruker"
            description =
                "Henter en aktiv deltakelse for en bruker basert på norskIdent og tiltakId. Krever tilgang til brukeren."
            tags = setOf("Historikk")
            operationId = "hentDeltakelse"
            request {
                body<GetAktivDeltakelseForBrukerRequest> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Aktiv deltakelse for bruker"
                    body<Deltakelse.DeltakelseGruppetiltak>()
                }
                code(HttpStatusCode.NotFound) {
                    description = "Fant ikke aktiv deltakelse for tiltak"
                }
                default {
                    description = "Feil ved henting av aktiv deltakelse"
                    body<ProblemDetail>()
                }
            }
        }) {
            val (norskIdent, tiltakId) = call.receive<GetAktivDeltakelseForBrukerRequest>()
            val obo = AccessType.OBO(call.getAccessToken())

            poaoTilgangService.verifyAccessToUserFromVeileder(getNavAnsattEntraObjectId(), norskIdent)

            val deltakelser = historikkService.getGruppetiltakDeltakelser(norskIdent, obo)

            val response = deltakelser.aktive
                .firstOrNull {
                    it is Deltakelse.DeltakelseGruppetiltak && it.gjennomforingId == tiltakId
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

enum class BrukerDeltakelseType {
    AKTIVE,
    HISTORISKE,
}

@Serializable
data class GetDeltakelserForBrukerRequest(
    val norskIdent: NorskIdent,
    val type: BrukerDeltakelseType,
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
    val tiltakId: UUID,
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
