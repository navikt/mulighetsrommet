package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningFri
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerManedsverk
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingBeregningPrisPerUkesverk
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
    val status: AdminUtbetalingStatus,
    val periode: Periode,
    val beregning: Beregning,
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
    @Serializable
    sealed class Beregning {
        abstract val belop: Int

        @Serializable
        @SerialName("PRIS_PER_MANEDSVERK")
        data class PrisPerManedsverk(
            val sats: Int,
            override val belop: Int,
        ) : Beregning()

        @Serializable
        @SerialName("PRIS_PER_UKESVERK")
        data class PrisPerUkesverk(
            val sats: Int,
            override val belop: Int,
        ) : Beregning()

        @Serializable
        @SerialName("FRI")
        data class Fri(
            override val belop: Int,
        ) : Beregning()
    }

    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling, status: AdminUtbetalingStatus): UtbetalingDto {
            return UtbetalingDto(
                id = utbetaling.id,
                status = status,
                periode = utbetaling.periode,
                godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
                betalingsinformasjon = utbetaling.betalingsinformasjon,
                createdAt = utbetaling.createdAt,
                beskrivelse = utbetaling.beskrivelse,
                begrunnelseMindreBetalt = utbetaling.begrunnelseMindreBetalt,
                beregning = when (utbetaling.beregning) {
                    is UtbetalingBeregningFri -> Beregning.Fri(
                        belop = utbetaling.beregning.output.belop,
                    )

                    is UtbetalingBeregningPrisPerManedsverk -> Beregning.PrisPerManedsverk(
                        belop = utbetaling.beregning.output.belop,
                        sats = utbetaling.beregning.input.sats,
                    )

                    is UtbetalingBeregningPrisPerUkesverk -> Beregning.PrisPerUkesverk(
                        belop = utbetaling.beregning.output.belop,
                        sats = utbetaling.beregning.input.sats,
                    )
                },
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
