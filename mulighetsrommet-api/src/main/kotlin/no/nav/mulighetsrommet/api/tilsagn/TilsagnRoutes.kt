package no.nav.mulighetsrommet.api.tilsagn

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningInput
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBesluttelseStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*

fun Route.tilsagnRoutes() {
    val service: TilsagnService by inject()
    val gjennomforinger: TiltaksgjennomforingService by inject()

    route("tilsagn") {
        get("/{id}") {
            val id = call.parameters.getOrFail<UUID>("id")

            val result = service.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(result)
        }

        get("/defaults") {
            val gjennomforingId: UUID by call.queryParameters

            val gjennomforing = gjennomforinger.get(gjennomforingId) ?: return@get call.respond(HttpStatusCode.NotFound)

            val byGjennomforingId = service.getByGjennomforingId(gjennomforingId)
            val tilsagn = byGjennomforingId.firstOrNull()

            val defaults = resolveTilsagnDefaults(gjennomforing, tilsagn, service)

            call.respond(HttpStatusCode.OK, defaults)
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

            post("/{id}/til-annullering") {
                val request = call.receive<TilAnnulleringRequest>()
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                call.respondWithStatusResponse(service.tilAnnullering(id, navIdent, request))
            }

            delete("/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")

                call.respondWithStatusResponse(service.slettTilsagn(id))
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
data class TilsagnDefaults(
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val antallPlasser: Int,
    val kostnadssted: String?,
    val beregning: Prismodell.TilsagnBeregning?,
)

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
        endretAv = opprettetAv,
        endretTidspunkt = LocalDateTime.now(),
        arrangorId = arrangorId,
    )
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("besluttelse")
sealed class BesluttTilsagnRequest(
    val besluttelse: TilsagnBesluttelseStatus,
) {
    @Serializable
    @SerialName("GODKJENT")
    data object GodkjentTilsagnRequest : BesluttTilsagnRequest(
        besluttelse = TilsagnBesluttelseStatus.GODKJENT,
    )

    @Serializable
    @SerialName("AVVIST")
    data class AvvistTilsagnRequest(
        val aarsaker: List<TilsagnStatusAarsak>?,
        val forklaring: String?,
    ) : BesluttTilsagnRequest(
        besluttelse = TilsagnBesluttelseStatus.AVVIST,
    )
}

@Serializable
data class TilAnnulleringRequest(
    val aarsaker: List<TilsagnStatusAarsak>,
    val forklaring: String?,
)

@Serializable
data class AFTSats(
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    val belop: Int,
)

private fun resolveTilsagnDefaults(
    gjennomforing: TiltaksgjennomforingDto,
    tilsagn: TilsagnDto?,
    service: TilsagnService,
) = when (gjennomforing.tiltakstype.tiltakskode) {
    Tiltakskode.ARBEIDSFORBEREDENDE_TRENING, Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> {
        val periodeStart = listOfNotNull(
            gjennomforing.startDato,
            tilsagn?.periodeSlutt?.plusDays(1),
        ).max()

        val forhandsgodkjentTilsagnPeriodeSlutt = periodeStart.plusMonths(6).minusDays(1)
        val lastDayOfYear = periodeStart.withMonth(12).withDayOfMonth(31)
        val periodeSlutt = listOfNotNull(
            gjennomforing.sluttDato,
            forhandsgodkjentTilsagnPeriodeSlutt,
            lastDayOfYear,
        ).min()

        val beregningInput = TilsagnBeregningInput.AFT(
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            antallPlasser = gjennomforing.antallPlasser,
        )
        val beregning = service.tilsagnBeregning(input = beregningInput).getOrNull()

        TilsagnDefaults(
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            antallPlasser = gjennomforing.antallPlasser,
            beregning = beregning,
            kostnadssted = null,
        )
    }

    else -> {
        val firstDayOfCurrentMonth = LocalDate.now().withDayOfMonth(1)
        val periodeStart = listOfNotNull(
            gjennomforing.startDato,
            tilsagn?.periodeSlutt?.plusDays(1),
            firstDayOfCurrentMonth,
        ).max()

        val lastDayOfMonth = periodeStart.with(TemporalAdjusters.lastDayOfMonth())
        val periodeSlutt = listOfNotNull(gjennomforing.sluttDato, lastDayOfMonth).min()

        TilsagnDefaults(
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            antallPlasser = gjennomforing.antallPlasser,
            kostnadssted = null,
            beregning = null,
        )
    }
}
