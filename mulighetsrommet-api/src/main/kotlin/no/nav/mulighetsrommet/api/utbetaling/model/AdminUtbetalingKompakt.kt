package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class AdminUtbetalingKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: AdminUtbetalingStatus,
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
        @Serializable(with = LocalDateSerializer::class)
        val periodeStart: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val periodeSlutt: LocalDate,
        val belop: Int,
    )

    companion object {
        fun fromUtbetalingDto(utbetaling: UtbetalingDto, status: AdminUtbetalingStatus) = AdminUtbetalingKompakt(
            id = utbetaling.id,
            status = status,
            beregning = Beregning(
                periodeStart = utbetaling.periode.start,
                periodeSlutt = utbetaling.periode.getLastInclusiveDate(),
                belop = utbetaling.beregning.output.belop,
            ),
            godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
            betalingsinformasjon = utbetaling.betalingsinformasjon,
            createdAt = utbetaling.createdAt,
            beskrivelse = utbetaling.beskrivelse,
        )
    }
}
