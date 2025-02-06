package no.nav.mulighetsrommet.api.utbetaling

import arrow.core.left
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.utbetaling.db.TilsagnUtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatus
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.util.*

fun Route.utbetalingRoutes() {
    val db: ApiDatabase by inject()

    route("/utbetaling/{id}") {
        get {
            val id = call.parameters.getOrFail<UUID>("id")

            val utbetaling = db.session {
                val utbetaling = queries.utbetaling.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                val delutbetalinger = queries.delutbetaling.getByutbetalingId(id)
                UtbetalingKompakt.fromUtbetalingDto(utbetaling, delutbetalinger)
            }

            call.respond(utbetaling)
        }

        get("/tilsagn") {
            val id = call.parameters.getOrFail<UUID>("id")
            val utbetaling = db.session {
                queries.utbetaling.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            }
            val tilsagn = db.session {
                queries.tilsagn.getTilsagnForGjennomforing(
                    utbetaling.gjennomforing.id,
                    periode = utbetaling.periode,
                )
            }

            call.respond(tilsagn)
        }

        post("/opprett-utbetaling") {
            val utbetalingId = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<OpprettManuellUtbetalingRequest>()

            UtbetalingValidator.validateManuellUtbetalingskrav(request)
                .onLeft {
                    return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                }

            db.session {
                queries.utbetaling.upsert(
                    UtbetalingDbo(
                        id = utbetalingId,
                        gjennomforingId = request.gjennomforingId,
                        fristForGodkjenning = request.periode.slutt.plusMonths(2).atStartOfDay(),
                        kontonummer = request.kontonummer,
                        kid = request.kidNummer,
                        beregning = UtbetalingBeregningFri.beregn(
                            input = UtbetalingBeregningFri.Input(
                                belop = request.belop,
                            ),
                        ),
                        periode = Periode.fromInclusiveDates(
                            request.periode.start,
                            request.periode.slutt,
                        ),
                    ),
                )
            }

            call.respond(request)
        }

        put("/behandling") {
            val utbetalingId = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<BehandleUtbetalingRequest>()
            val navIdent = getNavIdent()
            val utbetalinger = db.session {
                queries.delutbetaling.getByutbetalingId(utbetalingId)
            }

            UtbetalingValidator.validate(request, utbetalinger)
                .onLeft {
                    return@put call.respondWithStatusResponse(ValidationError(errors = it).left())
                }

            db.session {
                queries.delutbetaling.opprettTilsagnUtbetalinger(
                    request.kostnadsfordeling.map {
                        TilsagnUtbetalingDbo(
                            utbetalingId = utbetalingId,
                            tilsagnId = it.tilsagnId,
                            belop = it.belop,
                            opprettetAv = navIdent,
                        )
                    },
                )
            }
            call.respond(HttpStatusCode.OK)
        }
    }

    route("/gjennomforinger/{id}/utbetalinger") {
        authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetalinger = db.session {
                    queries.utbetaling.getByGjennomforing(id)
                        .map { utbetaling ->
                            val delutbetalinger = queries.delutbetaling.getByutbetalingId(utbetaling.id)

                            UtbetalingKompakt.fromUtbetalingDto(utbetaling, delutbetalinger)
                        }
                }

                call.respond(utbetalinger)
            }
        }
    }
}

@Serializable
data class BehandleUtbetalingRequest(
    val kostnadsfordeling: List<TilsagnOgBelop>,
) {
    @Serializable
    data class TilsagnOgBelop(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        val belop: Int,
    )
}

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

@Serializable
data class UtbetalingKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: UtbetalingStatus,
    val beregning: Beregning,
    val delutbetalinger: List<DelutbetalingDto>,
) {
    @Serializable
    data class Beregning(
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val belop: Int,
    )

    companion object {
        fun fromUtbetalingDto(utbetaling: UtbetalingDto, delutbetalinger: List<DelutbetalingDto>) = UtbetalingKompakt(
            id = utbetaling.id,
            status = utbetaling.status,
            beregning = Beregning(
                periodeStart = utbetaling.periode.start,
                periodeSlutt = utbetaling.periode.getLastDate(),
                belop = utbetaling.beregning.output.belop,
            ),
            delutbetalinger = delutbetalinger,
        )
    }
}
