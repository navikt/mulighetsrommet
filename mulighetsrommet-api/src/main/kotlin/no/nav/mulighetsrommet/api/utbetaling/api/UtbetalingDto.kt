package no.nav.mulighetsrommet.api.utbetaling.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
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
    val innsendtAv: String?,
) {
    @Serializable
    data class Beregning(
        val belop: Int,
    )

    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling, status: AdminUtbetalingStatus) = UtbetalingDto(
            id = utbetaling.id,
            status = status,
            periode = utbetaling.periode,
            godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
            betalingsinformasjon = utbetaling.betalingsinformasjon,
            createdAt = utbetaling.createdAt,
            beskrivelse = utbetaling.beskrivelse,
            beregning = Beregning(
                belop = utbetaling.beregning.output.belop,
            ),
            innsendtAv = formaterInnsendtAv(utbetaling.innsender),
        )

        private fun formaterInnsendtAv(innsender: Utbetaling.Innsender?): String? {
            return when (innsender) {
                is Utbetaling.Innsender.ArrangorAnsatt -> "Arrangør"
                is Utbetaling.Innsender.NavAnsatt -> innsender.navIdent.value
                else -> null
            }
        }
    }
}
