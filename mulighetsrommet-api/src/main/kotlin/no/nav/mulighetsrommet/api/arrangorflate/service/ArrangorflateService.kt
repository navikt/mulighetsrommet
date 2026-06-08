package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnSummary
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingFilter
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingKompakt
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.model.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingAdvarsler
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningAvtaltPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerBenyttetPlassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskLocalDateTime
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.map
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import java.time.LocalDate
import java.util.UUID

val TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR = listOf(
    TilsagnStatus.GODKJENT,
    TilsagnStatus.TIL_ANNULLERING,
    TilsagnStatus.ANNULLERT,
    TilsagnStatus.OPPGJORT,
    TilsagnStatus.TIL_OPPGJOR,
)

class ArrangorflateService(
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {

    fun getAllUtbetalingKompakt(filter: ArrangorflateUtbetalingFilter): PaginatedResult<ArrangorflateUtbetalingKompakt> = db.session {
        queries.utbetaling
            .getArrangorflateFiltered(filter)
            .map { toArrangorflateUtbetalingKompakt(it) }
    }

    suspend fun getTilsagn(id: UUID): ArrangorflateTilsagnDto? = db.session {
        queries.tilsagn.get(id)
            ?.takeIf { it.status in TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR }
            ?.let { ArrangorflateTilsagnDto.from(it, getPersonalia(it.deltakere.map { it.deltakerId })) }
    }

    fun getUtbetaling(id: UUID): Utbetaling? = db.session {
        return queries.utbetaling.get(id)
    }

    suspend fun getArrangorflateTilsagnTilUtbetaling(utbetaling: Utbetaling): List<ArrangorflateTilsagnDto> = db.session {
        queries.tilsagn
            .getAll(
                gjennomforingId = utbetaling.gjennomforing.id,
                periodeIntersectsWith = utbetaling.periode,
                typer = TilsagnType.fromTilskuddstype(utbetaling.tilskuddstype),
                statuser = listOf(TilsagnStatus.GODKJENT),
            )
            .map { ArrangorflateTilsagnDto.from(it, getPersonalia(it.deltakere.map { it.deltakerId })) }
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

    suspend fun toArrangorflateUtbetaling(
        utbetaling: Utbetaling,
        today: LocalDate = LocalDate.now(),
    ): ArrangorflateUtbetalingDto = db.session {
        val erTolvUkerEtterInnsending = utbetaling.innsending
            ?.let { it.tidspunkt.toLocalDate().plusWeeks(12) <= today }
            ?: false

        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(utbetaling.gjennomforing.id)

        val deltakere = if (erTolvUkerEtterInnsending) {
            emptyList()
        } else {
            val deltakelser = utbetaling.beregning.input.deltakelser().map { it.deltakelseId }
            queries.deltaker
                .getByGjennomforingId(utbetaling.gjennomforing.id)
                .filter { it.id in deltakelser }
        }

        val personalia = personaliaService.getPersonalia(deltakere.map { it.id }, PersonaliaService.OnBehalfOf.Arrangor)
            .associateBy { it.deltakerId }
        val advarsler = getAdvarsler(utbetaling)
        val (kanRegenereres, regenrertId) = kanRegenereres(utbetaling)

        return mapUtbetalingToArrangorflateUtbetaling(
            utbetaling = utbetaling,
            gjennomforing = gjennomforing,
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
        queries.utbetalingLinje.getByUtbetalingId(utbetalingId)
            .map { linje ->
                val tilsagn = queries.tilsagn.getOrError(linje.tilsagnId).let {
                    ArrangorflateTilsagnSummary(
                        id = it.id,
                        bestillingsnummer = it.bestilling.bestillingsnummer,
                    )
                }

                ArrangforflateUtbetalingLinje(
                    id = linje.id,
                    pris = linje.pris,
                    status = linje.status,
                    statusSistOppdatert = linje.faktura.statusEndretTidspunkt?.tilNorskLocalDateTime(),
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
            is UtbetalingBeregningFastSatsPerBenyttetPlassPerManed,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerHeleUke,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerManed,
            is UtbetalingBeregningAvtaltPrisPerBenyttetPlassPerUke,
            -> Unit

            is UtbetalingBeregningAvtaltPrisPerTimeOppfolging,
            is UtbetalingBeregningFastSatsPerAvtaltTiltaksplassPerManed,
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

    suspend fun getPersonalia(deltakerIds: List<UUID>): List<ArrangorflatePersonalia> {
        return personaliaService.getPersonalia(deltakerIds, PersonaliaService.OnBehalfOf.Arrangor)
            .map { p ->
                ArrangorflatePersonalia(
                    p.norskIdent(),
                    p.navn(),
                )
            }
    }

    private fun QueryContext.toArrangorflateUtbetalingKompakt(utbetaling: Utbetaling): ArrangorflateUtbetalingKompakt {
        val gjennomforing = queries.gjennomforing.getGjennomforingAvtaleOrError(utbetaling.gjennomforing.id)
        val status = ArrangorflateUtbetalingStatus.fromUtbetaling(utbetaling)
        val godkjentBelop = when (status) {
            ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
            ArrangorflateUtbetalingStatus.DELVIS_UTBETALT,
            ArrangorflateUtbetalingStatus.UTBETALT,
            -> getGodkjentBelopForUtbetaling(utbetaling.id, utbetaling.beregning.output.pris.valuta)

            ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
            ArrangorflateUtbetalingStatus.UBEHANDLET_FORSLAG,
            ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
            ArrangorflateUtbetalingStatus.AVBRUTT,
            -> null
        }
        return ArrangorflateUtbetalingKompakt.fromUtbetaling(utbetaling, gjennomforing, status, godkjentBelop)
    }

    private fun QueryContext.getGodkjentBelopForUtbetaling(id: UUID, valuta: Valuta): ValutaBelop {
        return queries.utbetalingLinje.getByUtbetalingId(id).sumOf { it.pris.belop }.withValuta(valuta)
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
