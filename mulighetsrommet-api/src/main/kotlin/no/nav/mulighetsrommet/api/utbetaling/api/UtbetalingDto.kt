package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.model.Betalingsinformasjon
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.Agent
import no.nav.mulighetsrommet.model.Arena
import no.nav.mulighetsrommet.model.Arrangor
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
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
    val status: UtbetalingStatusDto,
    val periode: Periode,
    val pris: ValutaBelop,
    @Serializable(with = LocalDateSerializer::class)
    val innsendtAvArrangorDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val utbetalesTidligstDato: LocalDate?,
    val betalingsinformasjon: Betalingsinformasjon?,
    // TODO: gjelder kun korreksjoner. Gjør om ifm. migrering til ny korreksjonsmodell
    val beskrivelse: String?,
    val begrunnelseMindreBetalt: String?,
    val avbruttBegrunnelse: String?,
    val innsendtAv: String?,
    val journalpostId: JournalpostId?,
    val tilskuddstype: Tilskuddstype,
    val type: UtbetalingTypeDto,
) {
    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling): UtbetalingDto {
            return UtbetalingDto(
                id = utbetaling.id,
                gjennomforingId = utbetaling.gjennomforing.id,
                status = UtbetalingStatusDto.fromUtbetalingStatus(utbetaling.status, utbetaling.blokkeringer),
                periode = utbetaling.periode,
                innsendtAvArrangorDato = utbetaling.godkjentAvArrangorTidspunkt?.toLocalDate(),
                utbetalesTidligstDato = utbetaling.utbetalesTidligstTidspunkt?.tilNorskDato(),
                betalingsinformasjon = utbetaling.betalingsinformasjon,
                beskrivelse = utbetaling.korreksjon?.begrunnelse,
                begrunnelseMindreBetalt = utbetaling.begrunnelseMindreBetalt,
                pris = utbetaling.beregning.output.pris,
                innsendtAv = formaterInnsendtAv(utbetaling.innsender),
                journalpostId = utbetaling.journalpostId,
                tilskuddstype = utbetaling.tilskuddstype,
                type = UtbetalingType.from(utbetaling).toDto(),
                avbruttBegrunnelse = utbetaling.avbruttBegrunnelse,
            )
        }

        private fun formaterInnsendtAv(agent: Agent?): String? {
            return when (agent) {
                Arena -> "Arena"
                Arrangor -> "Arrangør"
                is NavIdent -> agent.value
                Tiltaksadministrasjon -> "System"
                else -> null
            }
        }
    }
}
