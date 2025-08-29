package no.nav.mulighetsrommet.api.tilsagn.api

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingDto
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Periode
import org.koin.ktor.ext.inject
import java.time.LocalDate.now
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.*

fun Route.tilsagnRoutesBeregning() {
    val db: ApiDatabase by inject()
    val service: TilsagnService by inject()
    val gjennomforinger: GjennomforingService by inject()

    get("/{id}/defaults") {
        val id: UUID by call.parameters

        val tilsagn = db.session { queries.tilsagn.get(id) }
            ?: return@get call.respond(HttpStatusCode.NotFound)

        val defaults = resolveTilsagnRequest(tilsagn)

        call.respond(defaults)
    }

    post("/defaults") {
        val request = call.receive<TilsagnRequest>()

        val gjennomforing = gjennomforinger.get(request.gjennomforingId)
            ?: return@post call.respond(HttpStatusCode.NotFound)

        val prismodell = gjennomforing.avtaleId?.let { db.session { queries.avtale.get(it) } }
            ?.prismodell
            ?: throw StatusException(
                HttpStatusCode.BadRequest,
                "Tilsagn kan ikke opprettes uten at avtalen har en prismodell",
            )

        val defaults = when (request.type) {
            TilsagnType.TILSAGN -> db.session {
                val sisteTilsagn = queries.tilsagn
                    .getAll(typer = listOf(TilsagnType.TILSAGN), gjennomforingId = request.gjennomforingId)
                    .firstOrNull()

                resolveTilsagnDefaults(service.config.okonomiConfig, prismodell, gjennomforing, sisteTilsagn)
            }

            TilsagnType.INVESTERING,
            TilsagnType.EKSTRATILSAGN,
            -> db.session {
                resolveEkstraTilsagnInvesteringDefaults(request, gjennomforing, prismodell)
            }
        }

        call.respond(HttpStatusCode.OK, defaults)
    }

    post("/beregn") {
        val request = call.receive<BeregnTilsagnRequest>()
        val beregning = service.beregnTilsagn(request)?.let {
            TilsagnBeregningDto.from(it)
        }
        call.respond(
            BeregnTilsagnResponse(
                success = beregning != null,
                beregning = beregning,
            ),
        )
    }
}

