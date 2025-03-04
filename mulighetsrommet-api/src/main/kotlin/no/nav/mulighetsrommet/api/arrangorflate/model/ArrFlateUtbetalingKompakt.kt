package no.nav.mulighetsrommet.api.arrangorflate.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ArrFlateUtbetalingKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: ArrFlateUtbetaling.Status,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fristForGodkjenning: LocalDateTime,
    val tiltakstype: UtbetalingDto.Tiltakstype,
    val gjennomforing: UtbetalingDto.Gjennomforing,
    val arrangor: UtbetalingDto.Arrangor,
    @Serializable(with = LocalDateSerializer::class)
    val periodeStart: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val periodeSlutt: LocalDate,
    val belop: Int,
) {
    companion object {
        fun fromUtbetalingDto(utbetaling: UtbetalingDto, harRelevanteForslag: Boolean) = ArrFlateUtbetalingKompakt(
            id = utbetaling.id,
            status = ArrFlateUtbetaling.Status.fromUtbetaling(utbetaling, harRelevanteForslag),
            fristForGodkjenning = utbetaling.fristForGodkjenning,
            tiltakstype = utbetaling.tiltakstype,
            gjennomforing = utbetaling.gjennomforing,
            arrangor = utbetaling.arrangor,
            periodeStart = utbetaling.periode.start,
            periodeSlutt = utbetaling.periode.getLastInclusiveDate(),
            belop = utbetaling.beregning.output.belop,
        )
    }
}
