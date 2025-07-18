package no.nav.mulighetsrommet.api.tilsagn.api

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.navansatt.ktor.authorize
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.api.totrinnskontroll.api.toDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.*

fun Route.tilsagnRoutes() {
    val db: ApiDatabase by inject()
    val service: TilsagnService by inject()
    val gjennomforinger: GjennomforingService by inject()

    route("tilsagn") {
        get {
            val gjennomforingId: UUID? by call.queryParameters
            val status = call.queryParameters.getAll("statuser")
                ?.map { TilsagnStatus.valueOf(it) }

            val result = db.session {
                queries.tilsagn
                    .getAll(gjennomforingId = gjennomforingId, statuser = status)
                    .map { TilsagnDto.fromTilsagn(it) }
            }

            call.respond(result)
        }

        route("/{id}") {
            authorize(anyOf = setOf(Rolle.SAKSBEHANDLER_OKONOMI, Rolle.BESLUTTER_TILSAGN)) {
                get {
                    val id = call.parameters.getOrFail<UUID>("id")
                    val navIdent = getNavIdent()

                    val result = db.session {
                        val tilsagn = queries.tilsagn.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                        val ansatt = queries.ansatt.getByNavIdent(navIdent)
                            ?: throw IllegalStateException("Fant ikke ansatt med navIdent $navIdent")

                        val kostnadssted = tilsagn.kostnadssted.enhetsnummer

                        val opprettelse = queries.totrinnskontroll.getOrError(id, Totrinnskontroll.Type.OPPRETT).let {
                            it.toDto(kanBeslutteTilsagn(it, ansatt, kostnadssted))
                        }
                        val annullering = queries.totrinnskontroll.get(id, Totrinnskontroll.Type.ANNULLER)?.let {
                            it.toDto(kanBeslutteTilsagn(it, ansatt, kostnadssted))
                        }
                        val tilOppgjor = queries.totrinnskontroll.get(id, Totrinnskontroll.Type.GJOR_OPP)?.let {
                            it.toDto(kanBeslutteTilsagn(it, ansatt, kostnadssted))
                        }

                        TilsagnDetaljerDto(
                            tilsagn = TilsagnDto.fromTilsagn(tilsagn),
                            opprettelse = opprettelse,
                            annullering = annullering,
                            tilOppgjor = tilOppgjor,
                        )
                    }

                    call.respond(result)
                }
            }
            get("/historikk") {
                val id = call.parameters.getOrFail<UUID>("id")
                val historikk = service.getEndringshistorikk(id)
                call.respond(historikk)
            }
        }

        post("/defaults") {
            val request = call.receive<TilsagnDefaultsRequest>()

            val gjennomforing = gjennomforinger.get(request.gjennomforingId)
                ?: return@post call.respond(HttpStatusCode.NotFound)

            val defaults = when (request.type) {
                TilsagnType.TILSAGN -> {
                    val avtale = gjennomforing.avtaleId
                        ?.let { db.session { queries.avtale.get(it) } }

                    val sisteTilsagn = db.session {
                        queries.tilsagn
                            .getAll(typer = listOf(TilsagnType.TILSAGN), gjennomforingId = request.gjennomforingId)
                            .firstOrNull()
                    }

                    resolveTilsagnDefaults(service.config.okonomiConfig, avtale, gjennomforing, sisteTilsagn)
                }

                TilsagnType.EKSTRATILSAGN -> {
                    val prisbetingelser = gjennomforing.avtaleId
                        ?.let { db.session { queries.avtale.get(it)?.prisbetingelser } }

                    resolveEkstraTilsagnDefaults(request, gjennomforing, prisbetingelser)
                }

                TilsagnType.INVESTERING -> TilsagnDefaults(
                    id = null,
                    gjennomforingId = gjennomforing.id,
                    type = TilsagnType.INVESTERING,
                    periodeStart = request.periodeStart,
                    periodeSlutt = request.periodeSlutt,
                    kostnadssted = request.kostnadssted,
                    beregning = null,
                )
            }

            call.respond(HttpStatusCode.OK, defaults)
        }

        post("/beregn") {
            val request = call.receive<TilsagnBeregningInput>()
            val result = service.beregnTilsagn(request)
                .map { TilsagnBeregningDto.from(it) }
                .mapLeft { ValidationError(errors = it) }

            call.respondWithStatusResponse(result)
        }

        authorize(Rolle.SAKSBEHANDLER_OKONOMI) {
            put {
                val request = call.receive<TilsagnRequest>()
                val navIdent = getNavIdent()

                val result = service.upsert(request, navIdent)
                    .mapLeft { ValidationError(errors = it) }

                call.respondWithStatusResponse(result)
            }

            post("/{id}/til-annullering") {
                val request = call.receive<TilAnnulleringRequest>()
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                service.tilAnnulleringRequest(id, navIdent, request)

                call.respond(HttpStatusCode.OK)
            }

            post("/{id}/gjor-opp") {
                val request = call.receive<TilAnnulleringRequest>()
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                service.tilGjorOppRequest(id, navIdent, request)

                call.respond(HttpStatusCode.OK)
            }

            delete("/{id}") {
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                val result = service.slettTilsagn(id, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }

        authorize(Rolle.BESLUTTER_TILSAGN) {
            post("/{id}/beslutt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<BesluttTilsagnRequest>()
                val navIdent = getNavIdent()

                val result = service.beslutt(id, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                    .map { HttpStatusCode.OK }

                call.respondWithStatusResponse(result)
            }
        }
    }
}

@Serializable
data class TilsagnDefaultsRequest(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val type: TilsagnType,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate?,
    val kostnadssted: NavEnhetNummer?,
    val belop: Int?,
    val prismodell: Prismodell?,
)

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
    val kostnadssted: NavEnhetNummer?,
    val beregning: TilsagnBeregningInput?,
)

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
    val kostnadssted: NavEnhetNummer,
    val beregning: TilsagnBeregningInput,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("besluttelse")
sealed class BesluttTilsagnRequest(
    val besluttelse: Besluttelse,
) {
    @Serializable
    @SerialName("GODKJENT")
    data object Godkjent : BesluttTilsagnRequest(
        besluttelse = Besluttelse.GODKJENT,
    )

    @Serializable
    @SerialName("AVVIST")
    data class Avvist(
        val aarsaker: List<TilsagnStatusAarsak>,
        val forklaring: String?,
    ) : BesluttTilsagnRequest(
        besluttelse = Besluttelse.AVVIST,
    )
}

@Serializable
data class TilAnnulleringRequest(
    val aarsaker: List<TilsagnStatusAarsak>,
    val forklaring: String?,
)

private fun resolveTilsagnDefaults(
    config: OkonomiConfig,
    avtale: AvtaleDto?,
    gjennomforing: GjennomforingDto,
    tilsagn: Tilsagn?,
): TilsagnDefaults {
    val prismodell = avtale?.prismodell ?: throw StatusException(
        HttpStatusCode.BadRequest,
        "Tilsagn kan ikke opprettes uten at avtalen har en prismodell",
    )
    return when (prismodell) {
        Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK -> {
            val periode = getForhandsgodkjentTiltakPeriode(config, gjennomforing, tilsagn)
            getTilsagnBeregningPrisPerManedsverkDefaults(periode, avtale, gjennomforing, tilsagn)
        }

        Prismodell.AVTALT_PRIS_PER_MANEDSVERK -> {
            val periode = getAnskaffetTiltakPeriode(config, gjennomforing, tilsagn)
            getTilsagnBeregningPrisPerManedsverkDefaults(periode, avtale, gjennomforing, tilsagn)
        }

        Prismodell.AVTALT_PRIS_PER_UKESVERK -> {
            val periode = getAnskaffetTiltakPeriode(config, gjennomforing, tilsagn)
            getTilsagnBeregningPrisPerUkesverkDefaults(periode, avtale, gjennomforing, tilsagn)
        }

        Prismodell.ANNEN_AVTALT_PRIS -> {
            val periode = getAnskaffetTiltakPeriode(config, gjennomforing, tilsagn)
            getTilsagnBeregningFriDefaults(periode, avtale, gjennomforing)
        }
    }
}

private fun getForhandsgodkjentTiltakPeriode(
    config: OkonomiConfig,
    gjennomforing: GjennomforingDto,
    tilsagn: Tilsagn?,
): Periode {
    val periodeStart = listOfNotNull(
        config.minimumTilsagnPeriodeStart[gjennomforing.tiltakstype.tiltakskode],
        gjennomforing.startDato,
        tilsagn?.periode?.slutt,
    ).max()

    val defaultForhandsgodkjentTilsagnPeriodeSlutt = periodeStart.plusMonths(6).minusDays(1)
    val lastDayOfYear = periodeStart.withMonth(12).withDayOfMonth(31)
    val periodeSlutt = listOfNotNull(
        gjennomforing.sluttDato,
        defaultForhandsgodkjentTilsagnPeriodeSlutt,
        lastDayOfYear,
    ).filter { it > periodeStart }.min()

    return Periode.fromInclusiveDates(periodeStart, periodeSlutt)
}

private fun getAnskaffetTiltakPeriode(
    config: OkonomiConfig,
    gjennomforing: GjennomforingDto,
    tilsagn: Tilsagn?,
): Periode {
    val firstDayOfCurrentMonth = now().withDayOfMonth(1)
    val periodeStart = listOfNotNull(
        config.minimumTilsagnPeriodeStart[gjennomforing.tiltakstype.tiltakskode],
        gjennomforing.startDato,
        tilsagn?.periode?.slutt,
        firstDayOfCurrentMonth,
    ).max()

    val lastDayOfMonth = periodeStart.with(lastDayOfMonth())
    val periodeSlutt = listOfNotNull(gjennomforing.sluttDato, lastDayOfMonth).min()

    return Periode.fromInclusiveDates(periodeStart, periodeSlutt)
}

private fun getTilsagnBeregningPrisPerManedsverkDefaults(
    periode: Periode,
    avtale: AvtaleDto,
    gjennomforing: GjennomforingDto,
    tilsagn: Tilsagn?,
): TilsagnDefaults {
    val beregning = AvtalteSatser.findSats(avtale, periode)?.let { sats ->
        TilsagnBeregningPrisPerManedsverk.Input(
            periode = periode,
            sats = sats,
            antallPlasser = gjennomforing.antallPlasser,
        )
    }

    return TilsagnDefaults(
        id = null,
        gjennomforingId = gjennomforing.id,
        type = TilsagnType.TILSAGN,
        periodeStart = periode.start,
        periodeSlutt = periode.getLastInclusiveDate(),
        kostnadssted = tilsagn?.kostnadssted?.enhetsnummer,
        beregning = beregning,
    )
}

private fun getTilsagnBeregningPrisPerUkesverkDefaults(
    periode: Periode,
    avtale: AvtaleDto,
    gjennomforing: GjennomforingDto,
    tilsagn: Tilsagn?,
): TilsagnDefaults {
    val beregning = AvtalteSatser.findSats(avtale, periode)?.let { sats ->
        TilsagnBeregningPrisPerUkesverk.Input(
            periode = periode,
            sats = sats,
            antallPlasser = gjennomforing.antallPlasser,
        )
    }

    return TilsagnDefaults(
        id = null,
        gjennomforingId = gjennomforing.id,
        type = TilsagnType.TILSAGN,
        periodeStart = periode.start,
        periodeSlutt = periode.getLastInclusiveDate(),
        kostnadssted = tilsagn?.kostnadssted?.enhetsnummer,
        beregning = beregning,
    )
}

private fun getTilsagnBeregningFriDefaults(
    periode: Periode,
    avtale: AvtaleDto,
    gjennomforing: GjennomforingDto,
): TilsagnDefaults {
    val beregning = TilsagnBeregningFri.Input(
        linjer = listOf(
            TilsagnBeregningFri.InputLinje(id = UUID.randomUUID(), beskrivelse = "", belop = 0, antall = 1),
        ),
        prisbetingelser = avtale.prisbetingelser,
    )

    return TilsagnDefaults(
        id = null,
        gjennomforingId = gjennomforing.id,
        type = TilsagnType.TILSAGN,
        periodeStart = periode.start,
        periodeSlutt = periode.getLastInclusiveDate(),
        kostnadssted = null,
        beregning = beregning,
    )
}

private fun resolveEkstraTilsagnDefaults(
    request: TilsagnDefaultsRequest,
    gjennomforing: GjennomforingDto,
    prisbetingelser: String?,
): TilsagnDefaults {
    return if (request.prismodell == Prismodell.ANNEN_AVTALT_PRIS) {
        TilsagnDefaults(
            id = null,
            gjennomforingId = gjennomforing.id,
            type = TilsagnType.EKSTRATILSAGN,
            periodeStart = request.periodeStart,
            periodeSlutt = request.periodeSlutt,
            kostnadssted = request.kostnadssted,
            beregning = TilsagnBeregningFri.Input(
                prisbetingelser = prisbetingelser,
                linjer = listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "",
                        belop = request.belop ?: 0,
                        antall = 1,
                    ),
                ),
            ),
        )
    } else {
        TilsagnDefaults(
            id = null,
            gjennomforingId = gjennomforing.id,
            type = TilsagnType.EKSTRATILSAGN,
            periodeStart = request.periodeStart,
            periodeSlutt = request.periodeSlutt,
            kostnadssted = request.kostnadssted,
            beregning = null,
        )
    }
}

fun kanBeslutteTilsagn(totrinnskontroll: Totrinnskontroll, ansatt: NavAnsatt, kostnadssted: NavEnhetNummer): Boolean {
    return totrinnskontroll.behandletAv != ansatt.navIdent &&
        ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(kostnadssted))
}
