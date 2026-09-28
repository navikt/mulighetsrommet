package no.nav.mulighetsrommet.api.tilskuddbehandling.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.default
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.clients.helved.HelVedSimuleringResponse
import no.nav.mulighetsrommet.api.clients.helved.HelVedSimuleringsError
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.tilskuddbehandling.TilskuddService
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.Tilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.Tilskuddsnummer
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultatDto
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tilskuddRoutes() {
    val db: ApiDatabase by inject()
    val tilskuddService: TilskuddService by inject()

    route("tilskudd") {
        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            get({
                description = "Hent alle tilskudds for en gjennomføring"
                tags = setOf("Tilskudd")
                operationId = "getAllTilskuddKompakt"
                request {
                    queryParameterUuid("gjennomforingId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Liste av tilskudd"
                        body<List<TilskuddKompaktDto>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val gjennomforingId = call.parameters.getOrFail<UUID>("gjennomforingId")
                val result = db.session { queries.tilskudd.getAll(gjennomforingId).map { TilskuddKompaktDto.fromTilskuddKompakt(it) } }
                call.respond(result)
            }

            get("/{tilskuddId}", {
                description = "Hent tilskudds gitt id"
                tags = setOf("Tilskudd")
                operationId = "getTilskudd"
                request {
                    pathParameterUuid("tilskuddId")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Tilskudd"
                        body<TilskuddDto>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val tilskuddId = call.parameters.getOrFail<UUID>("tilskuddId")
                val navIdent = getNavIdent()
                val result = db.session {
                    val tilskudd = queries.tilskudd.get(tilskuddId) ?: return@session null
                    val handlinger = handlingerFor(tilskudd, navIdent)
                    TilskuddDto.from(tilskudd, handlinger)
                }
                result?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NotFound)
            }

            post("/simuler-opphor", {
                description = "Simuler opphor for gitt vedtak og tilskudd"
                tags = setOf("Tilskudd")
                operationId = "postTilskuddVedtakOpphorSimulering"
                request {
                    body<SimulerOpphorRequest>()
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Simulert opphør"
                        body<HelVedSimuleringResponse>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val request = call.receive<SimulerOpphorRequest>()
                if (NaisEnv.current().isProdGCP()) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        "Opphørssimulering er kun tillatt i dev-gcp miljøet",
                    )
                } else {
                    tilskuddService.simulerOpphor(request.gjennomforingId, request.vedtakId, request.belop).onLeft {
                        val result = when (it) {
                            HelVedSimuleringsError.BadRequest -> HttpStatusCode.BadRequest
                            HelVedSimuleringsError.NotFound -> HttpStatusCode.NotFound
                            HelVedSimuleringsError.Conflict -> HttpStatusCode.Conflict
                            HelVedSimuleringsError.Error -> HttpStatusCode.InternalServerError
                        }
                        call.respond(result)
                    }.onRight {
                        call.respond(HttpStatusCode.OK, it)
                    }
                }
            }
        }
    }
}

@Serializable
data class SimulerOpphorRequest(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val vedtakId: UUID,
    val belop: Int = 0,
)

@Serializable
data class TilskuddKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: Opplaeringtilskudd,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val tilskuddsnummer: Tilskuddsnummer,
    val sisteVedtakResultat: VedtakResultatDto?,
    val periode: Periode?,
    val sisteVedtakLopenummer: Int?,
) {
    companion object {
        fun fromTilskuddKompakt(tilskuddKompakt: TilskuddKompakt): TilskuddKompaktDto {
            return TilskuddKompaktDto(
                id = tilskuddKompakt.id,
                type = tilskuddKompakt.type,
                gjennomforingId = tilskuddKompakt.gjennomforingId,
                tilskuddsnummer = tilskuddKompakt.tilskuddsnummer,
                sisteVedtakResultat = tilskuddKompakt.sisteVedtakResultat?.let { VedtakResultatDto(it) },
                periode = tilskuddKompakt.periode,
                sisteVedtakLopenummer = tilskuddKompakt.sisteVedtakLopenummer,
            )
        }
    }
}

@Serializable
data class TilskuddDto(
    val tilskudd: Tilskudd,
    val handlinger: Set<TilskuddHandling>,
) {
    companion object {
        fun from(tilskudd: Tilskudd, handlinger: Set<TilskuddHandling>): TilskuddDto {
            return TilskuddDto(
                tilskudd = tilskudd,
                handlinger = handlinger,
            )
        }
    }
}

@Serializable
enum class TilskuddHandling {
    OPPHOR,
}

private fun QueryContext.handlingerFor(tilskudd: Tilskudd, navIdent: NavIdent): Set<TilskuddHandling> {
    val ansatt = queries.ansatt.get(navIdent) ?: return emptySet()
    val ansattITeamMulighetsrommet = ansatt.hasGenerellRolle(Rolle.TEAM_MULIGHETSROMMET)

    val sisteVedtak = tilskudd.vedtak.maxBy { it.lopenummer }
    val erIkkeOpphor = when (sisteVedtak.utbetaling) {
        is Tilskudd.Vedtak.Utbetaling.Arrangor -> sisteVedtak.utbetaling.belop.belop > 0
        is Tilskudd.Vedtak.Utbetaling.Bruker -> sisteVedtak.utbetaling.belop.belop > 0
        null -> false
    }
    val erInnvilget = sisteVedtak.vedtakResultat == VedtakResultat.INNVILGELSE

    return if (erInnvilget && erIkkeOpphor && ansattITeamMulighetsrommet) {
        setOf(TilskuddHandling.OPPHOR)
    } else {
        emptySet()
    }
}
