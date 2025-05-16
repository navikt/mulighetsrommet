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
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.totrinnskontroll.service.TotrinnskontrollService
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Prismodell
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

fun Route.tilsagnRoutes() {
    val db: ApiDatabase by inject()
    val service: TilsagnService by inject()
    val gjennomforinger: GjennomforingService by inject()
    val totrinnskontrollService: TotrinnskontrollService by inject()

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
            authorize(anyOf = setOf(Rolle.SAKSBEHANDLER_OKONOMI, Rolle.ATTESTANT_UTBETALING, Rolle.BESLUTTER_TILSAGN)) {
                get {
                    val id = call.parameters.getOrFail<UUID>("id")
                    val navIdent = getNavIdent()

                    val result = db.session {
                        val tilsagn = queries.tilsagn.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                        val ansatt = queries.ansatt.getByNavIdent(navIdent)
                            ?: throw IllegalStateException("Fant ikke ansatt med navIdent $navIdent")

                        val kostnadssted = tilsagn.kostnadssted.enhetsnummer

                        val opprettelse = queries.totrinnskontroll.getOrError(id, Totrinnskontroll.Type.OPPRETT).let {
                            getTotrinnskontrollForAnsatt(it, kostnadssted, ansatt, totrinnskontrollService)
                        }
                        val annullering = queries.totrinnskontroll.get(id, Totrinnskontroll.Type.ANNULLER)?.let {
                            getTotrinnskontrollForAnsatt(it, kostnadssted, ansatt, totrinnskontrollService)
                        }
                        val tilOppgjor = queries.totrinnskontroll.get(id, Totrinnskontroll.Type.GJOR_OPP)?.let {
                            getTotrinnskontrollForAnsatt(it, kostnadssted, ansatt, totrinnskontrollService)
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
                    val prismodell = gjennomforing.avtaleId
                        ?.let { db.session { queries.avtale.get(it)?.prismodell } }
                        ?: throw StatusException(
                            HttpStatusCode.BadRequest,
                            "Tilsagn kan ikke opprettes uten at avtalen har en prismodell",
                        )

                    val sisteTilsagn = db.session {
                        queries.tilsagn
                            .getAll(typer = listOf(TilsagnType.TILSAGN), gjennomforingId = request.gjennomforingId)
                            .firstOrNull()
                    }

                    resolveTilsagnDefaults(service.config.okonomiConfig, prismodell, gjennomforing, sisteTilsagn)
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
            val result = service.beregnTilsagn(request)
                .map { it.output }
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
                call.respondWithStatusResponse(service.slettTilsagn(id, navIdent))
            }
        }

        authorize(Rolle.BESLUTTER_TILSAGN) {
            post("/{id}/beslutt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<BesluttTilsagnRequest>()
                val navIdent = getNavIdent()

                call.respondWithStatusResponse(service.beslutt(id, request, navIdent))
            }
        }
    }

    get("/prismodell/satser") {
        val tiltakstype: Tiltakskode by call.queryParameters

        val satser = ForhandsgodkjenteSatser.satser(tiltakstype).map {
            AvtaltSats(
                periodeStart = it.periode.start,
                periodeSlutt = it.periode.getLastInclusiveDate(),
                pris = it.belop,
                valuta = "NOK",
            )
        }

        if (satser.isEmpty()) {
            return@get call.respond(HttpStatusCode.BadRequest, "Det finnes ingen avtalte satser for $tiltakstype")
        }

        call.respond(satser)
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
    data object GodkjentTilsagnRequest : BesluttTilsagnRequest(
        besluttelse = Besluttelse.GODKJENT,
    )

    @Serializable
    @SerialName("AVVIST")
    data class AvvistTilsagnRequest(
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
    config: OkonomiConfig,
    prismodell: Prismodell,
    gjennomforing: GjennomforingDto,
    tilsagn: Tilsagn?,
) = when (prismodell) {
    Prismodell.FORHANDSGODKJENT -> {
        val periodeStart = listOfNotNull(
            config.minimumTilsagnPeriodeStart[gjennomforing.tiltakstype.tiltakskode],
            gjennomforing.startDato,
            tilsagn?.periode?.slutt,
        ).max()

        val forhandsgodkjentTilsagnPeriodeSlutt = periodeStart.plusMonths(6).minusDays(1)
        val lastDayOfYear = periodeStart.withMonth(12).withDayOfMonth(31)
        val periodeSlutt = listOfNotNull(
            gjennomforing.sluttDato,
            forhandsgodkjentTilsagnPeriodeSlutt,
            lastDayOfYear,
        )
            .filter { it > periodeStart }
            .min()

        val periode = Periode.fromInclusiveDates(periodeStart, periodeSlutt)
        val beregning = ForhandsgodkjenteSatser.findSats(gjennomforing.tiltakstype.tiltakskode, periode)
            ?.let { sats ->
                TilsagnBeregningForhandsgodkjent.Input(
                    periode = periode,
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
            kostnadssted = tilsagn?.kostnadssted?.enhetsnummer,
            beregning = beregning,
        )
    }

    else -> {
        val firstDayOfCurrentMonth = LocalDate.now().withDayOfMonth(1)
        val periodeStart = listOfNotNull(
            config.minimumTilsagnPeriodeStart[gjennomforing.tiltakstype.tiltakskode],
            gjennomforing.startDato,
            tilsagn?.periode?.slutt,
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

private fun resolveEkstraTilsagnDefaults(
    request: TilsagnDefaultsRequest,
    gjennomforing: GjennomforingDto,
    prisbetingelser: String?,
): TilsagnDefaults {
    return if (request.prismodell == Prismodell.FRI && request.belop != null) {
        TilsagnDefaults(
            id = null,
            gjennomforingId = gjennomforing.id,
            type = TilsagnType.EKSTRATILSAGN,
            periodeStart = request.periodeStart,
            periodeSlutt = request.periodeSlutt,
            kostnadssted = request.kostnadssted,
            beregning = TilsagnBeregningFri.Input(prisbetingelser = prisbetingelser, belop = request.belop),
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

private fun getTotrinnskontrollForAnsatt(
    totrinnskontroll: Totrinnskontroll,
    kostnadssted: NavEnhetNummer,
    ansatt: NavAnsatt,
    totrinnskontrollService: TotrinnskontrollService,
): TotrinnskontrollDto {
    val kanBesluttesAvAnsatt = ansatt.hasKontorspesifikkRolle(Rolle.BESLUTTER_TILSAGN, setOf(kostnadssted))
    var besluttetAvNavn = totrinnskontrollService.getBesluttetAvNavn(totrinnskontroll)
    var behandletAvNavn = totrinnskontrollService.getBehandletAvNavn(totrinnskontroll)
    return TotrinnskontrollDto.fromTotrinnskontroll(
        totrinnskontroll,
        kanBesluttesAvAnsatt,
        behandletAvNavn,
        besluttetAvNavn,
    )
}
