package no.nav.mulighetsrommet.api.tilsagn.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.GjennomforingService
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.*
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.time.LocalDate.now
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.*

fun Route.tilsagnRoutesBeregning() {
    val db: ApiDatabase by inject()
    val service: TilsagnService by inject()
    val gjennomforinger: GjennomforingService by inject()

    get("/{id}/defaults", {
        description = "Hent standardverdier for tilsagn utledet fra gitt tilsagn"
        tags = setOf("Tilsagn")
        operationId = "getTilsagnRequest"
        request {
            pathParameterUuid("id")
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Standardverdier for tilsagn"
                body<TilsagnRequest>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val id: UUID by call.parameters

        val tilsagn = db.session { queries.tilsagn.get(id) }
            ?: return@get call.respond(HttpStatusCode.NotFound)

        val gjennomforing = gjennomforinger.get(tilsagn.gjennomforing.id)
            ?: return@get call.respond(HttpStatusCode.NotFound)

        val prismodell = gjennomforing.avtaleId?.let { db.session { queries.avtale.get(it) } }
            ?.prismodell
            ?: throw StatusException(
                HttpStatusCode.BadRequest,
                "Tilsagn kan ikke opprettes uten at avtalen har en prismodell",
            )

        val defaults = resolveTilsagnRequest(tilsagn, prismodell)

        call.respond(defaults)
    }

    post("/defaults", {
        description = "Hent standardverdier for tilsagn utledet av systemet"
        tags = setOf("Tilsagn")
        operationId = "getTilsagnDefaults"
        request {
            body<TilsagnRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Standardverdier for tilsagn"
                body<TilsagnRequest>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
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

    post("/beregn", {
        description = "Beregn tilsagn"
        tags = setOf("Tilsagn")
        operationId = "beregnTilsagn"
        request {
            body<BeregnTilsagnRequest>()
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Beregnet tilsagn"
                body<BeregnTilsagnResponse>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val request = call.receive<BeregnTilsagnRequest>()
        val beregning = service.beregnTilsagnUnvalidated(request)?.let {
            TilsagnBeregningDto.from(it)
        }
        call.respond(
            BeregnTilsagnResponse(
                beregning = beregning,
            ),
        )
    }
}

fun resolveTilsagnRequest(tilsagn: Tilsagn, prismodell: Prismodell): TilsagnRequest {
    val (beregningType, prisbetingelser) = resolveBeregningTypeAndPrisbetingelser(prismodell)

    val beregning = TilsagnBeregningRequest(
        type = beregningType,
        antallPlasser = when (tilsagn.beregning) {
            is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> tilsagn.beregning.input.antallPlasser
            is TilsagnBeregningPrisPerManedsverk -> tilsagn.beregning.input.antallPlasser
            is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> tilsagn.beregning.input.antallPlasser
            is TilsagnBeregningPrisPerUkesverk -> tilsagn.beregning.input.antallPlasser
            is TilsagnBeregningPrisPerHeleUkesverk -> tilsagn.beregning.input.antallPlasser
            is TilsagnBeregningFri -> null
        },
        prisbetingelser = prisbetingelser,
        linjer = when (tilsagn.beregning) {
            is TilsagnBeregningFri ->
                tilsagn.beregning.input.linjer.map {
                    TilsagnInputLinjeRequest(
                        id = it.id,
                        beskrivelse = it.beskrivelse,
                        belop = it.belop,
                        antall = it.antall,
                    )
                }

            else -> emptyList()
        },
        antallTimerOppfolgingPerDeltaker = when (tilsagn.beregning) {
            is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> tilsagn.beregning.input.antallTimerOppfolgingPerDeltaker
            else -> null
        },
    )

    return TilsagnRequest(
        id = tilsagn.id,
        type = tilsagn.type,
        gjennomforingId = tilsagn.gjennomforing.id,
        kostnadssted = tilsagn.kostnadssted.enhetsnummer,
        beregning = beregning,
        kommentar = tilsagn.kommentar,
        periodeStart = tilsagn.periode.start,
        periodeSlutt = tilsagn.periode.getLastInclusiveDate(),
    )
}

fun resolveTilsagnDefaults(
    config: OkonomiConfig,
    prismodell: Prismodell,
    gjennomforing: Gjennomforing,
    tilsagn: Tilsagn?,
): TilsagnRequest {
    val periode = when (prismodell) {
        is Prismodell.ForhandsgodkjentPrisPerManedsverk ->
            getForhandsgodkjentTiltakPeriode(config, gjennomforing, tilsagn)

        else -> getAnskaffetTiltakPeriode(config, gjennomforing, tilsagn)
    }

    val (beregningType, prisbetingelser) = resolveBeregningTypeAndPrisbetingelser(prismodell)

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
    gjennomforing: Gjennomforing,
    tilsagn: Tilsagn?,
): Periode {
    val periodeStart = listOfNotNull(
        config.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode]?.start,
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
    gjennomforing: Gjennomforing,
    tilsagn: Tilsagn?,
): Periode {
    val firstDayOfCurrentMonth = now().withDayOfMonth(1)
    val periodeStart = listOfNotNull(
        config.gyldigTilsagnPeriode[gjennomforing.tiltakstype.tiltakskode]?.start,
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
    gjennomforing: Gjennomforing,
    prismodell: Prismodell,
): TilsagnRequest {
    val (beregningType, prisbetingelser) = resolveBeregningTypeAndPrisbetingelser(prismodell)

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

private fun resolveBeregningTypeAndPrisbetingelser(
    prismodell: Prismodell,
): Pair<TilsagnBeregningType, String?> = when (prismodell) {
    is Prismodell.AnnenAvtaltPris -> TilsagnBeregningType.FRI to prismodell.prisbetingelser
    is Prismodell.AvtaltPrisPerManedsverk -> TilsagnBeregningType.PRIS_PER_MANEDSVERK to prismodell.prisbetingelser
    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> TilsagnBeregningType.PRIS_PER_TIME_OPPFOLGING to prismodell.prisbetingelser
    is Prismodell.AvtaltPrisPerUkesverk -> TilsagnBeregningType.PRIS_PER_UKESVERK to prismodell.prisbetingelser
    is Prismodell.ForhandsgodkjentPrisPerManedsverk -> TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED to null
}
