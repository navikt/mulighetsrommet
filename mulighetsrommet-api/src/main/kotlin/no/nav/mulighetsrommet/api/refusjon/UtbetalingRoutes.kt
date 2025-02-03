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
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.plugins.AuthProvider
import no.nav.mulighetsrommet.api.plugins.authenticate
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.refusjon.db.TilsagnUtbetalingDbo
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravDto
import no.nav.mulighetsrommet.api.refusjon.model.RefusjonskravStatus
import no.nav.mulighetsrommet.api.refusjon.model.TilsagnUtbetalingDto
import no.nav.mulighetsrommet.api.responses.ValidationError
import no.nav.mulighetsrommet.api.responses.respondWithStatusResponse
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

            val krav = db.session {
                queries.refusjonskrav.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            }
            val utbetalinger = db.session {
                queries.utbetaling.getByRefusjonskravId(id)
            }
            val tilsagn = db.session {
                queries.tilsagn.getTilsagnTilRefusjon(krav.gjennomforing.id, periode = krav.beregning.input.periode)
            }

            call.respond(
                Utbetaling.from(
                    RefusjonKravKompakt.fromRefusjonskravDto(krav, tilsagn.map { it.kostnadssted }),
                    utbetalinger,
                ),
            )
        }

        get("/tilsagn") {
            val id = call.parameters.getOrFail<UUID>("id")
            val krav = db.session {
                queries.refusjonskrav.get(id) ?: return@get call.respond(HttpStatusCode.NotFound)
            }
            val tilsagn = db.session {
                queries.tilsagn.getTilsagnTilRefusjon(krav.gjennomforing.id, periode = krav.beregning.input.periode)
            }

            call.respond(tilsagn)
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
                            val tilsagn = queries.tilsagn.getTilsagnTilRefusjon(krav.gjennomforing.id, krav.beregning.input.periode)
                            val kravKompakt = RefusjonKravKompakt.fromRefusjonskravDto(krav, tilsagn.map { it.kostnadssted })
                            val utbetalinger = queries.utbetaling.getByRefusjonskravId(krav.id)

                            Utbetaling.from(kravKompakt, utbetalinger)
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
    val kostnadsteder: List<NavEnhetDbo>,
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
        fun fromRefusjonskravDto(krav: RefusjonskravDto, kostnadsteder: List<NavEnhetDbo>) = RefusjonKravKompakt(
            id = krav.id,
            status = krav.status,
            beregning = krav.beregning.let {
                Beregning(
                    periodeStart = it.input.periode.start,
                    periodeSlutt = it.input.periode.getLastDate(),
                    belop = it.output.belop,
                )
            },
            kostnadsteder = kostnadsteder,
        )
    }
}
