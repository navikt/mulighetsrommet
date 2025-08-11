package no.nav.mulighetsrommet.api.arrangorflate

import arrow.core.Either
import kotlinx.serialization.Serializable
import no.nav.amt.model.Melding
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.api.*
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontonummerRegisterOrganisasjonError
import no.nav.mulighetsrommet.api.clients.kontoregisterOrganisasjon.KontoregisterOrganisasjonClient
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnBeregningDto
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatusAarsak
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll
import no.nav.mulighetsrommet.api.utbetaling.Person
import no.nav.mulighetsrommet.api.utbetaling.PersonService
import no.nav.mulighetsrommet.api.utbetaling.api.ArrangorUtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

private val TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR = listOf(
    TilsagnStatus.GODKJENT,
    TilsagnStatus.TIL_ANNULLERING,
    TilsagnStatus.ANNULLERT,
    TilsagnStatus.OPPGJORT,
    TilsagnStatus.TIL_OPPGJOR,
)

class ArrangorFlateService(
    private val db: ApiDatabase,
    private val personService: PersonService,
    private val kontoregisterOrganisasjonClient: KontoregisterOrganisasjonClient,
) {
    fun getUtbetalinger(orgnr: Organisasjonsnummer): ArrFlateUtbetalinger = db.session {
        val aktive = mutableListOf<ArrFlateUtbetalingKompaktDto>()
        val historiske = mutableListOf<ArrFlateUtbetalingKompaktDto>()

        queries.utbetaling.getByArrangorIds(orgnr).map { utbetaling ->
            val advarsler = getAdvarsler(utbetaling)
            val status = getArrFlateUtbetalingStatus(utbetaling, advarsler)
            val godkjentBelop =
                if (status in listOf(
                        ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
                        ArrFlateUtbetalingStatus.UTBETALT,
                    )
                ) {
                    getGodkjentBelopForUtbetaling(utbetaling.id)
                } else {
                    null
                }
            val dto = ArrFlateUtbetalingKompaktDto.fromUtbetaling(utbetaling, status, godkjentBelop)
            when (dto.status) {
                ArrFlateUtbetalingStatus.KREVER_ENDRING,
                ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV,
                ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING,
                -> aktive += dto
                ArrFlateUtbetalingStatus.AVBRUTT,
                ArrFlateUtbetalingStatus.UTBETALT,
                ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING,
                -> historiske += dto
            }
        }
        return ArrFlateUtbetalinger(aktive = aktive, historiske = historiske)
    }

    fun getUtbetaling(id: UUID): Utbetaling? = db.session {
        return queries.utbetaling.get(id)
    }

    fun getTilsagn(id: UUID): ArrangorflateTilsagnDto? = db.session {
        queries.tilsagn.get(id)
            ?.takeIf { it.status in TILSAGN_STATUS_RELEVANT_FOR_ARRANGOR }
            ?.let { toArrangorflateTilsagn(it) }
    }

    fun getTilsagn(filter: ArrFlateTilsagnFilter, orgnr: Organisasjonsnummer): List<ArrangorflateTilsagnDto> = db.session {
        queries.tilsagn
            .getAll(
                arrangor = orgnr,
                statuser = filter.statuser,
                typer = filter.typer,
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

    fun getAdvarsler(utbetaling: Utbetaling): List<DeltakerAdvarsel> = db.session {
        val deltakere = queries.deltaker
            .getAll(gjennomforingId = utbetaling.gjennomforing.id)
            .filter { it.id in utbetaling.beregning.input.deltakelser().map { it.deltakelseId } }

        return getRelevanteForslag(utbetaling) +
            getFeilSluttDato(deltakere, LocalDate.now()) +
            getOverlappendePerioder(deltakere, utbetaling.periode)
    }

    fun getOverlappendePerioder(
        deltakere: List<Deltaker>,
        utbetalingPeriode: Periode,
    ): List<DeltakerAdvarsel.OverlappendePeriode> {
        val deltakerPerioder = deltakere.mapNotNull { deltaker ->
            deltaker.norskIdent?.let {
                DeltakerOgPeriode(
                    deltaker.id,
                    deltaker.norskIdent,
                    Periode.fromInclusiveDates(
                        deltaker.startDato ?: utbetalingPeriode.start,
                        deltaker.sluttDato ?: utbetalingPeriode.getLastInclusiveDate(),
                    ),
                )
            }
        }
        return deltakerPerioder
            .mapNotNull { deltakerOgPeriode ->
                DeltakerAdvarsel.OverlappendePeriode(deltakerOgPeriode.id)
                    .takeIf { _ -> harOverlappendePeriode(deltakerOgPeriode, deltakerPerioder) }
            }
    }

    fun getFeilSluttDato(deltakere: List<Deltaker>, today: LocalDate): List<DeltakerAdvarsel.FeilSluttDato> {
        return deltakere
            .mapNotNull {
                DeltakerAdvarsel.FeilSluttDato(it.id)
                    .takeIf { _ -> harFeilSluttDato(it.status.type, it.sluttDato, today = today) }
            }
    }

    private fun QueryContext.getRelevanteForslag(utbetaling: Utbetaling): List<DeltakerAdvarsel.RelevanteForslag> {
        return queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
            .mapNotNull { (deltakerId, forslag) ->
                when (val count = forslag.count { isForslagRelevantForUtbetaling(it, utbetaling) }) {
                    0 -> null
                    else -> DeltakerAdvarsel.RelevanteForslag(
                        deltakerId = deltakerId,
                        antallRelevanteForslag = count,
                    )
                }
            }
    }

    private fun getGodkjentBelopForUtbetaling(utbetalingId: UUID): Int = db.session {
        return queries.delutbetaling.getByUtbetalingId(utbetalingId).sumOf { it.belop }
    }

    suspend fun toArrFlateUtbetaling(
        utbetaling: Utbetaling,
        relativeDate: LocalDateTime = LocalDateTime.now(),
    ): ArrFlateUtbetaling = db.session {
        val advarsler = getAdvarsler(utbetaling)
        val status = getArrFlateUtbetalingStatus(utbetaling, advarsler)
        val erTolvUkerEtterInnsending = utbetaling.godkjentAvArrangorTidspunkt
            ?.let { it.plusWeeks(12) <= relativeDate } ?: false

        val deltakere = if (erTolvUkerEtterInnsending) {
            emptyList()
        } else {
            queries.deltaker
                .getAll(gjennomforingId = utbetaling.gjennomforing.id)
                .filter { it.id in utbetaling.beregning.output.deltakelser().map { it.deltakelseId } }
        }

        val personerByNorskIdent = personService.getPersoner(deltakere.mapNotNull { it.norskIdent })
            .associateBy { it.norskIdent }
        val deltakerPersoner: Map<UUID, Pair<Deltaker, Person?>> = deltakere
            .associateBy { it.id }
            .mapValues { it.value to it.value.norskIdent?.let { personerByNorskIdent.getValue(it) } }

        return mapUtbetalingToArrFlateUtbetaling(
            utbetaling = utbetaling,
            status = status,
            deltakerPersoner = deltakerPersoner,
            advarsler = advarsler,
            linjer = getLinjer(utbetaling.id),
            kanViseBeregning = !erTolvUkerEtterInnsending,
        )
    }

    private fun QueryContext.getLinjer(utbetalingId: UUID): List<ArrangorUtbetalingLinje> {
        return queries.delutbetaling.getByUtbetalingId(utbetalingId)
            .map { delutbetaling ->
                val tilsagn = checkNotNull(queries.tilsagn.get(delutbetaling.tilsagnId)).let {
                    TilsagnDto.fromTilsagn(it)
                }

                ArrangorUtbetalingLinje(
                    id = delutbetaling.id,
                    belop = delutbetaling.belop,
                    status = delutbetaling.status,
                    statusSistOppdatert = delutbetaling.fakturaStatusSistOppdatert,
                    tilsagn = ArrangorUtbetalingLinje.Tilsagn(
                        id = tilsagn.id,
                        bestillingsnummer = tilsagn.bestillingsnummer,
                    ),
                )
            }
    }

    private fun QueryContext.getArrFlateUtbetalingStatus(
        utbetaling: Utbetaling,
        advarsler: List<DeltakerAdvarsel>,
    ): ArrFlateUtbetalingStatus {
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)
        return ArrFlateUtbetalingStatus.fromUtbetaling(
            utbetaling.status,
            delutbetalinger,
            harAdvarsler = advarsler.isNotEmpty(),
        )
    }

    fun getGjennomforinger(
        orgnr: Organisasjonsnummer,
        prismodeller: List<Prismodell>,
    ): List<ArrangorflateGjennomforing> = db.session {
        queries.gjennomforing
            .getAll(
                arrangorOrgnr = listOf(orgnr),
                prismodeller = prismodeller,
            )
            .items.map {
                ArrangorflateGjennomforing(
                    id = it.id,
                    navn = it.navn,
                    startDato = it.startDato,
                    sluttDato = it.sluttDato,
                )
            }
    }

    suspend fun getKontonummer(orgnr: Organisasjonsnummer): Either<KontonummerRegisterOrganisasjonError, String> {
        return kontoregisterOrganisasjonClient
            .getKontonummerForOrganisasjon(orgnr)
            .map { it.kontonr }
    }

    suspend fun synkroniserKontonummer(utbetaling: Utbetaling): Either<KontonummerRegisterOrganisasjonError, String> = db.session {
        getKontonummer(utbetaling.arrangor.organisasjonsnummer).onRight {
            queries.utbetaling.setKontonummer(
                id = utbetaling.id,
                kontonummer = Kontonummer(it),
            )
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

private fun QueryContext.toArrangorflateTilsagn(
    tilsagn: Tilsagn,
): ArrangorflateTilsagnDto {
    val annullering = queries.totrinnskontroll.get(tilsagn.id, Totrinnskontroll.Type.ANNULLER)
    return ArrangorflateTilsagnDto(
        id = tilsagn.id,
        gjennomforing = ArrangorflateTilsagnDto.Gjennomforing(
            id = tilsagn.gjennomforing.id,
            navn = tilsagn.gjennomforing.navn,
        ),
        bruktBelop = tilsagn.belopBrukt,
        gjenstaendeBelop = tilsagn.gjenstaendeBelop(),
        tiltakstype = ArrangorflateTilsagnDto.Tiltakstype(
            navn = tilsagn.tiltakstype.navn,
        ),
        type = tilsagn.type,
        periode = tilsagn.periode,
        beregning = TilsagnBeregningDto.from(tilsagn.beregning),
        arrangor = ArrangorflateTilsagnDto.Arrangor(
            id = tilsagn.arrangor.id,
            navn = tilsagn.arrangor.navn,
            organisasjonsnummer = tilsagn.arrangor.organisasjonsnummer,
        ),
        status = ArrangorflateTilsagnDto.StatusOgAarsaker(
            status = tilsagn.status,
            aarsaker = annullering?.aarsaker?.map { TilsagnStatusAarsak.valueOf(it) } ?: listOf(),
        ),
        bestillingsnummer = tilsagn.bestilling.bestillingsnummer,
    )
}

@Serializable
data class ArrangorflateGjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
)
