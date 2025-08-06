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
    val tiltakstype: ArrangorflateTiltakstype,
    val gjennomforing: ArrangorflateGjennomforingInfo,
    val arrangor: ArrangorflateArrangor,
    val type: UtbetalingType? = null,
    val periode: Periode,
    val status: ArrFlateUtbetalingStatus,
    @Serializable(with = LocalDateTimeSerializer::class)
    val godkjentAvArrangorTidspunkt: LocalDateTime?,
    val belop: Int,
    val godkjentBelop: Int?,
) {
    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling, status: ArrFlateUtbetalingStatus, godkjentBelop: Int?) = ArrFlateUtbetalingKompaktDto(
            id = utbetaling.id,
            status = status,
            godkjentAvArrangorTidspunkt = utbetaling.godkjentAvArrangorTidspunkt,
            tiltakstype = ArrangorflateTiltakstype(
                navn = utbetaling.tiltakstype.navn,
                tiltakskode = utbetaling.tiltakstype.tiltakskode,
            ),
            gjennomforing = ArrangorflateGjennomforingInfo(
                id = utbetaling.gjennomforing.id,
                navn = utbetaling.gjennomforing.navn,
            ),
            arrangor = ArrangorflateArrangor(
                id = utbetaling.arrangor.id,
                organisasjonsnummer = utbetaling.arrangor.organisasjonsnummer,
                navn = utbetaling.arrangor.navn,
            ),
            periode = utbetaling.periode,
            belop = utbetaling.beregning.output.belop,
            godkjentBelop = godkjentBelop,
            type = UtbetalingType.from(utbetaling),
        )
    }
}
