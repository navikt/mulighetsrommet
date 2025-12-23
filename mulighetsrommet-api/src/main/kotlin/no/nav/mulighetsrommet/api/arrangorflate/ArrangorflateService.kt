package no.nav.mulighetsrommet.api.arrangorflate

import arrow.core.Either
import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateArrangor
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateGjennomforingInfo
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflatePersonalia
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateTilsagnDto
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateTilsagnSummary
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateTiltakstype
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingDto
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingKompaktDto
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalinger
import no.nav.mulighetsrommet.api.arrangorflate.api.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.arrangorflate.api.mapUtbetalingToArrangorflateUtbetaling
import no.nav.mulighetsrommet.api.clients.amtDeltaker.AmtDeltakerClient
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import java.time.LocalDate
import java.time.LocalDateTime
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
    private val amtDeltakerClient: AmtDeltakerClient,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    fun getUtbetalinger(orgnr: Organisasjonsnummer): ArrangorflateUtbetalinger = db.session {
        val (aktive, historiske) = queries.utbetaling.getByArrangorIds(orgnr)
            .map { utbetaling ->
                tilArrangorflateUtbetalingKompakt(utbetaling)
            }
            .partition { dto ->
                when (dto.status) {
                    ArrangorflateUtbetalingStatus.KREVER_ENDRING,
                    ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
                    ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
                    -> true

                    ArrangorflateUtbetalingStatus.DELVIS_UTBETALT,
                    ArrangorflateUtbetalingStatus.UTBETALT,
                    ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
                    ArrangorflateUtbetalingStatus.AVBRUTT,
                    -> false
                }
            }
        return ArrangorflateUtbetalinger(
            aktive = aktive,
            historiske = historiske,
        )
    }

    private fun tilArrangorflateUtbetalingKompakt(utbetaling: Utbetaling): ArrangorflateUtbetalingKompaktDto {
        val harAdvarsler = when (utbetaling.status) {
            UtbetalingStatusType.GENERERT -> harAdvarsler(utbetaling)

            UtbetalingStatusType.INNSENDT,
            UtbetalingStatusType.TIL_ATTESTERING,
            UtbetalingStatusType.RETURNERT,
            UtbetalingStatusType.FERDIG_BEHANDLET,
            UtbetalingStatusType.DELVIS_UTBETALT,
            UtbetalingStatusType.UTBETALT,
            UtbetalingStatusType.AVBRUTT,
            -> false
        }
        val status = getArrangorflateUtbetalingStatus(utbetaling, harAdvarsler)
        val godkjentBelop = when (status) {
            ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
            ArrangorflateUtbetalingStatus.DELVIS_UTBETALT,
            ArrangorflateUtbetalingStatus.UTBETALT,
            -> getGodkjentBelopForUtbetaling(utbetaling.id)

            ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
            ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
            ArrangorflateUtbetalingStatus.KREVER_ENDRING,
            ArrangorflateUtbetalingStatus.AVBRUTT,
            -> null
        }
        return ArrangorflateUtbetalingKompaktDto.fromUtbetaling(utbetaling, status, godkjentBelop)
    }

    fun getUtbetalingerByArrangorerAndStatus(arrangorer: Set<Organisasjonsnummer>, statuser: Set<UtbetalingStatusType>): List<ArrangorflateUtbetalingKompaktDto> = db.session {
        queries.utbetaling.getByArrangorerAndStatus(arrangorer, statuser).map { tilArrangorflateUtbetalingKompakt(it) }
    }

    fun getUtbetaling(id: UUID): Utbetaling? = db.session {
        return queries.utbetaling.get(id)
    }

    fun getTilsagn(id: UUID): ArrangorflateTilsagnDto? = db.session {
        queries.tilsagn.get(id)
            ?.takeIf { it.status in TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR }
            ?.let { toArrangorflateTilsagn(it) }
    }

    fun getTilsagn(
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
            .map { toArrangorflateTilsagn(it) }
    }

    fun getArrangorflateTilsagnTilUtbetaling(utbetaling: Utbetaling): List<ArrangorflateTilsagnDto> = db.session {
        queries.tilsagn
            .getAll(
                gjennomforingId = utbetaling.gjennomforing.id,
                periodeIntersectsWith = utbetaling.periode,
                typer = TilsagnType.fromTilskuddstype(utbetaling.tilskuddstype),
                statuser = listOf(TilsagnStatus.GODKJENT),
            )
            .map { toArrangorflateTilsagn(it) }
    }

    fun harAdvarsler(utbetaling: Utbetaling): Boolean = db.session {
        val deltakere = queries.deltaker
            .getAll(gjennomforingId = utbetaling.gjennomforing.id)
            .filter { it.id in utbetaling.beregning.input.deltakelser().map { it.deltakelseId } }

        return (deltakereMedRelevanteForslag(utbetaling) + deltakereMedFeilSluttDato(deltakere, LocalDate.now())).isNotEmpty()
    }

    suspend fun getAdvarsler(utbetaling: Utbetaling): List<DeltakerAdvarsel> = db.session {
        val deltakelseIds = utbetaling.beregning.deltakelsePerioder().map { it.deltakelseId }.toSet()
        val personalia = getPersonalia(deltakelseIds)
        val deltakere = queries.deltaker
            .getAll(gjennomforingId = utbetaling.gjennomforing.id)
            .filter { it.id in utbetaling.beregning.input.deltakelser().map { it.deltakelseId } }

        return getRelevanteForslag(utbetaling, personalia) + getFeilSluttDato(deltakere, personalia, LocalDate.now())
    }

    fun getFeilSluttDato(
        deltakere: List<Deltaker>,
        personalia: Map<UUID, ArrangorflatePersonalia>,
        today: LocalDate,
    ): List<DeltakerAdvarsel.FeilSluttDato> {
        return deltakereMedFeilSluttDato(deltakere, today)
            .map { deltakerId ->
                val navn = personalia[deltakerId]?.navn
                val deltaker = deltakere.find { it.id == deltakerId }
                DeltakerAdvarsel.FeilSluttDato(
                    deltakerId = deltakerId,
                    beskrivelse = "$navn har status “${deltaker?.status}” og slutt dato frem i tid",
                )
            }
    }

    private fun QueryContext.deltakereMedRelevanteForslag(utbetaling: Utbetaling): List<UUID> {
        return queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
            .mapNotNull { (deltakerId, forslag) ->
                when (forslag.count { isForslagRelevantForUtbetaling(it, utbetaling) }) {
                    0 -> null
                    else -> deltakerId
                }
            }
    }

    private fun QueryContext.getRelevanteForslag(utbetaling: Utbetaling, personalia: Map<UUID, ArrangorflatePersonalia>): List<DeltakerAdvarsel.RelevanteForslag> {
        return deltakereMedRelevanteForslag(utbetaling)
            .map { deltakerId ->
                DeltakerAdvarsel.RelevanteForslag(
                    deltakerId = deltakerId,
                    beskrivelse = "${personalia[deltakerId]?.navn} har ubehandlede forslag. Disse må først godkjennes av Nav-veileder før utbetalingen oppdaterer seg",
                )
            }
    }

    private fun getGodkjentBelopForUtbetaling(utbetalingId: UUID): Int = db.session {
        return queries.delutbetaling.getByUtbetalingId(utbetalingId).sumOf { it.belop }
    }

    suspend fun toArrangorflateUtbetaling(
        utbetaling: Utbetaling,
        relativeDate: LocalDateTime = LocalDateTime.now(),
    ): ArrangorflateUtbetalingDto = db.session {
        val erTolvUkerEtterInnsending = utbetaling.godkjentAvArrangorTidspunkt
            ?.let { it.plusWeeks(12) <= relativeDate } ?: false

        val deltakere = if (erTolvUkerEtterInnsending) {
            emptyList()
        } else {
            val deltakelser = utbetaling.beregning.input.deltakelser().map { it.deltakelseId }
            queries.deltaker
                .getAll(gjennomforingId = utbetaling.gjennomforing.id)
                .filter { it.id in deltakelser }
        }

        val personalia = getPersonalia(deltakere.map { it.id }.toSet())
        val advarsler = getAdvarsler(utbetaling)
        val status = getArrangorflateUtbetalingStatus(utbetaling, advarsler.isNotEmpty())

        return mapUtbetalingToArrangorflateUtbetaling(
            utbetaling = utbetaling,
            status = status,
            deltakereById = deltakere.associateBy { it.id },
            personaliaById = personalia,
            advarsler = advarsler,
            linjer = getLinjer(utbetaling.id),
            kanViseBeregning = !erTolvUkerEtterInnsending,
            kanAvbrytes = arrangorAvbrytStatus(utbetaling),
        )
    }

    fun getLinjer(utbetalingId: UUID): List<ArrangforflateUtbetalingLinje> = db.session {
        queries.delutbetaling.getByUtbetalingId(utbetalingId)
            .map { delutbetaling ->
                val tilsagn = queries.tilsagn.getOrError(delutbetaling.tilsagnId).let {
                    TilsagnDto.fromTilsagn(it)
                }

                ArrangforflateUtbetalingLinje(
                    id = delutbetaling.id,
                    belop = delutbetaling.belop,
                    status = delutbetaling.status,
                    statusSistOppdatert = delutbetaling.faktura.statusSistOppdatert,
                    tilsagn = ArrangorflateTilsagnSummary(
                        id = tilsagn.id,
                        bestillingsnummer = tilsagn.bestillingsnummer,
                    ),
                )
            }
    }

    private fun getArrangorflateUtbetalingStatus(
        utbetaling: Utbetaling,
        harAdvarsler: Boolean,
    ): ArrangorflateUtbetalingStatus {
        return ArrangorflateUtbetalingStatus.fromUtbetaling(
            utbetaling.status,
            harAdvarsler = harAdvarsler,
        )
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

    suspend fun getPersonalia(deltakerIds: Set<UUID>): Map<UUID, ArrangorflatePersonalia> {
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
}

fun isForslagRelevantForUtbetaling(
    forslag: DeltakerForslag,
    utbetaling: Utbetaling,
): Boolean {
    val periode = utbetaling.beregning.input.deltakelser()
        .find { it.deltakelseId == forslag.deltakerId }
        ?.periode()
        ?: return false
    return isForslagRelevantForPeriode(forslag, utbetaling.periode, periode)
}

fun isForslagRelevantForPeriode(
    forslag: DeltakerForslag,
    utbetalingPeriode: Periode,
    deltakelsePeriode: Periode,
): Boolean {
    val deltakerPeriodeSluttDato = deltakelsePeriode.getLastInclusiveDate()

    return when (forslag.endring) {
        is Melding.Forslag.Endring.AvsluttDeltakelse -> {
            val sluttDato = forslag.endring.sluttdato
            forslag.endring.harDeltatt == false || (sluttDato != null && sluttDato.isBefore(deltakerPeriodeSluttDato))
        }

        is Melding.Forslag.Endring.Deltakelsesmengde -> {
            forslag.endring.gyldigFra?.isBefore(deltakerPeriodeSluttDato) ?: true
        }

        is Melding.Forslag.Endring.ForlengDeltakelse -> {
            val sluttdato = forslag.endring.sluttdato
            sluttdato.isAfter(deltakerPeriodeSluttDato) && sluttdato.isBefore(utbetalingPeriode.slutt)
        }

        is Melding.Forslag.Endring.Sluttdato -> {
            forslag.endring.sluttdato.isBefore(deltakerPeriodeSluttDato)
        }

        is Melding.Forslag.Endring.Startdato -> {
            forslag.endring.startdato.isAfter(deltakelsePeriode.start)
        }

        is Melding.Forslag.Endring.Sluttarsak -> false

        is Melding.Forslag.Endring.IkkeAktuell,
        is Melding.Forslag.Endring.FjernOppstartsdato,
        is Melding.Forslag.Endring.EndreAvslutning,
        -> true
    }
}

fun deltakereMedFeilSluttDato(
    deltakere: List<Deltaker>,
    today: LocalDate,
): List<UUID> {
    return deltakere.mapNotNull {
        it.id.takeIf { _ -> harFeilSluttDato(it.status.type, it.sluttDato, today = today) }
    }
}

fun harFeilSluttDato(
    deltakerStatusType: DeltakerStatusType,
    sluttDato: LocalDate?,
    today: LocalDate,
): Boolean {
    return deltakerStatusType in listOf(
        DeltakerStatusType.AVBRUTT,
        DeltakerStatusType.FULLFORT,
        DeltakerStatusType.HAR_SLUTTET,
    ) &&
        (sluttDato == null || sluttDato.isAfter(today))
}

data class DeltakerOgPeriode(
    val id: UUID,
    val norskIdent: NorskIdent,
    val periode: Periode,
)

fun harOverlappendePeriode(
    deltakerOgPeriode: DeltakerOgPeriode,
    deltakere: List<DeltakerOgPeriode>,
): Boolean {
    val sammePerson = deltakere
        .filter { (idB, _, _) -> idB != deltakerOgPeriode.id }
        .filter { (_, norskIdentB, _) -> norskIdentB == deltakerOgPeriode.norskIdent }
    return sammePerson.any { (_, _, periodeB) -> periodeB.intersects(deltakerOgPeriode.periode) }
}

fun toArrangorflateTilsagn(
    tilsagn: Tilsagn,
): ArrangorflateTilsagnDto {
    return ArrangorflateTilsagnDto(
        id = tilsagn.id,
        gjennomforing = ArrangorflateGjennomforingInfo(
            id = tilsagn.gjennomforing.id,
            lopenummer = tilsagn.gjennomforing.lopenummer,
            navn = tilsagn.gjennomforing.navn,
        ),
        bruktBelop = tilsagn.belopBrukt,
        gjenstaendeBelop = tilsagn.gjenstaendeBelop(),
        tiltakstype = ArrangorflateTiltakstype(
            navn = tilsagn.tiltakstype.navn,
            tiltakskode = tilsagn.tiltakstype.tiltakskode,
        ),
        type = tilsagn.type,
        periode = tilsagn.periode,
        beregning = toArrangorflateTilsagnBeregningDetails(tilsagn),
        arrangor = ArrangorflateArrangor(
            id = tilsagn.arrangor.id,
            navn = tilsagn.arrangor.navn,
            organisasjonsnummer = tilsagn.arrangor.organisasjonsnummer,
        ),
        status = tilsagn.status,
        bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
        beskrivelse = tilsagn.beskrivelse,
    )
}

private fun toArrangorflateTilsagnBeregningDetails(tilsagn: Tilsagn): DataDetails {
    val entries = when (tilsagn.beregning) {
        is TilsagnBeregningFri -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.nok("Totalbeløp", tilsagn.beregning.output.belop),
            LabeledDataElement.nok("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.nok("Sats per tiltaksplass per måned", tilsagn.beregning.input.sats),
            LabeledDataElement.nok("Totalbeløp", tilsagn.beregning.output.belop),
            LabeledDataElement.nok("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerManedsverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.nok("Avtalt månedspris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.nok("Totalbeløp", tilsagn.beregning.output.belop),
            LabeledDataElement.nok("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerUkesverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.nok("Avtalt ukespris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.nok("Totalbeløp", tilsagn.beregning.output.belop),
            LabeledDataElement.nok("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerHeleUkesverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.nok("Avtalt ukespris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.nok("Totalbeløp", tilsagn.beregning.output.belop),
            LabeledDataElement.nok("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.nok("Pris per time oppfølging", tilsagn.beregning.input.sats),
            LabeledDataElement.nok("Totalbeløp", tilsagn.beregning.output.belop),
            LabeledDataElement.nok("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )
    }
    return DataDetails(entries = entries)
}

fun arrangorAvbrytStatus(utbetaling: Utbetaling): ArrangorAvbrytStatus {
    if (utbetaling.innsender != Arrangor) {
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

        UtbetalingStatusType.INNSENDT,
        UtbetalingStatusType.RETURNERT,
        -> ArrangorAvbrytStatus.ACTIVATED
    }
}

enum class ArrangorAvbrytStatus {
    ACTIVATED,
    DEACTIVATED,
    HIDDEN,
}
