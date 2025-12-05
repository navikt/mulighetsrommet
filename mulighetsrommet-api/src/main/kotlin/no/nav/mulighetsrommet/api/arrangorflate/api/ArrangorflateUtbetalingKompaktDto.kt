package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingType
import no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingTypeDto
import no.nav.mulighetsrommet.api.utbetaling.api.toDto
import no.nav.mulighetsrommet.api.utbetaling.model.Utbetaling
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.*

@Serializable
data class ArrangorflateUtbetalingKompaktDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: ArrangorflateTiltakstype,
    val gjennomforing: ArrangorflateGjennomforingInfo,
    val arrangor: ArrangorflateArrangor,
    val type: UtbetalingTypeDto,
    val periode: Periode,
    val status: ArrangorflateUtbetalingStatus,
    val belop: Int,
    val godkjentBelop: Int?,
) {
    companion object {
        fun fromUtbetaling(utbetaling: Utbetaling, status: ArrangorflateUtbetalingStatus, godkjentBelop: Int?) = ArrangorflateUtbetalingKompaktDto(
            id = utbetaling.id,
            status = status,
            tiltakstype = ArrangorflateTiltakstype(
                navn = utbetaling.tiltakstype.navn,
                tiltakskode = utbetaling.tiltakstype.tiltakskode,
            ),
            gjennomforing = ArrangorflateGjennomforingInfo(
                id = utbetaling.gjennomforing.id,
                lopenummer = utbetaling.gjennomforing.lopenummer,
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
            type = UtbetalingType.from(utbetaling).toDto(),
        )
    }
}
