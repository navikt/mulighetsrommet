package no.nav.mulighetsrommet.api.utbetaling.api

import arrow.core.left
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
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Besluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingService
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingValidator
import no.nav.mulighetsrommet.api.utbetaling.api.DeltakerForKostnadsfordeling.Companion.toDeltakereForKostnadsfordeling
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningForhandsgodkjent
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.utbetalingRoutes() {
    val db: ApiDatabase by inject()
    val service: UtbetalingService by inject()

    route("/utbetaling/{id}") {
        get {
            val id = call.parameters.getOrFail<UUID>("id")

            val navIdent = getNavIdent()

            val utbetaling = db.session {
                val ansatt = queries.ansatt.getByNavIdent(navIdent)
                    ?: throw IllegalStateException("Fant ikke ansatt med navIdent $navIdent")

                val utbetaling = queries.utbetaling.get(id)
                    ?: throw NoSuchElementException("Utbetaling id=$id finnes ikke")

                val linjer = queries.delutbetaling.getByUtbetalingId(utbetaling.id).map { delutbetaling ->
                    val tilsagnBesluttetAv = queries.totrinnskontroll
                        .getOrError(delutbetaling.tilsagnId, Totrinnskontroll.Type.OPPRETT)
                        .besluttetAv

                    val opprettelse = queries.totrinnskontroll
                        .getOrError(delutbetaling.id, Totrinnskontroll.Type.OPPRETT)

                    val kanBesluttesAvAnsatt = tilsagnBesluttetAv != ansatt.navIdent &&
                        opprettelse.behandletAv != ansatt.navIdent &&
                        NavAnsattRolle.ATTESTANT_UTBETALING in ansatt.roller

                    UtbetalingLinje(
                        id = delutbetaling.id,
                        gjorOppTilsagn = delutbetaling.gjorOppTilsagn,
                        belop = delutbetaling.belop,
                        status = delutbetaling.status,
                        tilsagn = checkNotNull(queries.tilsagn.get(delutbetaling.tilsagnId)).let {
                            TilsagnDto.fromTilsagn(
                                it,
                            )
                        },
                        opprettelse = TotrinnskontrollDto.fromTotrinnskontroll(opprettelse, kanBesluttesAvAnsatt),
                    )
                }

                val deltakere = when (utbetaling.beregning) {
                    is UtbetalingBeregningForhandsgodkjent -> queries.deltaker.getAll(gjennomforingId = utbetaling.gjennomforing.id)
                        .toDeltakereForKostnadsfordeling(utbetaling.beregning.output.deltakelser.associateBy { it.deltakelseId })

                    is UtbetalingBeregningFri -> emptyList()
                }

                UtbetalingDetaljerDto(
                    utbetaling = toUtbetalingDto(utbetaling),
                    deltakere = deltakere,
                    linjer = linjer,
                )
            }

            call.respond(utbetaling)
        }

        get("/delutbetalinger") {
            val id = call.parameters.getOrFail<UUID>("id")

            val delutbetalinger = db.session {
                queries.delutbetaling.getByUtbetalingId(id)
            }

            call.respond(delutbetalinger)
        }

        get("/historikk") {
            val id = call.parameters.getOrFail<UUID>("id")
            val historikk = db.session {
                queries.endringshistorikk.getEndringshistorikk(DocumentClass.UTBETALING, id)
            }
            call.respond(historikk)
        }

        get("/tilsagn") {
            val id = call.parameters.getOrFail<UUID>("id")

            val tilsagn = db.session {
                val utbetaling = queries.utbetaling.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)

                queries.tilsagn.getAll(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periodeIntersectsWith = utbetaling.periode,
                    typer = listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN),
                ).map { TilsagnDto.fromTilsagn(it) }
            }

            call.respond(tilsagn)
        }

        authenticate(AuthProvider.AZURE_AD_SAKSBEHANDLER_OKONOMI) {
            post("/opprett-utbetaling") {
                val utbetalingId = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<OpprettManuellUtbetalingRequest>()
                val navIdent = getNavIdent()

                UtbetalingValidator.validateManuellUtbetalingskrav(request)
                    .onLeft {
                        return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                    }

                service.opprettManuellUtbetaling(utbetalingId, request, navIdent)

                call.respond(request)
            }
        }
    }

    route("/delutbetalinger") {
        authenticate(AuthProvider.AZURE_AD_SAKSBEHANDLER_OKONOMI) {
            put {
                val request = call.receive<OpprettDelutbetalingerRequest>()
                val navIdent = getNavIdent()

                val result = service.opprettDelutbetalinger(request, navIdent)
                call.respondWithStatusResponse(result)
            }
        }

        authenticate(AuthProvider.AZURE_AD_ATTESTANT_UTBETALING) {
            post("/{id}/beslutt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<BesluttDelutbetalingRequest>()
                val navIdent = getNavIdent()

                service.besluttDelutbetaling(id, request, navIdent)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/gjennomforinger/{id}/utbetalinger") {
        get {
            val id = call.parameters.getOrFail<UUID>("id")

            val utbetalinger = db.session {
                queries.utbetaling.getByGjennomforing(id)
                    .map { utbetaling -> toUtbetalingDto(utbetaling) }
            }

            call.respond(utbetalinger)
        }
    }
}

private fun QueryContext.toUtbetalingDto(utbetaling: Utbetaling): UtbetalingDto {
    val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)
    val status = AdminUtbetalingStatus.fromUtbetaling(utbetaling, delutbetalinger)

    return UtbetalingDto.fromUtbetaling(utbetaling, status)
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("besluttelse")
sealed class BesluttDelutbetalingRequest(
    val besluttelse: Besluttelse,
) {
    @Serializable
    @SerialName("GODKJENT")
    data object GodkjentDelutbetalingRequest : BesluttDelutbetalingRequest(
        besluttelse = Besluttelse.GODKJENT,
    )

    @Serializable
    @SerialName("AVVIST")
    data class AvvistDelutbetalingRequest(
        val aarsaker: List<String>,
        val forklaring: String?,
    ) : BesluttDelutbetalingRequest(
        besluttelse = Besluttelse.AVVIST,
    )
}

@Serializable
data class DelutbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    val belop: Int,
    val gjorOppTilsagn: Boolean,
)

@Serializable
data class OpprettDelutbetalingerRequest(
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
    val delutbetalinger: List<DelutbetalingRequest>,
)

@Serializable
data class OpprettManuellUtbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val periode: Periode,
    val beskrivelse: String,
    val kontonummer: Kontonummer,
    val kidNummer: Kid? = null,
    val belop: Int,
) {
    @Serializable
    data class Periode(
        @Serializable(with = LocalDateSerializer::class)
        val start: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val slutt: LocalDate,
    )
}