private fun resolveTilsagnRequest(tilsagn: Tilsagn): TilsagnRequest {
    return TilsagnRequest(
        id = tilsagn.id,
        type = tilsagn.type,
        gjennomforingId = tilsagn.gjennomforing.id,
        kostnadssted = tilsagn.kostnadssted.enhetsnummer,
        beregning = when (tilsagn.beregning) {
            is TilsagnBeregningFri -> TilsagnBeregningRequest(
                type = TilsagnBeregningType.FRI,
                linjer = tilsagn.beregning.input.linjer.map {
                    TilsagnInputLinjeRequest(
                        id = it.id,
                        beskrivelse = it.beskrivelse,
                        belop = it.belop,
                        antall = it.antall,
                    )
                },
                prisbetingelser = tilsagn.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> TilsagnBeregningRequest(
                type = TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED,
                antallPlasser = tilsagn.beregning.input.antallPlasser,
            )

            is TilsagnBeregningPrisPerManedsverk -> TilsagnBeregningRequest(
                type = TilsagnBeregningType.PRIS_PER_MANEDSVERK,
                antallPlasser = tilsagn.beregning.input.antallPlasser,
                prisbetingelser = tilsagn.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningPrisPerUkesverk -> TilsagnBeregningRequest(
                type = TilsagnBeregningType.PRIS_PER_UKESVERK,
                antallPlasser = tilsagn.beregning.input.antallPlasser,
                prisbetingelser = tilsagn.beregning.input.prisbetingelser,
            )

            is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningRequest(
                type = TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING,
                antallPlasser = tilsagn.beregning.input.antallPlasser,
                antallTimerOppfolgingPerDeltaker = tilsagn.beregning.input.antallTimerOppfolgingPerDeltaker,
                prisbetingelser = tilsagn.beregning.input.prisbetingelser,
            )
        },
        kommentar = tilsagn.kommentar,
        periodeStart = tilsagn.periode.start,
        periodeSlutt = tilsagn.periode.getLastInclusiveDate(),
    )
}

private fun resolveTilsagnDefaults(
    config: OkonomiConfig,
    prismodell: AvtaleDto.PrismodellDto,
    gjennomforing: GjennomforingDto,
    tilsagn: Tilsagn?,
): TilsagnRequest {
    val periode = when (prismodell) {
        is AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk ->
            getForhandsgodkjentTiltakPeriode(config, gjennomforing, tilsagn)

        else -> getAnskaffetTiltakPeriode(config, gjennomforing, tilsagn)
    }

    val (beregningType, prisbetingelser) = when (prismodell) {
        is AvtaleDto.PrismodellDto.AnnenAvtaltPris -> TilsagnBeregningType.FRI to prismodell.prisbetingelser
        is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> TilsagnBeregningType.PRIS_PER_MANEDSVERK to prismodell.prisbetingelser
        is AvtaleDto.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING to prismodell.prisbetingelser
        is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> TilsagnBeregningType.PRIS_PER_UKESVERK to prismodell.prisbetingelser
        AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED to null
    }

    val beregning = TilsagnBeregningRequest(
        type = beregningType,
        antallPlasser = gjennomforing.antallPlasser,
        prisbetingelser = prisbetingelser,
        linjer = listOf(TilsagnInputLinjeRequest(id = UUID.randomUUID(), beskrivelse = "", belop = 0, antall = 1)),
        antallTimerOppfolgingPerDeltaker = when (tilsagn?.beregning) {
            is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> tilsagn.beregning.input.antallTimerOppfolgingPerDeltaker
            else -> null
        },
    )

    return TilsagnRequest(
        id = UUID.randomUUID(),
        gjennomforingId = gjennomforing.id,
        type = TilsagnType.TILSAGN,
        periodeStart = periode.start,
        periodeSlutt = periode.getLastInclusiveDate(),
        kostnadssted = tilsagn?.kostnadssted?.enhetsnummer,
        beregning = beregning,
    )
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
    val periodeSlutt = listOfNotNull(gjennomforing.sluttDato, lastDayOfMonth)
        .filter { it > periodeStart }
        .min()

    return Periode.fromInclusiveDates(periodeStart, periodeSlutt)
}

private fun resolveEkstraTilsagnInvesteringDefaults(
    request: TilsagnRequest,
    gjennomforing: GjennomforingDto,
    prismodell: AvtaleDto.PrismodellDto,
): TilsagnRequest {
    val (beregningType, prisbetingelser) = when (prismodell) {
        is AvtaleDto.PrismodellDto.AnnenAvtaltPris -> TilsagnBeregningType.FRI to prismodell.prisbetingelser
        is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> TilsagnBeregningType.PRIS_PER_MANEDSVERK to prismodell.prisbetingelser
        is AvtaleDto.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING to prismodell.prisbetingelser
        is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> TilsagnBeregningType.PRIS_PER_UKESVERK to prismodell.prisbetingelser
        AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED to null
    }

    return TilsagnRequest(
        id = UUID.randomUUID(),
        gjennomforingId = gjennomforing.id,
        type = request.type,
        periodeStart = request.periodeStart,
        periodeSlutt = request.periodeSlutt,
        kostnadssted = request.kostnadssted,
        beregning = TilsagnBeregningRequest(
            type = beregningType,
            prisbetingelser = prisbetingelser,
            linjer = listOf(TilsagnInputLinjeRequest(id = UUID.randomUUID(), beskrivelse = "", belop = 0, antall = 1)),
            antallPlasser = 0,
        ),
    )
}
