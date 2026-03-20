package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingLinje
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingStatusType
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.UUID

@Serializable
data class UtbetalingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val type: UtbetalingTypeDto,
    val status: UtbetalingStatusDto,
    val periode: Periode,
    val beregning: ValutaBelop,
    val utbetalt: ValutaBelop?,
    @Serializable(with = LocalDateSerializer::class)
    val innsendtAvArrangorDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val utbetalesTidligstDato: LocalDate?,
    val betalingsinformasjon: Betalingsinformasjon?,
    val kommentar: String?,
    val korreksjon: Korreksjon?,
    val begrunnelseMindreBetalt: String?,
    val avbruttBegrunnelse: String?,
    val journalpostId: JournalpostId?,
    val tilskuddstype: Tilskuddstype,
) {
    @Serializable
    data class Korreksjon(
        @Serializable(with = UUIDSerializer::class)
        val opprinneligUtbetaling: UUID,
        val begrunnelse: String,
    )

    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling, linjer: List<UtbetalingLinje>): UtbetalingDto {
            return UtbetalingDto(
                id = utbetaling.id,
                gjennomforingId = utbetaling.gjennomforing.id,
                status = UtbetalingStatusDto.fromUtbetalingStatus(utbetaling.status, utbetaling.blokkeringer),
                periode = utbetaling.periode,
                beregning = utbetaling.beregning.output.pris,
                utbetalt = getUtbetaltBelop(utbetaling, linjer),
                innsendtAvArrangorDato = utbetaling.innsending?.tidspunkt?.toLocalDate(),
                utbetalesTidligstDato = utbetaling.utbetalesTidligstTidspunkt?.tilNorskDato(),
                betalingsinformasjon = utbetaling.betalingsinformasjon,
                kommentar = utbetaling.kommentar,
                korreksjon = utbetaling.korreksjon?.let { Korreksjon(it.gjelderUtbetalingId, it.begrunnelse) },
                begrunnelseMindreBetalt = utbetaling.begrunnelseMindreBetalt,
                journalpostId = utbetaling.journalpostId,
                tilskuddstype = utbetaling.tilskuddstype,
                type = UtbetalingType.from(utbetaling).toDto(),
                avbruttBegrunnelse = utbetaling.avbruttBegrunnelse,
            )
        }
    }
}

private fun getUtbetaltBelop(
    utbetaling: Utbetaling,
    linjer: List<UtbetalingLinje>,
): ValutaBelop? = when (utbetaling.status) {
    UtbetalingStatusType.GENERERT,
    UtbetalingStatusType.TIL_BEHANDLING,
    UtbetalingStatusType.TIL_ATTESTERING,
    UtbetalingStatusType.RETURNERT,
    UtbetalingStatusType.AVBRUTT,
    -> null

    UtbetalingStatusType.FERDIG_BEHANDLET,
    UtbetalingStatusType.DELVIS_UTBETALT,
    UtbetalingStatusType.UTBETALT,
    -> linjer.fold(ValutaBelop(0, utbetaling.valuta)) { sum, linje -> sum + linje.pris }
}
