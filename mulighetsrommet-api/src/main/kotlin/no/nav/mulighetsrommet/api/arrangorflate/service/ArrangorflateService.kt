package no.nav.mulighetsrommet.api.arrangorflate.service

import arrow.core.Either
import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangforflateUtbetalingLinje
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateArrangorDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateGjennomforingDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnSummary
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTiltakstypeDto
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateUtbetalingDto
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingKompakt
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
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
import no.nav.mulighetsrommet.api.utbetaling.DeltakerAdvarsel
import no.nav.mulighetsrommet.api.utbetaling.UtbetalingAdvarsler
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFastSatsPerTiltaksplassPerManed
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerHeleUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerTimeOppfolging
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.ktor.exception.StatusException
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.DataDetails
import no.nav.mulighetsrommet.model.Kontonummer
import no.nav.mulighetsrommet.model.LabeledDataElement
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
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
    private fun tilArrangorflateUtbetalingKompakt(utbetaling: Utbetaling): ArrangorflateUtbetalingKompakt {
        val harAdvarsler = when (utbetaling.status) {
            UtbetalingStatusType.GENERERT -> getAdvarsler(utbetaling).isNotEmpty()

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
            -> getGodkjentBelopForUtbetaling(utbetaling.id, valuta = utbetaling.beregning.output.pris.valuta)

            ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING,
            ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
            ArrangorflateUtbetalingStatus.KREVER_ENDRING,
            ArrangorflateUtbetalingStatus.AVBRUTT,
            -> null
        }
        return ArrangorflateUtbetalingKompakt.fromUtbetaling(utbetaling, status, godkjentBelop)
    }

    fun getUtbetalingerByArrangorerAndStatus(arrangorer: Set<Organisasjonsnummer>, statuser: Set<UtbetalingStatusType>): List<ArrangorflateUtbetalingKompakt> = db.session {
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

    fun getAdvarsler(utbetaling: Utbetaling): List<DeltakerAdvarsel> = db.session {
        val forslag = queries.deltakerForslag.getForslagByGjennomforing(utbetaling.gjennomforing.id)
        val deltakere = queries.deltaker
            .getByGjennomforingId(utbetaling.gjennomforing.id)
            .filter { it.id in utbetaling.beregning.input.deltakelser().map { it.deltakelseId } }

        return UtbetalingAdvarsler.getAdvarsler(utbetaling, deltakere, forslag)
    }

    private fun getGodkjentBelopForUtbetaling(utbetalingId: UUID, valuta: Valuta): ValutaBelop = db.session {
        return queries.delutbetaling.getByUtbetalingId(utbetalingId).sumOf { it.pris.belop }.withValuta(valuta)
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
                .getByGjennomforingId(utbetaling.gjennomforing.id)
                .filter { it.id in deltakelser }
        }

        val personalia = getPersonalia(deltakere.map { it.id }.toSet())
        val advarsler = getAdvarsler(utbetaling)
        val status = getArrangorflateUtbetalingStatus(utbetaling, advarsler.isNotEmpty())
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
                    TilsagnDto.fromTilsagn(it)
                }

                ArrangforflateUtbetalingLinje(
                    id = delutbetaling.id,
                    pris = delutbetaling.pris,
                    status = delutbetaling.status,
                    statusSistOppdatert = delutbetaling.faktura.statusSistOppdatert,
                    tilsagn = ArrangorflateTilsagnSummary(
                        id = tilsagn.id,
                        bestillingsnummer = tilsagn.bestillingsnummer,
                    ),
                )
            }
    }

    fun QueryContext.kanRegenereres(utbetaling: Utbetaling): Pair<Boolean, UUID?> {
        if (utbetaling.innsender != Arrangor) {
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

fun toArrangorflateTilsagn(
    tilsagn: Tilsagn,
): ArrangorflateTilsagnDto {
    return ArrangorflateTilsagnDto(
        id = tilsagn.id,
        gjennomforing = ArrangorflateGjennomforingDto(
            id = tilsagn.gjennomforing.id,
            lopenummer = tilsagn.gjennomforing.lopenummer,
            navn = tilsagn.gjennomforing.navn,
        ),
        bruktBelop = tilsagn.belopBrukt,
        gjenstaendeBelop = tilsagn.gjenstaendeBelop(),
        tiltakstype = ArrangorflateTiltakstypeDto(
            navn = tilsagn.tiltakstype.navn,
            tiltakskode = tilsagn.tiltakstype.tiltakskode,
        ),
        type = tilsagn.type,
        periode = tilsagn.periode,
        beregning = toArrangorflateTilsagnBeregningDetails(tilsagn),
        arrangor = ArrangorflateArrangorDto(
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
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningFastSatsPerTiltaksplassPerManed -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Sats per tiltaksplass per måned", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerManedsverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt månedspris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerUkesverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt ukespris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerHeleUkesverk -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Avtalt ukespris per tiltaksplass", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
        )

        is TilsagnBeregningPrisPerTimeOppfolgingPerDeltaker -> listOf(
            LabeledDataElement.periode("Tilsagnsperiode", tilsagn.periode),
            LabeledDataElement.number("Antall plasser", tilsagn.beregning.input.antallPlasser),
            LabeledDataElement.money("Pris per time oppfølging", tilsagn.beregning.input.sats),
            LabeledDataElement.money("Totalbeløp", tilsagn.beregning.output.pris),
            LabeledDataElement.money("Gjenstående beløp", tilsagn.gjenstaendeBelop()),
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
