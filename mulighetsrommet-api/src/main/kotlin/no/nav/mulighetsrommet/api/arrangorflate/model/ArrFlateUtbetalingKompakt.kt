package no.nav.mulighetsrommet.api.arrangorflate.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ArrFlateUtbetalingKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: ArrFlateUtbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fristForGodkjenning: LocalDateTime,
    val tiltakstype: UtbetalingDto.Tiltakstype,
    val gjennomforing: UtbetalingDto.Gjennomforing,
    val arrangor: UtbetalingDto.Arrangor,
    val periode: Periode,
    val belop: Int,
) {
    companion object {
        fun fromUtbetalingDto(utbetaling: UtbetalingDto, status: ArrFlateUtbetalingStatus) = ArrFlateUtbetalingKompakt(
            id = utbetaling.id,
            status = status,
            fristForGodkjenning = utbetaling.fristForGodkjenning,
            tiltakstype = utbetaling.tiltakstype,
            gjennomforing = utbetaling.gjennomforing,
            arrangor = utbetaling.arrangor,
            periode = utbetaling.periode,
            belop = utbetaling.beregning.output.belop,
        )
    }
}
