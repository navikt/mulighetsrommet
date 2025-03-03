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
import no.nav.mulighetsrommet.api.endringshistorikk.DocumentClass
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.ktor.exception.NotFound
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Kontonummer
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
                UtbetalingKompakt.fromUtbetalingDto(utbetaling)
            }

            call.respond(utbetaling)
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
            val utbetaling = db.session {
                queries.utbetaling.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            }
            val tilsagn = db.session {
                queries.tilsagn.getAll(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periode = utbetaling.periode,
                )
            }

            call.respond(tilsagn)
        }

        authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
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

        route("/delutbetaling") {
            authenticate(AuthProvider.AZURE_AD_TILTAKSJENNOMFORINGER_SKRIV) {
                put {
                    val utbetalingId = call.parameters.getOrFail<UUID>("id")
                    val request = call.receive<DelutbetalingRequest>()
                    val navIdent = getNavIdent()

                    call.respondWithStatusResponse(service.validateAndUpsertDelutbetaling(utbetalingId, request, navIdent))
                }
                put("/bulk") {
                    val utbetalingId = call.parameters.getOrFail<UUID>("id")
                    val request = call.receive<DelutbetalingBulkRequest>()
                    val navIdent = getNavIdent()

                    call.respondWithStatusResponse(service.opprettDelutbetalinger(utbetalingId, request, navIdent))
                }
            }

            authenticate(AuthProvider.AZURE_AD_OKONOMI_BESLUTTER) {
                post("/beslutt") {
                    val request = call.receive<BesluttDelutbetalingRequest>()
                    val navIdent = getNavIdent()

                    service.besluttDelutbetaling(request, navIdent)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    route("/gjennomforinger/{id}/utbetalinger") {
        get {
            val id = call.parameters.getOrFail<UUID>("id")

            val utbetalinger = db.session {
                queries.utbetaling.getByGjennomforing(id)
                    .map { utbetaling ->
                        UtbetalingKompakt.fromUtbetalingDto(utbetaling)
                    }
            }

            call.respond(utbetalinger)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("besluttelse")
sealed class BesluttDelutbetalingRequest(
    val besluttelse: Besluttelse,
) {
    abstract val id: UUID

    @Serializable
    @SerialName("GODKJENT")
    data class GodkjentDelutbetalingRequest(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
    ) : BesluttDelutbetalingRequest(
        besluttelse = Besluttelse.GODKJENT,
    )

    @Serializable
    @SerialName("AVVIST")
    data class AvvistDelutbetalingRequest(
        val aarsaker: List<String>,
        val forklaring: String?,
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
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
)

@Serializable
data class DelutbetalingBulkRequest(
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
        fun fromUtbetalingDto(utbetaling: UtbetalingDto) = UtbetalingKompakt(
            id = utbetaling.id,
            status = utbetaling.status,
            beregning = Beregning(
                periodeStart = utbetaling.periode.start,
                periodeSlutt = utbetaling.periode.getLastInclusiveDate(),
                belop = utbetaling.beregning.output.belop,
            ),
            godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
            delutbetalinger = utbetaling.delutbetalinger,
            betalingsinformasjon = utbetaling.betalingsinformasjon,
            createdAt = utbetaling.createdAt,
        )
    }
}
