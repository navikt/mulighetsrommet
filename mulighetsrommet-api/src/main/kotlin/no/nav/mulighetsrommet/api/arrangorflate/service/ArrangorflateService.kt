package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnSummary
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingKompakt
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingAdvarsler
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.map
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.component1

val TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR = listOf(
    TilsagnStatus.GODKJENT,
    TilsagnStatus.TIL_ANNULLERING,
    TilsagnStatus.ANNULLERT,
    TilsagnStatus.OPPGJORT,
    TilsagnStatus.TIL_OPPGJOR,
)

class ArrangorflateService(
    private val db: ApiDatabase,
    private val amtDeltakerClient: AmtDeltakerClient,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    private fun tilArrangorflateUtbetalingKompakt(utbetaling: Utbetaling): ArrangorflateUtbetalingKompakt {
        val status = ArrangorflateUtbetalingStatus.fromUtbetaling(utbetaling.status, utbetaling.blokkeringer)
        val godkjentBelop = when (status) {
            ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
            ArrangorflateUtbetalingStatus.DELVIS_UTBETALT,
            ArrangorflateUtbetalingStatus.UTBETALT,
                -> getGodkjentBelopForUtbetaling(utbetaling.id, valuta = utbetaling.beregning.output.pris.valuta)

            ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
            ArrangorflateUtbetalingStatus.UBEHANDLET_FORSLAG,
            ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
            ArrangorflateUtbetalingStatus.AVBRUTT,
                -> null
        }
        return ArrangorflateUtbetalingKompakt.fromUtbetaling(utbetaling, status, godkjentBelop)
    }

    fun getUtbetalingerByArrangorerAndStatus(
        arrangorer: Set<Organisasjonsnummer>,
        statuser: Set<UtbetalingStatusType>,
        pagination: Pagination = Pagination.all(),
    ): PaginatedResult<ArrangorflateUtbetalingKompakt> = db.session {
        val utbetalinger = queries.utbetaling.getByArrangorerAndStatus(
            arrangorer,
            statuser,
            pagination
        )
        utbetalinger.map { tilArrangorflateUtbetalingKompakt(it) }
    }

    fun getUtbetaling(id: UUID): Utbetaling? = db.session {
        return queries.utbetaling.get(id)
    }

    suspend fun getTilsagn(id: UUID): ArrangorflateTilsagnDto? = db.session {
        queries.tilsagn.get(id)
            ?.takeIf { it.status in TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR }
            ?.let { ArrangorflateTilsagnDto.from(it, getTilsagnDeltakerPersonalia(it.deltakere)) }
    }

    suspend fun getTilsagn(
        arrangorer: Set<Organisasjonsnummer>,
        statuser: List<TilsagnStatus>? = null,
        typer: List<TilsagnType>? = null,
        gjennomforingId: UUID? = null,
    ): List<ArrangorflateTilsagnDto> = db.session {
        queries.tilsagn
            .getAll(
                arrangorer = arrangorer,
                statuser = statuser,
                typer = typer,
                gjennomforingId = gjennomforingId,
            )
            .map { ArrangorflateTilsagnDto.from(it, getTilsagnDeltakerPersonalia(it.deltakere)) }
    }

    suspend fun getArrangorflateTilsagnTilUtbetaling(utbetaling: Utbetaling): List<ArrangorflateTilsagnDto> =
        db.session {
            queries.tilsagn
                .getAll(
                    gjennomforingId = utbetaling.gjennomforing.id,
                    periodeIntersectsWith = utbetaling.periode,
                    typer = TilsagnType.fromTilskuddstype(utbetaling.tilskuddstype),
                    statuser = listOf(TilsagnStatus.GODKJENT),
                )
                .map { ArrangorflateTilsagnDto.from(it, getTilsagnDeltakerPersonalia(it.deltakere)) }
        }

    fun getAdvarsler(utbetaling: Utbetaling): List<DeltakerAdvarsel> = db.session {
        return when (utbetaling.status) {
            UtbetalingStatusType.GENERERT -> {
                val forslag = queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
                val deltakere = queries.deltaker
                    .getByGjennomforingId(utbetaling.gjennomforing.id)
                    .filter { it.id in utbetaling.beregning.input.deltakelser().map { it.deltakelseId } }

                UtbetalingAdvarsler.getAdvarsler(utbetaling, deltakere, forslag)
            }

            UtbetalingStatusType.TIL_BEHANDLING,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
                -> emptyList()
        }
    }

    private fun getGodkjentBelopForUtbetaling(utbetalingId: UUID, valuta: Valuta): ValutaBelop = db.session {
        return queries.delutbetaling.getByUtbetalingId(utbetalingId).sumOf { it.pris.belop }.withValuta(valuta)
    }

    suspend fun toArrangorflateUtbetaling(
        utbetaling: Utbetaling,
        today: LocalDate = LocalDate.now(),
    ): ArrangorflateUtbetalingDto = db.session {
        val erTolvUkerEtterInnsending = utbetaling.innsending
            ?.let { it.tidspunkt.toLocalDate().plusWeeks(12) <= today }
            ?: false

        val deltakere = if (erTolvUkerEtterInnsending) {
            emptyList()
        } else {
            val deltakelser = utbetaling.beregning.input.deltakelser().map { it.deltakelseId }
            queries.deltaker
                .getByGjennomforingId(utbetaling.gjennomforing.id)
                .filter { it.id in deltakelser }
        }

        val personalia = getPersonalia(deltakere.map { it.id })
        val advarsler = getAdvarsler(utbetaling)
        val status = ArrangorflateUtbetalingStatus.fromUtbetaling(utbetaling.status, utbetaling.blokkeringer)
        val (kanRegenereres, regenrertId) = kanRegenereres(utbetaling)

        return mapUtbetalingToArrangorflateUtbetaling(
            utbetaling = utbetaling,
            status = status,
            deltakereById = deltakere.associateBy { it.id },
            personaliaById = personalia,
            advarsler = advarsler,
            linjer = getLinjer(utbetaling.id),
            kanViseBeregning = !erTolvUkerEtterInnsending,
            kanAvbrytes = arrangorAvbrytStatus(utbetaling),
            kanRegenereres = kanRegenereres,
            regenerertId = regenrertId,
        )
    }

    fun getLinjer(utbetalingId: UUID): List<ArrangforflateUtbetalingLinje> = db.session {
        queries.delutbetaling.getByUtbetalingId(utbetalingId)
            .map { delutbetaling ->
                val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId).let {
                    ArrangorflateTilsagnSummary(
                        id = it.id,
                        bestillingsnummer = it.bestilling.bestillingsnummer,
                    )
                }

                ArrangforflateUtbetalingLinje(
                    id = delutbetaling.id,
                    pris = delutbetaling.pris,
                    status = delutbetaling.status,
                    statusSistOppdatert = delutbetaling.faktura.statusSistOppdatert,
                    tilsagn = tilsagn,
                )
            }
    }

    fun QueryContext.kanRegenereres(utbetaling: Utbetaling): Pair<Boolean, UUID?> {
        if (utbetaling.innsending == null) {
            return false to null
        }
        if (utbetaling.status != UtbetalingStatusType.AVBRUTT) {
            return false to null
        }
        when (utbetaling.beregning) {
            is UtbetalingBeregningFastSatsPerTiltaksplassPerManed,
            is UtbetalingBeregningPrisPerHeleUkesverk,
            is UtbetalingBeregningPrisPerManedsverk,
            is UtbetalingBeregningPrisPerUkesverk,
                -> Unit

            is UtbetalingBeregningPrisPerTimeOppfolging,
            is UtbetalingBeregningFri,
                -> return false to null
        }

        val utbetalingerSammePeriode = queries.utbetaling.getByGjennomforing(utbetaling.gjennomforing.id)
            .filter { it.periode == utbetaling.periode }

        val regenerertKrav = utbetalingerSammePeriode
            .sortedByDescending { it.createdAt }
            .firstOrNull { it.status != UtbetalingStatusType.AVBRUTT }

        return if (regenerertKrav != null) {
            false to regenerertKrav.id
        } else {
            true to null
        }
    }

    suspend fun getKontonummer(
        orgnr: Organisasjonsnummer,
    ): Either<KontonummerRegisterOrganisasjonError, Kontonummer> {
        return kontoregisterOrganisasjonClient
            .getKontonummerForOrganisasjon(orgnr)
            .map { Kontonummer(it.kontonr) }
    }

    suspend fun synkroniserKontonummer(
        utbetaling: Utbetaling,
    ): Either<KontonummerRegisterOrganisasjonError, Kontonummer> = db.session {
        getKontonummer(utbetaling.arrangor.organisasjonsnummer).onRight { kontonummer ->
            queries.utbetaling.setKontonummer(
                id = utbetaling.id,
                kontonummer = kontonummer,
            )
        }
    }

    suspend fun getPersonalia(deltakerIds: List<UUID>): Map<UUID, ArrangorflatePersonalia> {
        return amtDeltakerClient.hentPersonalia(deltakerIds)
            .getOrElse {
                throw StatusException(
                    status = HttpStatusCode.InternalServerError,
                    detail = "Klarte ikke hente personalia fra amt-deltaker error: $it",
                )
            }
            .associateBy { it.deltakerId }
            .mapValues {
                ArrangorflatePersonalia.fromPersonalia(it.value)
            }
    }

    suspend fun getTilsagnDeltakerPersonalia(deltakere: List<Tilsagn.Deltaker>): List<ArrangorflateTilsagnDto.DeltakerPersonalia> {
        return getPersonalia(deltakere.map { it.deltakerId }).map { (deltakerId, p) ->
            ArrangorflateTilsagnDto.DeltakerPersonalia(
                deltakerId = deltakerId,
                norskIdent = p.norskIdent,
                navn = p.navn,
            )
        }
    }
}

fun arrangorAvbrytStatus(utbetaling: Utbetaling): ArrangorAvbrytStatus {
    if (utbetaling.innsending == null) {
        return ArrangorAvbrytStatus.HIDDEN
    }

    return when (utbetaling.status) {
        UtbetalingStatusType.GENERERT,
        UtbetalingStatusType.DELVIS_UTBETALT,
        UtbetalingStatusType.TIL_ATTESTERING,
            -> ArrangorAvbrytStatus.DEACTIVATED

        UtbetalingStatusType.FERDIG_BEHANDLET,
        UtbetalingStatusType.UTBETALT,
        UtbetalingStatusType.AVBRUTT,
            -> ArrangorAvbrytStatus.HIDDEN

        UtbetalingStatusType.TIL_BEHANDLING,
        UtbetalingStatusType.RETURNERT,
            -> ArrangorAvbrytStatus.ACTIVATED
    }
}

enum class ArrangorAvbrytStatus {
    ACTIVATED,
    DEACTIVATED,
    HIDDEN,
}
