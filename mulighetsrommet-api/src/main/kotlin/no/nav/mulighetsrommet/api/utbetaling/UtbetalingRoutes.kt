package no.nav.mulighetsrommet.api.utbetaling

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
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun Route.utbetalingRoutes() {
    val db: ApiDatabase by inject()
    val service: UtbetalingService by inject()

    route("/utbetaling/{id}") {
        get {
            val id = call.parameters.getOrFail<UUID>("id")

            val utbetaling = db.session {
                val utbetaling = queries.utbetaling.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                val delutbetalinger = queries.delutbetaling.getByUtbetalingId(id)
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

        route("/delutbetaling") {
            put {
                val utbetalingId = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<DelutbetalingRequest>()
                val navIdent = getNavIdent()

                val result = service.upsertDelutbetaling(utbetalingId, request, navIdent)
                    .mapLeft { ValidationError(errors = it) }
                call.respondWithStatusResponse(result)
            }

            post("/beslutt") {
                val id = call.parameters.getOrFail<UUID>("id")
                val request = call.receive<BesluttDelutbetalingRequest>()
                val navIdent = getNavIdent()

                call.respondWithStatusResponse(service.besluttDelutbetaling(request, id, navIdent))
            }
        }
    }

    route("/gjennomforinger/{id}/utbetalinger") {
        authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
            get {
                val id = call.parameters.getOrFail<UUID>("id")

                val utbetalinger = db.session {
                    queries.utbetaling.getByGjennomforing(id)
                        .map { utbetaling ->
                            val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)

                            UtbetalingKompakt.fromUtbetalingDto(utbetaling, delutbetalinger)
                        }
                }

                call.respond(utbetalinger)
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("besluttelse")
sealed class BesluttDelutbetalingRequest(
    val besluttelse: Besluttelse,
) {
    abstract val tilsagnId: UUID

    @Serializable
    @SerialName("GODKJENT")
    data class GodkjentDelutbetalingRequest(
        @Serializable(with = UUIDSerializer::class)
        override val tilsagnId: UUID,
    ) : BesluttDelutbetalingRequest(
        besluttelse = Besluttelse.GODKJENT,
    )

    @Serializable
    @SerialName("AVVIST")
    data class AvvistDelutbetalingRequest(
        val aarsaker: List<String>,
        val forklaring: String?,
        @Serializable(with = UUIDSerializer::class)
        override val tilsagnId: UUID,
    ) : BesluttDelutbetalingRequest(
        besluttelse = Besluttelse.AVVIST,
    )
}

@Serializable
data class DelutbetalingRequest(
    @Serializable(with = UUIDSerializer::class)
    val tilsagnId: UUID,
    val belop: Int,
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

@Serializable
data class UtbetalingKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: UtbetalingStatus,
    val beregning: Beregning,
    val delutbetalinger: List<DelutbetalingDto>,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val betalingsinformasjon: UtbetalingDto.Betalingsinformasjon,
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
            godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
            delutbetalinger = delutbetalinger,
            betalingsinformasjon = utbetaling.betalingsinformasjon,
            createdAt = utbetaling.createdAt,
        )
    }
}
