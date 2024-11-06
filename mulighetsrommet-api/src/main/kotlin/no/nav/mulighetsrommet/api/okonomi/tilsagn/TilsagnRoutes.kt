package no.nav.mulighetsrommet.api.okonomi.tilsagn

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.okonomi.prismodell.Prismodell
import no.nav.mulighetsrommet.api.okonomi.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.NotFound
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.tilsagnRoutes() {
    val service: TilsagnService by inject()

    route("tilsagn") {
        get("/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            val result = service.get(id) ?: NotFound()

            call.respond(result)
        }

        post("/beregn") {
            val request = call.receive<TilsagnBeregningInput>()

            val result = service.tilsagnBeregning(request)
                .mapLeft { BadRequest(errors = it) }

            call.respondWithStatusResponse(result)
        }

        authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
            put {
                val request = call.receive<TilsagnRequest>()
                val navIdent = getNavIdent()

                val result = service.upsert(request, navIdent)
                    .mapLeft { BadRequest(errors = it) }

                call.respondWithStatusResponse(result)
            }

            delete("/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")

                call.respondWithStatusResponse(service.annuller(id))
            }
        }

        authenticate(AuthProvider.AZURE_AD_OKONOMI_BESLUTTER) {
            post("/{id}/beslutt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<BesluttTilsagnRequest>()
                val navIdent = getNavIdent()

                call.respondWithStatusResponse(service.beslutt(id, request, navIdent))
            }
        }

        get("/aft/sats") {
            call.respond(
                Prismodell.AFT.satser.map {
                    AFTSats(
                        startDato = it.key,
                        belop = it.value,
                    )
                },
            )
        }
    }

    route("/tiltaksgjennomforinger/{id}/tilsagn") {
        authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
            get {
                val tiltaksgjennomforingId = call.parameters.getOrFail<UUID>("id")

                val result = service.getByGjennomforingId(tiltaksgjennomforingId)

                call.respond(result)
            }
        }
    }
}

@Serializable
data class TilsagnRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tiltaksgjennomforingId: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val kostnadssted: String,
    val beregning: Prismodell.TilsagnBeregning,
) {
    fun toDbo(
        opprettetAv: NavIdent,
        arrangorId: UUID,
    ) = TilsagnDbo(
        id = id,
        tiltaksgjennomforingId = tiltaksgjennomforingId,
        periodeStart = periodeStart,
        periodeSlutt = periodeSlutt,
        kostnadssted = kostnadssted,
        beregning = beregning,
        opprettetAv = opprettetAv,
        arrangorId = arrangorId,
    )
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("besluttelse")
sealed class BesluttTilsagnRequest(
    val besluttelse: TilsagnBesluttelse,
) {

    @Serializable
    @SerialName("GODKJENT")
    data object GodkjentTilsagnRequest : BesluttTilsagnRequest(
        besluttelse = TilsagnBesluttelse.GODKJENT,
    )

    @Serializable
    @SerialName("AVVIST")
    data class AvvistTilsagnRequest(
        val aarsaker: List<AvvistTilsagnAarsak>,
        val forklaring: String?,
    ) : BesluttTilsagnRequest(
        besluttelse = TilsagnBesluttelse.AVVIST,
    )
}

@Serializable
enum class TilsagnBesluttelse {
    GODKJENT,
    AVVIST,
}

@Serializable
enum class AvvistTilsagnAarsak {
    FEIL_ANTALL_PLASSER,
    FEIL_KOSTNADSSTED,
    FEIL_PERIODE,
    FEIL_BELOP,
    FEIL_ANNET,
}

@Serializable
sealed class TilsagnBeregningInput {
    @Serializable
    @SerialName("AFT")
    data class AFT(
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val antallPlasser: Int,
        val sats: Int,
    ) : TilsagnBeregningInput()

    @Serializable
    @SerialName("FRI")
    data class Fri(val belop: Int) : TilsagnBeregningInput()
}

@Serializable
data class AFTSats(
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    val belop: Int,
)
