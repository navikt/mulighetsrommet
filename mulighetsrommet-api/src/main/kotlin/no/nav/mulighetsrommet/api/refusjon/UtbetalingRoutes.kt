package no.nav.mulighetsrommet.api.refusjon

import arrow.core.left
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.refusjon.db.TilsagnUtbetalingDbo
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonKravBeregningFri
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravDto
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.refusjon.model.TilsagnUtbetalingDto
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
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
                val krav = queries.refusjonskrav.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
                val utbetalinger = queries.utbetaling.getByRefusjonskravId(id)
                Utbetaling.from(RefusjonKravKompakt.fromRefusjonskravDto(krav), utbetalinger)
            }

            call.respond(utbetaling)
        }

        get("/tilsagn") {
            val id = call.parameters.getOrFail<UUID>("id")
            val krav = db.session {
                queries.refusjonskrav.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            }
            val tilsagn = db.session {
                queries.tilsagn.getTilsagnForGjennomforing(
                    krav.gjennomforing.id,
                    periode = Periode(start = krav.periodeStart, slutt = krav.periodeSlutt)
                )
            }

            call.respond(tilsagn)
        }


        post("/opprett-utbetalingskrav") {
            val kravId = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<OpprettManuellUtbetalingkravRequest>()

            UtbetalingValidator.validateManuellUtbetalingskrav(request)
                .onLeft {
                    return@post call.respondWithStatusResponse(ValidationError(errors = it).left())
                }

            db.session {
                queries.refusjonskrav.upsert(
                    RefusjonskravDbo(
                        id = kravId,
                        gjennomforingId = request.gjennomforingId,
                        fristForGodkjenning = request.periode.slutt.plusMonths(2).atStartOfDay(),
                        kontonummer = request.kontonummer,
                        kid = request.kidNummer,
                        beregning = RefusjonKravBeregningFri.beregn(
                            input = RefusjonKravBeregningFri.Input(
                                belop = request.belop
                            )
                        ),
                        periode = Periode.fromInclusiveDates(
                            request.periode.start,
                            request.periode.slutt
                        ),
                    )
                )
            }

            call.respond(request)
        }

        put("/behandling") {
            val kravId = call.parameters.getOrFail<UUID>("id")
            val request = call.receive<UtbetalingRequest>()
            val navIdent = getNavIdent()
            val utbetalinger = db.session {
                queries.utbetaling.getByRefusjonskravId(kravId)
            }

            UtbetalingValidator.validate(request, utbetalinger)
                .onLeft {
                    return@put call.respondWithStatusResponse(ValidationError(errors = it).left())
                }

            db.session {
                queries.utbetaling.opprettTilsagnUtbetalinger(
                    request.kostnadsfordeling.map {
                        TilsagnUtbetalingDbo(
                            refusjonskravId = kravId,
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
                    queries.refusjonskrav.getByGjennomforing(id)
                        .map { krav ->
                            val utbetalinger = queries.utbetaling.getByRefusjonskravId(krav.id)

                            Utbetaling.from(RefusjonKravKompakt.fromRefusjonskravDto(krav), utbetalinger)
                        }
                }

                call.respond(utbetalinger)
            }
        }
    }
}

@Serializable
data class UtbetalingRequest(
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
data class OpprettManuellUtbetalingkravRequest(
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
        val slutt: LocalDate
    )
}

@Serializable
sealed class Utbetaling {
    @Serializable
    @SerialName("UTBETALING_TIL_GODKJENNING")
    data class UtbetalingTilGodkjenning(
        val krav: RefusjonKravKompakt,
        val utbetalinger: List<TilsagnUtbetalingDto>,
    ) : Utbetaling()

    @Serializable
    @SerialName("UTBETALING_GODKJENT")
    data class UtbetalingGodkjent(
        val krav: RefusjonKravKompakt,
        val utbetalinger: List<TilsagnUtbetalingDto.TilsagnUtbetalingGodkjent>,
    ) : Utbetaling()

    companion object {
        fun from(krav: RefusjonKravKompakt, utbetalinger: List<TilsagnUtbetalingDto>): Utbetaling {
            return when (utbetalinger.all { it is TilsagnUtbetalingDto.TilsagnUtbetalingGodkjent }) {
                true -> UtbetalingGodkjent(
                    krav = krav,
                    // Alle er godkjent, gitt sjekken i when
                    utbetalinger = utbetalinger.filterIsInstance<TilsagnUtbetalingDto.TilsagnUtbetalingGodkjent>(),
                )

                false -> UtbetalingTilGodkjenning(
                    krav = krav,
                    utbetalinger = utbetalinger,
                )
            }
        }
    }
}

@Serializable
data class RefusjonKravKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: RefusjonskravStatus,
    val beregning: Beregning,
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
        fun fromRefusjonskravDto(krav: RefusjonskravDto) = RefusjonKravKompakt(
            id = krav.id,
            status = krav.status,
            beregning = Beregning(
                periodeStart = krav.periodeStart,
                periodeSlutt = krav.periodeSlutt,
                belop = krav.beregning.output.belop,
            ),
        )
    }
}
