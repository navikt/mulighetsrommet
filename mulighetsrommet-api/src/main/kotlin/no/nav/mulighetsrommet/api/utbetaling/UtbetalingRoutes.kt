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
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
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

            val utbetaling = service.getUtbetalingKompakt(id)

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
            val utbetaling = db.session {
                queries.utbetaling.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            }
            val tilsagn = db.session {
                queries.tilsagn.getAll(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periode = utbetaling.periode,
                    typer = listOf(TilsagnType.TILSAGN, TilsagnType.EKSTRATILSAGN),
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

                    val result = service.validateAndUpsertDelutbetaling(utbetalingId, request, navIdent)
                    call.respondWithStatusResponse(result)
                }

                put("/bulk") {
                    val utbetalingId = call.parameters.getOrFail<UUID>("id")
                    val request = call.receive<DelutbetalingBulkRequest>()
                    val navIdent = getNavIdent()

                    val result = service.opprettDelutbetalinger(utbetalingId, request, navIdent)
                    call.respondWithStatusResponse(result)
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

            val utbetalinger = service.getUtbetalingKompaktByGjennomforing(id)

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
    val frigjorTilsagn: Boolean,
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
