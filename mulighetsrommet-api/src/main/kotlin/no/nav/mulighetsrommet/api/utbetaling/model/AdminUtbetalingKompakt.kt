package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AdminUtbetalingKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: AdminUtbetalingStatus,
    val periode: Periode,
    val beregning: Beregning,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val betalingsinformasjon: UtbetalingDto.Betalingsinformasjon,
    val beskrivelse: String?,
) {
    @Serializable
    data class Beregning(
        val belop: Int,
    )

    companion object {
        fun fromUtbetalingDto(utbetaling: UtbetalingDto, status: AdminUtbetalingStatus) = AdminUtbetalingKompakt(
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
        )
    }
}
