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
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.gjennomforing.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.okonomi.ForhandsgodkjentSats
import no.nav.mulighetsrommet.api.okonomi.Prismodell
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.BadRequest
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnRepository
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

fun Route.tilsagnRoutes() {
    val service: TilsagnService by inject()
    val tilsagn: TilsagnRepository by inject()
    val gjennomforinger: TiltaksgjennomforingService by inject()
    val avtaler: AvtaleRepository by inject()

    route("tilsagn") {
        get {
            val gjennomforingId: UUID? by call.queryParameters
            val status = call.queryParameters.getAll("statuser")
                ?.map { TilsagnStatus.valueOf(it) }

            val result = tilsagn.getAll(gjennomforingId = gjennomforingId, statuser = status)

            call.respond(result)
        }

        route("/{id}") {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val result = tilsagn.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(result)
            }

            get("/historikk") {
                val id = call.parameters.getOrFail<UUID>("id")
                val historikk = service.getEndringshistorikk(id)
                call.respond(historikk)
            }
        }

        get("/defaults") {
            val gjennomforingId: UUID by call.queryParameters
            val type: TilsagnType by call.queryParameters

            val gjennomforing = gjennomforinger.get(gjennomforingId) ?: return@get call.respond(HttpStatusCode.NotFound)

            val defaults = when (type) {
                TilsagnType.TILSAGN -> {
                    val sisteTilsagn = tilsagn
                        .getAll(type = TilsagnType.TILSAGN, gjennomforingId = gjennomforingId)
                        .firstOrNull()
                    resolveTilsagnDefaults(gjennomforing, sisteTilsagn)
                }

                TilsagnType.EKSTRATILSAGN -> TilsagnDefaults(
                    id = null,
                    gjennomforingId = gjennomforing.id,
                    type = TilsagnType.EKSTRATILSAGN,
                    periodeStart = null,
                    periodeSlutt = null,
                    kostnadssted = null,
                    beregning = null,
                )
            }

            call.respond(HttpStatusCode.OK, defaults)
        }

        post("/beregn") {
            val request = call.receive<TilsagnBeregningInput>()

            val result = service.tilsagnBeregning(request)
                .map { it.output }
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
    }

    get("/prismodell/satser") {
        val avtaleId: UUID by call.queryParameters

        val avtale = avtaler.get(avtaleId)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Fant ikke avtale=$avtaleId")

        fun toAvtaltSats(it: ForhandsgodkjentSats) = AvtaltSats(
            periodeStart = it.periode.start,
            periodeSlutt = it.periode.getLastDate(),
            pris = it.belop,
            valuta = "NOK",
        )

        val satser = when (avtale.tiltakstype.tiltakskode) {
            Tiltakskode.ARBEIDSFORBEREDENDE_TRENING -> Prismodell.AFT.satser.map(::toAvtaltSats)

            Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET -> Prismodell.VTA.satser.map(::toAvtaltSats)

            else -> return@get call.respond(
                HttpStatusCode.BadRequest,
                "Det finnes ingen avtalte satser for avtale=$avtaleId",
            )
        }

        call.respond(satser)
    }
}

@Serializable
data class TilsagnDefaults(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID?,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID?,
    val type: TilsagnType?,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate?,
    val kostnadssted: String?,
    val beregning: TilsagnBeregningInput?,
)

// TODO: benytt TilsagnDefaults (modell med bare nullable) i begge tilfeller og valider at feltene ikke er null i stedet. Da kan vi gjøre all validering i backend!
@Serializable
data class TilsagnRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val type: TilsagnType,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val kostnadssted: String,
    val beregning: TilsagnBeregningInput,
)

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
        val aarsaker: List<TilsagnStatusAarsak>,
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
data class AvtaltSats(
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val pris: Int,
    val valuta: String,
)

private fun resolveTilsagnDefaults(
    gjennomforing: TiltaksgjennomforingDto,
    tilsagn: TilsagnDto?,
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

        val beregning = Prismodell.AFT.findSats(periodeStart)?.let { sats ->
            TilsagnBeregningAft.Input(
                periodeStart = periodeStart,
                periodeSlutt = periodeSlutt,
                sats = sats,
                antallPlasser = gjennomforing.antallPlasser,
            )
        }

        TilsagnDefaults(
            id = null,
            gjennomforingId = gjennomforing.id,
            type = TilsagnType.TILSAGN,
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            kostnadssted = null,
            beregning = beregning,
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
            id = null,
            gjennomforingId = gjennomforing.id,
            type = TilsagnType.TILSAGN,
            periodeStart = periodeStart,
            periodeSlutt = periodeSlutt,
            kostnadssted = null,
            beregning = null,
        )
    }
}
