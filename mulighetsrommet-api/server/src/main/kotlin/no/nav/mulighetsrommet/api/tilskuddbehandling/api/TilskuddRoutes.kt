package no.nav.mulighetsrommet.api.tilskuddbehandling.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.default
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.Tilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddKompakt
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultatDto
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.tilskuddRoutes() {
    val db: ApiDatabase by inject()

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
                        body<Tilskudd>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val tilskuddId = call.parameters.getOrFail<UUID>("tilskuddId")
                val result = db.session { queries.tilskudd.get(tilskuddId) }
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(result)
            }
        }
    }
}

@Serializable
data class TilskuddKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: Opplaeringtilskudd,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val tilskuddsnummer: String,
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
