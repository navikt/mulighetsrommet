package no.nav.mulighetsrommet.api.arrangorflate

import arrow.core.Either
import kotlinx.serialization.Serializable
import kotliquery.Row
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
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
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
    fun getUtbetalinger(orgnr: Organisasjonsnummer): List<ArrFlateUtbetalingKompaktDto> = db.session {
        return queries.utbetaling.getByArrangorIds(orgnr).map { utbetaling ->
            val status = getArrFlateUtbetalingStatus(utbetaling)
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
            ArrFlateUtbetalingKompaktDto.fromUtbetaling(utbetaling, status, godkjentBelop)
        }
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

    fun getRelevanteForslag(utbetaling: Utbetaling): List<RelevanteForslag> = db.session {
        return queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
            .map { (deltakerId, forslag) ->
                RelevanteForslag(
                    deltakerId = deltakerId,
                    antallRelevanteForslag = forslag.count { isForslagRelevantForUtbetaling(it, utbetaling) },
                )
            }
    }

    private fun getGodkjentBelopForUtbetaling(utbetalingId: UUID): Int = db.session {
        return queries.delutbetaling.getByUtbetalingId(utbetalingId).sumOf { it.belop }
    }

    suspend fun toArrFlateUtbetaling(
        utbetaling: Utbetaling,
        relativeDate: LocalDateTime = LocalDateTime.now(),
    ): ArrFlateUtbetaling = db.session {
        val status = getArrFlateUtbetalingStatus(utbetaling)
        val erTolvUkerEtterInnsending = utbetaling.godkjentAvArrangorTidspunkt
            ?.let { it.plusWeeks(12) <= relativeDate } ?: false

        val deltakere = if (erTolvUkerEtterInnsending) {
            emptyList()
        } else {
            queries.deltaker
                .getAll(gjennomforingId = utbetaling.gjennomforing.id)
                .filter { it.id in utbetaling.beregning.output.deltakelser.map { it.deltakelseId } }
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

    private fun QueryContext.getArrFlateUtbetalingStatus(utbetaling: Utbetaling): ArrFlateUtbetalingStatus {
        val delutbetalinger = queries.delutbetaling.getByUtbetalingId(utbetaling.id)
        val relevanteForslag = getRelevanteForslag(utbetaling)
        return ArrFlateUtbetalingStatus.fromUtbetaling(
            utbetaling.status,
            delutbetalinger,
            relevanteForslag,
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
    val periode = when (utbetaling.beregning) {
        is UtbetalingBeregningFri -> return false

        is UtbetalingBeregningPrisPerManedsverkMedDeltakelsesmengder -> {
            val deltaker = utbetaling.beregning.input.deltakelser
                .find { it.deltakelseId == forslag.deltakerId }
                ?: return false
            Periode.fromRange(deltaker.perioder.map { it.periode })
        }

        is UtbetalingBeregningPrisPerManedsverk -> {
            utbetaling.beregning.input.deltakelser
                .find { it.deltakelseId == forslag.deltakerId }
                ?.periode
                ?: return false
        }

        is UtbetalingBeregningPrisPerUkesverk -> {
            utbetaling.beregning.input.deltakelser
                .find { it.deltakelseId == forslag.deltakerId }
                ?.periode
                ?: return false
        }
    }
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

fun Row.toArrangorflateGjennomforing(): ArrangorflateGjennomforing = ArrangorflateGjennomforing(
    id = uuid("id"),
    navn = string("navn"),
    startDato = localDate("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
)
