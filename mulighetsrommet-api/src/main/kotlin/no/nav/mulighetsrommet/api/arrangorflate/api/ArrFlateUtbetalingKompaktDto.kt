package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ArrFlateUtbetalingKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: ArrFlateUtbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    val tiltakstype: Utbetaling.Tiltakstype,
    val gjennomforing: Utbetaling.Gjennomforing,
    val arrangor: Utbetaling.Arrangor,
    val periode: Periode,
    val belop: Int,
    val type: UtbetalingType? = null,
) {
    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling, status: ArrFlateUtbetalingStatus) = ArrFlateUtbetalingKompaktDto(
            id = utbetaling.id,
            status = status,
            godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
            tiltakstype = utbetaling.tiltakstype,
            gjennomforing = utbetaling.gjennomforing,
            arrangor = utbetaling.arrangor,
            periode = utbetaling.periode,
            belop = utbetaling.beregning.output.belop,
            type = UtbetalingType.from(utbetaling)
        )
    }
}
