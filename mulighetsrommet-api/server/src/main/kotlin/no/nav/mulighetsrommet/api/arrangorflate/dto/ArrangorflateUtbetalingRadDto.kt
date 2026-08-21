package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingKompakt
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class ArrangorflateUtbetalingRadDto(
    @Serializable(with = UUIDSerializer::class)
    val utbetalingId: UUID,
    val gjennomforing: ArrangorflateGjennomforingDto,
    val arrangor: ArrangorflateArrangorDto,
    val tiltakstype: ArrangorflateTiltakstypeDto,
    val periode: Periode,
    val pris: ArrangorflatePris,
    val godkjentPris: ValutaBelop?,
    val type: String,
    val status: ArrangorflateUtbetalingStatus,
)

fun ArrangorflateUtbetalingKompakt.toRadDto(): ArrangorflateUtbetalingRadDto = ArrangorflateUtbetalingRadDto(
    utbetalingId = id,
    gjennomforing = gjennomforing,
    arrangor = arrangor,
    tiltakstype = tiltakstype,
    periode = periode,
    pris = pris,
    godkjentPris = godkjentPris,
    type = type.displayName,
    status = status,
)
