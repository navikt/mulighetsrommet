package no.nav.mulighetsrommet.api.tilskuddbehandling.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.util.getValue
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.helved.HelVedStatus
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultatDto
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.tilskuddUtbetalingRoutes() {
    val db: ApiDatabase by inject()

    get("/tilskudd-utbetaling", {
        description = "Hent alle utbetalinger for gitt gjennomføring"
        tags = setOf("Utbetaling", "Tilskudd")
        operationId = "getTilskuddUtbetalinger"
        request {
            queryParameterUuid("gjennomforingId") {
                required = true
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Alle utbetalinger for gitt gjennomføring"
                body<List<TilskuddUtbetalingKompaktDto>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val gjennomforingId: UUID by call.queryParameters
        val utbetalinger = db.session {
            queries.tilskuddBehandling.getByGjennomforingId(gjennomforingId)
                .flatMap { behandling ->
                    behandling.tilskudd.mapNotNull { tilskudd ->
                        val arrangorUtbetaling = queries.utbetaling.getByTilskudd(tilskudd.id)
                        if (arrangorUtbetaling != null) {
                            val utbetalingLinje = queries.utbetalingLinje.getByUtbetalingId(arrangorUtbetaling.id)[0]
                            val belopUtbetalt = utbetalingLinje.pris.belop.withValuta(arrangorUtbetaling.valuta)
                            val kostnadssted = queries.tilsagn.get(utbetalingLinje.tilsagnId)?.kostnadssted

                            TilskuddUtbetalingKompaktDto(
                                id = arrangorUtbetaling.id,
                                tilskuddBehandlingId = behandling.id,
                                status = TilskuddUtbetalingStatusDto.from(arrangorUtbetaling.status),
                                periode = arrangorUtbetaling.periode,
                                type = tilskudd.tilskuddOpplaeringType,
                                kostnadssted = kostnadssted?.let { KostnadsstedDto.fromNavEnhetDbo(it) },
                                belopUtbetalt = belopUtbetalt,
                                mottaker = TilskuddMottaker.ARRANGOR,
                                vedtakResultat = tilskudd.vedtakResultat,
                            )
                        } else {
                            queries.helvedUtbetaling.getByTilskudd(tilskudd.id)?.let { utbetaling ->
                                TilskuddUtbetalingKompaktDto(
                                    id = utbetaling.id,
                                    tilskuddBehandlingId = behandling.id,
                                    status = TilskuddUtbetalingStatusDto.from(utbetaling.helVedStatus),
                                    periode = utbetaling.periode,
                                    type = tilskudd.tilskuddOpplaeringType,
                                    kostnadssted = null,
                                    belopUtbetalt = utbetaling.belop.withValuta(Valuta.NOK),
                                    mottaker = TilskuddMottaker.BRUKER,
                                    vedtakResultat = tilskudd.vedtakResultat,
                                )
                            }
                        }
                    }
                }
        }

        call.respond(utbetalinger)
    }
}

@Serializable
data class TilskuddUtbetalingKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val tilskuddBehandlingId: UUID,
    val status: TilskuddUtbetalingStatusDto,
    val periode: Periode,
    val type: Opplaeringtilskudd.Kode,
    val kostnadssted: KostnadsstedDto?,
    val belopUtbetalt: ValutaBelop?,
    val vedtakResultat: VedtakResultatDto,
    val mottaker: TilskuddMottaker,
)

@Serializable
data class TilskuddUtbetalingStatusDto(
    val type: Type,
    val status: DataElement.Status,
) {
    companion object {
        fun from(utbetalingStatus: UtbetalingStatusType): TilskuddUtbetalingStatusDto {
            val type: Type = when (utbetalingStatus) {
                UtbetalingStatusType.TIL_BEHANDLING,
                UtbetalingStatusType.AVBRUTT,
                UtbetalingStatusType.TIL_ATTESTERING,
                UtbetalingStatusType.RETURNERT,
                UtbetalingStatusType.GENERERT,
                -> throw IllegalStateException("Tilskudd utbetaling status var $utbetalingStatus. Burde ikke være mulig")

                UtbetalingStatusType.FERDIG_BEHANDLET -> Type.OVERFORT_TIL_UTBETALING

                UtbetalingStatusType.DELVIS_UTBETALT -> Type.DELVIS_UTBETALT

                UtbetalingStatusType.UTBETALT -> Type.UTBETALT
            }

            val status = DataElement.Status(type.beskrivelse, type.variant)
            return TilskuddUtbetalingStatusDto(type, status)
        }

        fun from(helVedStatus: HelVedStatus.Status?): TilskuddUtbetalingStatusDto {
            val type: Type = when (helVedStatus) {
                null,
                HelVedStatus.Status.MOTTATT,
                HelVedStatus.Status.HOS_OPPDRAG,
                HelVedStatus.Status.OK,
                HelVedStatus.Status.FEILET,
                -> Type.OVERFORT_TIL_UTBETALING
            }

            val status = DataElement.Status(type.beskrivelse, type.variant)
            return TilskuddUtbetalingStatusDto(type, status)
        }
    }

    enum class Type(val beskrivelse: String, val variant: DataElement.Status.Variant) {
        FEILET("Feilet", DataElement.Status.Variant.ERROR),
        OVERFORT_TIL_UTBETALING("Overført til utbetaling", DataElement.Status.Variant.SUCCESS),
        DELVIS_UTBETALT("Delvis utbetalt", DataElement.Status.Variant.SUCCESS),
        UTBETALT("Utbetalt", DataElement.Status.Variant.SUCCESS),
    }
}
