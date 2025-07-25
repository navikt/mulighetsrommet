package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.*
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.tiltak.okonomi.Tilskuddstype
import java.time.LocalDateTime
import java.util.*

@Serializable
data class UtbetalingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: UtbetalingStatusDto,
    val periode: Periode,
    val belop: Int,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val betalingsinformasjon: Utbetaling.Betalingsinformasjon,
    val beskrivelse: String?,
    val begrunnelseMindreBetalt: String?,
    val innsendtAv: String?,
    val journalpostId: String?,
    val tilskuddstype: Tilskuddstype,
    val type: UtbetalingType?,
) {
    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling): UtbetalingDto {
            return UtbetalingDto(
                id = utbetaling.id,
                status = UtbetalingStatusDto.fromUtbetaling(utbetaling),
                periode = utbetaling.periode,
                godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
                betalingsinformasjon = utbetaling.betalingsinformasjon,
                createdAt = utbetaling.createdAt,
                beskrivelse = utbetaling.beskrivelse,
                begrunnelseMindreBetalt = utbetaling.begrunnelseMindreBetalt,
                belop = utbetaling.beregning.output.belop,
                innsendtAv = formaterInnsendtAv(utbetaling.innsender),
                journalpostId = utbetaling.journalpostId,
                tilskuddstype = utbetaling.tilskuddstype,
                type = UtbetalingType.from(utbetaling),
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
