package no.nav.mulighetsrommet.api.tilsagn.api

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.AppConfig
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.gjennomforing.service.GjennomforingAvtaleService
import no.nav.mulighetsrommet.api.plugins.pathParameterUuid
import no.nav.mulighetsrommet.api.tilsagn.TilsagnService
import no.nav.mulighetsrommet.api.tilsagn.model.BeregnTilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.BeregnTilsagnResponse
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningType
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnInputLinjeRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.withValuta
import org.koin.ktor.ext.inject
import java.time.LocalDate.now
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.UUID

fun Route.tilsagnRoutesBeregning() {
    val config: AppConfig by inject()
    val db: ApiDatabase by inject()
    val service: TilsagnService by inject()
    val gjennomforinger: GjennomforingAvtaleService by inject()

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

        val prismodell = db.session { queries.gjennomforing.getPrismodell(tilsagn.gjennomforing.id) }
            ?: throw StatusException(
                HttpStatusCode.BadRequest,
                "Tilsagn kan ikke opprettes fordi gjennomføring mangler prismodell",
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

        // TODO: må også gjelde enkeltplasser
        val gjennomforing = gjennomforinger.getGjennomforingAvtale(request.gjennomforingId)
            ?: return@post call.respond(HttpStatusCode.NotFound)

        val defaults = when (request.type) {
            TilsagnType.TILSAGN -> db.session {
                val sisteTilsagn = queries.tilsagn
                    .getAll(typer = listOf(TilsagnType.TILSAGN), gjennomforingId = request.gjennomforingId)
                    .firstOrNull()

                resolveTilsagnDefaults(config.okonomi, gjennomforing, sisteTilsagn)
            }

            TilsagnType.INVESTERING, TilsagnType.EKSTRATILSAGN -> db.session {
                resolveEkstraTilsagnInvesteringDefaults(request, gjennomforing)
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
        val request = try {
            call.receive<BeregnTilsagnRequest>()
        } catch (_: Throwable) {
            call.respond(BeregnTilsagnResponse(beregning = null))
            return@post
        }
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
                        pris = it.pris,
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
        beskrivelse = tilsagn.beskrivelse,
        periodeStart = tilsagn.periode.start.toString(),
        periodeSlutt = tilsagn.periode.getLastInclusiveDate().toString(),
    )
}

fun resolveTilsagnDefaults(
    config: OkonomiConfig,
    gjennomforing: GjennomforingAvtale,
    tilsagn: Tilsagn?,
): TilsagnRequest {
    val periode = when (gjennomforing.prismodell) {
        is Prismodell.AnnenAvtaltPris -> null

        is Prismodell.ForhandsgodkjentPrisPerManedsverk,
        -> getForhandsgodkjentTiltakPeriode(config, gjennomforing, tilsagn)

        is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker,
        is Prismodell.AvtaltPrisPerUkesverk,
        is Prismodell.AvtaltPrisPerHeleUkesverk,
        is Prismodell.AvtaltPrisPerManedsverk,
        -> getAnskaffetTiltakPeriode(config, gjennomforing, tilsagn)
    }

    val (beregningType, prisbetingelser) = resolveBeregningTypeAndPrisbetingelser(gjennomforing.prismodell)
    val valuta = gjennomforing.prismodell.valuta

    val beregning = TilsagnBeregningRequest(
        type = beregningType,
        antallPlasser = gjennomforing.antallPlasser,
        prisbetingelser = prisbetingelser,
        valuta = valuta,
        linjer = listOf(
            TilsagnInputLinjeRequest(
                id = UUID.randomUUID(),
                beskrivelse = "",
                pris = 0.withValuta(valuta),
                antall = 1,
            ),
        ),
        antallTimerOppfolgingPerDeltaker = when (tilsagn?.beregning) {
            is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> tilsagn.beregning.input.antallTimerOppfolgingPerDeltaker
            else -> null
        },
    )

    return TilsagnRequest(
        id = UUID.randomUUID(),
        gjennomforingId = gjennomforing.id,
        type = TilsagnType.TILSAGN,
        periodeStart = periode?.start?.toString(),
        periodeSlutt = periode?.getLastInclusiveDate()?.toString(),
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
    gjennomforing: GjennomforingAvtale,
): TilsagnRequest {
    val (beregningType, prisbetingelser) = resolveBeregningTypeAndPrisbetingelser(gjennomforing.prismodell)
    val valuta = gjennomforing.prismodell.valuta
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
            linjer = listOf(
                TilsagnInputLinjeRequest(
                    id = UUID.randomUUID(),
                    beskrivelse = "",
                    pris = 0.withValuta(valuta),
                    antall = 1,
                ),
            ),
            antallPlasser = 0,
            valuta = valuta,
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
    is Prismodell.AvtaltPrisPerHeleUkesverk -> TilsagnBeregningType.PRIS_PER_HELE_UKESVERK to prismodell.prisbetingelser
    is Prismodell.ForhandsgodkjentPrisPerManedsverk -> TilsagnBeregningType.FAST_SATS_PER_TILTAKSPLASS_PER_MANED to null
}
