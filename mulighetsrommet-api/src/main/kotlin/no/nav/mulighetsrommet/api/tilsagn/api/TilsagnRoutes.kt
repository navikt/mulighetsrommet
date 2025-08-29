package no.nav.mulighetsrommet.api.tilsagn.api

import arrow.core.flatMap
import arrow.core.right
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.OkonomiConfig
import no.nav.mulighetsrommet.api.aarsakerforklaring.AarsakerOgForklaringRequest
import no.nav.mulighetsrommet.api.aarsakerforklaring.validateAarsakerOgForklaring
import no.nav.mulighetsrommet.api.avtale.mapper.prisbetingelser
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
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
import no.nav.mulighetsrommet.api.utbetaling.api.BesluttTotrinnskontrollRequest
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import org.koin.ktor.ext.inject
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
            authorize(anyOf = setOf(Rolle.OKONOMI_LES, Rolle.SAKSBEHANDLER_OKONOMI, Rolle.BESLUTTER_TILSAGN)) {
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
            val request = call.receive<TilsagnRequest>()

            val gjennomforing = gjennomforinger.get(request.gjennomforingId)
                ?: return@post call.respond(HttpStatusCode.NotFound)

            val prismodell = gjennomforing.avtaleId?.let {
                db.session { queries.avtale.get(it) }
            }?.prismodell
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
            call.respond(
                TilsagnBeregningDto.from(service.beregnTilsagn(request)),
            )
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
                val request = call.receive<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
                    .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                    .onRight {
                        service.tilAnnulleringRequest(id, navIdent, request)
                        call.respond(HttpStatusCode.OK)
                    }
            }

            post("/{id}/gjor-opp") {
                val request = call.receive<AarsakerOgForklaringRequest<TilsagnStatusAarsak>>()
                val id = call.parameters.getOrFail<UUID>("id")
                val navIdent = getNavIdent()

                validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
                    .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                    .onRight {
                        service.tilGjorOppRequest(id, navIdent, request)
                        call.respond(HttpStatusCode.OK)
                    }
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
                val request = call.receive<BesluttTotrinnskontrollRequest<TilsagnStatusAarsak>>()
                val navIdent = getNavIdent()

                when (request.besluttelse) {
                    Besluttelse.GODKJENT -> Unit.right()
                    Besluttelse.AVVIST -> validateAarsakerOgForklaring(request.aarsaker, request.forklaring)
                }
                    .flatMap {
                        service.beslutt(id, request, navIdent)
                    }
                    .onLeft { call.respondWithProblemDetail(ValidationError(errors = it)) }
                    .onRight { call.respond(HttpStatusCode.OK) }
            }
        }
    }
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
            antallPlasser = gjennomforing.antallPlasser,
        ),
    )
}

fun kanBeslutteTilsagn(totrinnskontroll: Totrinnskontroll, ansatt: NavAnsatt, kostnadssted: NavEnhetNummer): Boolean {
    return totrinnskontroll.behandletAv != ansatt.navIdent &&
        ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(kostnadssted))
}
