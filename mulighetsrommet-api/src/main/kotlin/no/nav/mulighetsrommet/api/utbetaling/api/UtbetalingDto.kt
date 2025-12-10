package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDate
import java.util.*

@Serializable
data class UtbetalingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: UtbetalingStatusDto,
    val periode: Periode,
    val belop: Int,
    @Serializable(with = LocalDateSerializer::class)
    val innsendtAvArrangorDato: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val utbetalesTidligstDato: LocalDate?,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val beskrivelse: String?,
    val begrunnelseMindreBetalt: String?,
    val avbruttBegrunnelse: String?,
    val innsendtAv: String?,
    val journalpostId: String?,
    val tilskuddstype: Tilskuddstype,
    val type: UtbetalingTypeDto,
) {
    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling): UtbetalingDto {
            return UtbetalingDto(
                id = utbetaling.id,
                status = UtbetalingStatusDto.fromUtbetalingStatus(utbetaling.status),
                periode = utbetaling.periode,
                innsendtAvArrangorDato = utbetaling.godkjentAvArrangorTidspunkt?.toLocalDate(),
                utbetalesTidligstDato = utbetaling.utbetalesTidligstTidspunkt?.tilNorskDato(),
                betalingsinformasjon = utbetaling.betalingsinformasjon,
                beskrivelse = utbetaling.beskrivelse,
                begrunnelseMindreBetalt = utbetaling.begrunnelseMindreBetalt,
                belop = utbetaling.beregning.output.belop,
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
